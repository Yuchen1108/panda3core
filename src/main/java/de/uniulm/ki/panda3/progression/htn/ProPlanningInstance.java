// PANDA 3 -- a domain-independent planner for classical and hierarchical planning
// Copyright (C) 2014-2018 the original author or authors.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package de.uniulm.ki.panda3.progression.htn;

import de.uniulm.ki.panda3.configuration.*;
import de.uniulm.ki.panda3.progression.TDGReachabilityAnalysis.TDGLandmarkFactory;
import de.uniulm.ki.panda3.progression.TDGReachabilityAnalysis.TaskReachabilityGraph;
import de.uniulm.ki.panda3.progression.heuristics.htn.*;
import de.uniulm.ki.panda3.progression.heuristics.htn.RelaxedComposition.RelaxedCompositionSAS;
import de.uniulm.ki.panda3.progression.heuristics.htn.RelaxedComposition.RelaxedCompositionSTRIPS;
import de.uniulm.ki.panda3.progression.heuristics.htn.RelaxedComposition.gphRcFFMulticount;
import de.uniulm.ki.panda3.progression.heuristics.htn.RelaxedComposition.gphRelaxedComposition;
import de.uniulm.ki.panda3.progression.heuristics.sasp.SasHeuristic;
import de.uniulm.ki.panda3.progression.htn.representation.ProMethod;
import de.uniulm.ki.panda3.progression.htn.search.*;
import de.uniulm.ki.panda3.progression.htn.search.searchRoutine.PriorityQueueSearch;
import de.uniulm.ki.panda3.progression.htn.search.SolutionStep;
import de.uniulm.ki.panda3.symbolic.domain.Domain;
import de.uniulm.ki.panda3.symbolic.domain.SimpleDecompositionMethod;
import de.uniulm.ki.panda3.symbolic.domain.Task;
import de.uniulm.ki.panda3.symbolic.plan.Plan;
import de.uniulm.ki.util.InformationCapsule;
import de.uniulm.ki.util.TimeCapsule;
import scala.Tuple2;

import java.io.*;
import java.util.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by Daniel Höller on 01.07.16.
 */
public class ProPlanningInstance {

    final boolean verbose = false;

    public static Random random;
    public static long randomSeed;

    public Task[] plan(Domain d, Plan p, Map<Task, Set<SimpleDecompositionMethod>> methodsByTask,
                       InformationCapsule ic, TimeCapsule tc,
                       PriorityQueueSearch.abstractTaskSelection taskSelectionStrategy,
                       SearchHeuristic heuristic,
                       SearchAlgorithmType search,
                       String searchEngineCall,
                       long randomSeed,
                       long quitAfterMs) throws ExecutionException, InterruptedException {
        if (d.sasPlusRepresentation().isEmpty()) {
            System.out.println("Error: Progression search algorithm did not find action model.");
            System.exit(-1);
        }

        // check for an unsolvable planning instance
        if (d.decompositionMethods().length() == 0) {
            // there cannot be a solution ...
            ic.set(Information.SEARCH_SPACE_FULLY_EXPLORED(), "true");
            return null;
        }

        // Convert data structures
        long totaltime = System.currentTimeMillis();
        ProPlanningInstance.randomSeed = randomSeed;
        random = new Random(ProPlanningInstance.randomSeed);

        ProgressionNetwork.flatProblem = d.sasPlusRepresentation().get().sasPlusProblem();

        Map<Integer, Task> indexToTask = mapTomap(d.sasPlusRepresentation().get().sasPlusIndexToTask());

        Tuple2<Map<Integer, Task>, Map<Task, Integer>> mappings
                = ProgressionNetwork.flatProblem.restrictTo(indexToTask.keySet(), indexToTask);
        indexToTask = mappings._1();
        assert ((d.abstractTasks().size() + d.primitiveTasks().size()) == d.tasks().size());

        // create permanent mappings
        ProgressionNetwork.taskToIndex = new HashMap<>();
        ProgressionNetwork.indexToTask = new Task[d.tasks().size()];

        // create mapping for actions
        for (int i = 0; i < indexToTask.keySet().size(); i++) {
            Task action = indexToTask.get(i);
            ProgressionNetwork.taskToIndex.put(action, i);
            ProgressionNetwork.indexToTask[i] = action;
        }

        // add non-primitive tasks
        int iAbs = indexToTask.keySet().size();
        scala.collection.Iterator<Task> iter = d.abstractTasks().iterator();
        while (iter.hasNext()) {
            Task t = iter.next();
            ProgressionNetwork.taskToIndex.put(t, iAbs);
            ProgressionNetwork.indexToTask[iAbs] = t;
            iAbs++;
        }
        indexToTask = null; // do not use this anymore

        // prepare method representation that is used internally
        HashMap<Task, List<ProMethod>> methods = getEfficientMethodRep(methodsByTask);
        finalizeMethods(methods);
        ProgressionNetwork.methods = methods;

        if (p.planStepsWithoutInitGoal().size() != 1) {
            System.out.println("Error: Progression search algorithm found more than one task in the initial task network.");
            System.exit(-1);
        }

        List<ProgressionPlanStep> initialTasks = new LinkedList<>();
        ProgressionPlanStep ps = new ProgressionPlanStep(p.planStepsWithoutInitGoal().apply(0).schema());
        initialTasks.add(ps);
        ps.methods = methods.get(ps.getTask());
        ProgressionNetwork initialNode = new ProgressionNetwork(ProgressionNetwork.flatProblem.getS0(), initialTasks);

        /* Spezialfälle
         * - BFS/DFS -> PriorityQueue A* & spezielle Heuristik
         * - Greedy Progression
         * - Shop -> branching over all & Tiefensuche
         * - Echte Heuristik x Greedy, Greedy A* (mit Faktor)
         */
        if (search instanceof BFSType$)
            initialNode.heuristic = new gphBFS();
        else if (search instanceof DFSType$) {
            initialNode.heuristic = new gphDFS();
        } else if (search instanceof ExternalSearchEngine) { // write model to hd to use external search engine
            ExternalSearchEngine searchEngine = (ExternalSearchEngine) search;
            writeModelToHD(methods, initialTasks, initialNode, searchEngine.uuid(), searchEngineCall);
            System.exit(0);
        } else if (heuristic instanceof HierarchicalHeuristicRelaxedComposition) {
            HierarchicalHeuristicRelaxedComposition h = (HierarchicalHeuristicRelaxedComposition) heuristic;
            initialNode.heuristic = new gphRelaxedComposition(ProgressionNetwork.flatProblem, h.classicalHeuristic(), methods, initialTasks);
        } else if (heuristic instanceof RelaxedCompositionGraph) {
            RelaxedCompositionGraph heu = (RelaxedCompositionGraph) heuristic;
            initialNode.heuristic = new gphRcFFMulticount(methods, initialTasks, ProgressionNetwork.taskToIndex.keySet(), heu.useTDReachability(), heu.producerSelectionStrategy(), heu.heuristicExtraction());
        } else if (heuristic instanceof GreedyProgression$)
            initialNode.heuristic = new ProGreedyProgression();
        else {
            throw new IllegalArgumentException("Heuristic " + heuristic + " is not supported");
        }

        initialNode.heuristic.build(initialNode);
        initialNode.metric = initialNode.heuristic.getHeuristic();

        PriorityQueueSearch routine;
        boolean printOutput = true;
        boolean findShortest = false;

        boolean aStar = true;
        if (search instanceof GreedyType$)
            aStar = false;

        routine = new PriorityQueueSearch(aStar, printOutput, findShortest, taskSelectionStrategy);
        if (search instanceof AStarActionsType) {
            routine.greediness = (int) ((AStarActionsType) search).weight();
        }

        routine.wallTime = quitAfterMs;

        System.out.println("Searching with \n - " + routine.SearchName() + " search routine");
        if (aStar) {
            System.out.println(" - A-Star search");
        } else {
            System.out.println(" - Greedy search");
        }
        System.out.println(" - HTN heuristic:" + initialNode.heuristic.getName());

        if (taskSelectionStrategy == PriorityQueueSearch.abstractTaskSelection.random) {
            System.out.println(" - Abstract task choice: randomly");
        } else if (taskSelectionStrategy == PriorityQueueSearch.abstractTaskSelection.decompDepth) {
            System.out.println(" - Abstract task choice: via min decomposition depth left");
        } else if (taskSelectionStrategy == PriorityQueueSearch.abstractTaskSelection.methodCount) {
            System.out.println(" - Abstract task choice: via min number of decomposition methods");
        } else if (taskSelectionStrategy == PriorityQueueSearch.abstractTaskSelection.branchOverAll) {
            System.out.println(" - Abstract task choice: branch over all abstract tasks");
        }

        if (quitAfterMs > 0) {
            System.out.println(" - time limit for search is " + (quitAfterMs / 1000) + " sec");
        }

        SolutionStep solution;
        if ((routine instanceof PriorityQueueSearch) && (taskSelectionStrategy == PriorityQueueSearch.abstractTaskSelection.branchOverAll)) {
            System.out.println(" - This is not a good configuration -- it BRANCHES over ALL abstract tasks. " +
                    "One should only do that for evaluation purposes.");
            solution = ((PriorityQueueSearch) routine).searchWithAbstractBranching(initialNode, ic, tc);
        } else {
            solution = routine.search(initialNode, ic, tc);
        }

        assert (isApplicable(solution, ProgressionNetwork.flatProblem.getS0()));

        if (solution != null) {
            System.out.println("\nFound a solution:");
            System.out.println(solution.toString());
            System.out.println("It contains " + solution.getLength() + " modifications, including " + solution.getPrimitiveCount() + " actions.");
        } else System.out.println("Problem unsolvable.");

        if (solution == null)
            return null;
        else
            return solution.toPrimitiveSequence();
    }

    private void writeModelToHD(HashMap<Task, List<ProMethod>> methods, List<ProgressionPlanStep> initialTasks, ProgressionNetwork initialNode, String uuid, String progName) {
        String htnModelFile = System.getProperty("user.dir") + "/" + uuid + ".htn";
        String heuristicModelFile = System.getProperty("user.dir") + "/" + uuid + ".rc";

        System.out.print("Writing HTN model...");
        initialNode.writeToDisk(htnModelFile);
        System.out.println("done");

        System.out.println("Generating RC model");
        RelaxedCompositionSTRIPS compEnc = new RelaxedCompositionSTRIPS(ProgressionNetwork.flatProblem);
        compEnc.generateTaskCompGraph(methods, initialTasks, false);

        try {
            System.out.print("Writing RC model...");
            PrintStream ps2 = new PrintStream(new BufferedOutputStream(new FileOutputStream(heuristicModelFile)));
            compEnc.writeToDisk(ps2, true);
            ps2.close();
            System.out.println("done");
            System.exit(0);

            Process process = new ProcessBuilder(progName, htnModelFile, heuristicModelFile).start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            //System.out.printf("Output of running %s is:", Arrays.toString(args));

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            //System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<Integer, Task> mapTomap(scala.collection.immutable.Map<Object, Task> key2task) {
        Map<Integer, Task> indexToTask = new HashMap<>();
        scala.collection.Iterator<Object> keySet = key2task.keysIterator();
        while (keySet.hasNext()) {
            Integer key = (Integer) keySet.next();
            indexToTask.put(key, key2task.apply(key));
        }
        return indexToTask;
    }

    private void finalizeMethods(HashMap<Task, List<ProMethod>> methods) {
        for (List<ProMethod> y : methods.values()) {
            for (ProMethod z : y) {
                z.finalizeMethod(methods);
            }
        }
    }

    private boolean isApplicable(SolutionStep solution, BitSet state) {
        if (solution == null)
            return true;
        for (Object mod : solution.getSolution()) {
            if (mod instanceof Integer) {
                int a = (Integer) mod;
                for (int pre : ProgressionNetwork.flatProblem.precLists[a]) {
                    if (!state.get(pre))
                        return false;
                }

                for (int df : ProgressionNetwork.flatProblem.delLists[a])
                    state.set(df, false);
                for (int af : ProgressionNetwork.flatProblem.addLists[a])
                    state.set(af, true);
            }
        }
        return true;
    }

    private HashMap<Task, List<ProMethod>> getEfficientMethodRep(Map<Task, Set<SimpleDecompositionMethod>> methodsByTask) {
        HashMap<Task, List<ProMethod>> res = new HashMap<>();
        for (Task t : methodsByTask.keySet()) {
            List<ProMethod> oneSchema = new ArrayList<>();
            res.put(t, oneSchema);
            for (SimpleDecompositionMethod m : methodsByTask.get(t)) {
                oneSchema.add(new ProMethod(m));
            }
        }
        return res;
    }
}
