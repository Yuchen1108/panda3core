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

package de.uniulm.ki.panda3.progression.htn.search;

import de.uniulm.ki.panda3.progression.htn.ProPlanningInstance;
import de.uniulm.ki.panda3.progression.htn.representation.*;
import de.uniulm.ki.panda3.progression.heuristics.htn.GroundedProgressionHeuristic;
import de.uniulm.ki.panda3.progression.htn.representation.SasPlusProblem;
import de.uniulm.ki.panda3.symbolic.domain.Task;

import java.io.*;
import java.util.*;

/**
 * Created by dhoeller on 21.07.16.
 * This is a state in the progression search, i.e. it contains the current network of undone tasks as well as
 * the current state of the environment.
 * <p/>
 * The implementation reuses unchanged parts of the task network and adds only those nodes that are new. This can
 * be done because it is changed only at its beginning, and the nodes only hold pointers to its successors.
 */
public class ProgressionNetwork implements Comparable<ProgressionNetwork>, Cloneable {


    public static SasPlusProblem flatProblem;
    public static Task[] indexToTask;
    public static Map<Task, Integer> taskToIndex;
    public static Map<Task, List<ProMethod>> methods;
    public static Set<Integer> ShopPrecActions = new HashSet<>();

    public BitSet state;
    List<ProgressionPlanStep> unconstraintPrimitiveTasks;
    List<ProgressionPlanStep> unconstraintAbstractTasks;
    public int numProgressionSteps = 0;
    public int numSHOPProgressionSteps = 0;
    public int numDecompositionSteps = 0;

    private boolean printProgressionTrace = false;
    public String progressionTrace;

    public GroundedProgressionHeuristic heuristic;

    public int heuristicVal = 0;
    public int metric = 0;
    public boolean goalRelaxedReachable = true;
    public int id = 0;
    private int numberOfTasks = 0;
    private int numberOfPrimitiveTasks = 0;
    public SolutionStep solution;

    public static boolean useHelpfulActions = false;

    public BitSet helpfulActions;
    private Task initialTask;

    public boolean isHelpfulAction(int action) {
        if (!useHelpfulActions)
            return false;
        return helpfulActions.get(action);
    }

    public boolean isHelpfulMethod(ProMethod method) {
        if (!useHelpfulActions)
            return false;
        return helpfulActions.get(method.methodID);
    }

    private ProgressionNetwork() {
    }

    public ProgressionNetwork(BitSet state, List<ProgressionPlanStep> ps) {
        this.unconstraintPrimitiveTasks = new LinkedList<>();
        this.unconstraintAbstractTasks = new LinkedList<>();
        for (ProgressionPlanStep p : ps) {
            if (p.isPrimitive) {
                unconstraintPrimitiveTasks.add(p);
                numberOfTasks++;
                numberOfPrimitiveTasks++;
            } else {
                unconstraintAbstractTasks.add(p);
                numberOfTasks++;
            }
        }
        solution = new SolutionStep();
        if (printProgressionTrace) {
            System.out.println("WARNING: The system is recording a full decomposition trace - this is VERY slow and only recommended for debugging.");
            this.progressionTrace = "\nPROGRESSION-TRACE:\n\n";
            this.progressionTrace += this.toString();
        }
        this.state = state;
    }

    public boolean empty() {
        return (this.unconstraintPrimitiveTasks.isEmpty() && this.unconstraintAbstractTasks.isEmpty());
    }

    public List<ProgressionPlanStep> getFirstPrimitiveTasks() {
        return this.unconstraintPrimitiveTasks;
    }

    public List<ProgressionPlanStep> getFirstAbstractTasks() {
        return this.unconstraintAbstractTasks;
    }

    @Deprecated
    public List<ProgressionPlanStep> getFirst() {
        List<ProgressionPlanStep> all = new LinkedList<>();
        all.addAll(this.unconstraintAbstractTasks);
        all.addAll(this.unconstraintPrimitiveTasks);
        return all;
    }

    @Override
    public String toString() {
        return networkToString(unconstraintAbstractTasks, unconstraintPrimitiveTasks);
    }

    public static String networkToString(List<ProgressionPlanStep> abstractTasks, List<ProgressionPlanStep> primitiveTasks) {
        int taskid = 0;
        HashMap<ProgressionPlanStep, Integer> tasks = new HashMap<>();
        List<int[]> orderings = new LinkedList<>();
        LinkedList<ProgressionPlanStep> pss = new LinkedList<>();

        for (ProgressionPlanStep ps : abstractTasks) {
            pss.add(ps);
            tasks.put(ps, taskid++);
        }
        for (ProgressionPlanStep ps : primitiveTasks) {
            pss.add(ps);
            tasks.put(ps, taskid++);
        }
        while (!pss.isEmpty()) {
            ProgressionPlanStep ps = pss.removeFirst();
            int iPrec = tasks.get(ps);
            for (ProgressionPlanStep succ : ps.successorList) {
                if (!tasks.containsKey(succ)) {
                    tasks.put(succ, taskid++);
                    pss.add(succ);
                }
                int iSucc = tasks.get(succ);
                int[] ord = new int[2];
                ord[0] = iPrec;
                ord[1] = iSucc;
                orderings.add(ord);
            }
        }
        HashMap<Integer, ProgressionPlanStep> inverse = new HashMap<>();
        for (ProgressionPlanStep key : tasks.keySet()) {
            inverse.put(tasks.get(key), key);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("tasks={");
        int i = 0;
        while (inverse.containsKey(i)) {
            ProgressionPlanStep ps = inverse.get(i);
            if (i > 0) {
                sb.append(", ");
                sb.append("\n       ");
            }
            sb.append(i);
            sb.append(":");
            sb.append(ps.getTask().shortInfo());
            i++;
        }
        boolean first = true;
        sb.append("},\norderings={");
        for (int[] ord : orderings) {
            if (first)
                first = false;
            else
                sb.append(", ");
            sb.append("(");
            sb.append(ord[0]);
            sb.append("<");
            sb.append(ord[1]);
            sb.append(")");
        }
        first = true;
        sb.append("}\nnext={");
        for (ProgressionPlanStep ps : abstractTasks) {
            if (first)
                first = false;
            else
                sb.append(", ");
            sb.append(tasks.get(ps));
        }
        for (ProgressionPlanStep ps : primitiveTasks) {
            if (first)
                first = false;
            else
                sb.append(", ");
            sb.append(tasks.get(ps));
        }
        sb.append("}\n");

        return sb.toString();
    }

    public ProgressionNetwork decompose(ProgressionPlanStep ps, ProMethod m) {
        ProgressionNetwork res = this.clone();

        res.numberOfTasks--; // some task will be decomposed
        res.numberOfTasks += m.numberOfAbsSubtasks;
        res.numberOfTasks += m.numberOfPrimSubtasks;

        res.state = this.state;
        res.solution = new SolutionStep(this.solution, m.m);
        res.numDecompositionSteps++;
        res.unconstraintAbstractTasks.remove(ps);

        // get copy of method subtask network
        ProSubtaskNetwork tn = m.instantiate();

        assert (tn.size() > 0);
        assert (tn.getLastNodes().size() > 0);
        assert (tn.getFirstNodes().size() > 0);

        // method's last tasks constrain all tasks that have been constrained by ps before
        List<ProgressionPlanStep> lastNodes = tn.getLastNodes();
        for (ProgressionPlanStep ps2 : lastNodes) {
            ps2.successorList.addAll(ps.successorList);
        }

        // the first nodes off the task network are new first nodes of this network
        for (ProgressionPlanStep p : tn.getFirstNodes()) {
            if (p.isPrimitive)
                res.unconstraintPrimitiveTasks.add(p);
            else
                res.unconstraintAbstractTasks.add(p);
        }
        if (printProgressionTrace) {
            res.progressionTrace += "\n";
            res.progressionTrace += res.toString();
        }
        return res;
    }

    public ProgressionNetwork apply(ProgressionPlanStep ps) {
        ProgressionNetwork res = this.clone();
        res.numberOfTasks--;
        res.numberOfPrimitiveTasks--;
        res.state = (BitSet) this.state.clone();

        res.solution = new SolutionStep(this.solution, ps.action);
        if (ProgressionNetwork.ShopPrecActions.contains(ps.action))
            res.numSHOPProgressionSteps++;
        else
            res.numProgressionSteps++;

        res.unconstraintPrimitiveTasks.remove(ps);

        assert (isApplicable(res.state, ps.action));

        // transfer state
        for (int df : ProgressionNetwork.flatProblem.delLists[ps.action])
            res.state.set(df, false);
        for (int af : ProgressionNetwork.flatProblem.addLists[ps.action])
            res.state.set(af, true);

        // every successor of ps is a first task if and only if it is
        // not a successor of any task in the firstTasks list.
        HashSet<ProgressionPlanStep> potentialFirst = new HashSet<>();
        potentialFirst.addAll(ps.successorList);

        // todo: shouldn't this be a set?
        LinkedList<ProgressionPlanStep> potentialPredecessors = new LinkedList<>();
        for (ProgressionPlanStep f : res.unconstraintAbstractTasks) { // this must be res.unconstraintTasks. otherwise, the ps itself is in there!
            potentialPredecessors.addAll(f.successorList);
        }
        for (ProgressionPlanStep f : res.unconstraintPrimitiveTasks) {
            potentialPredecessors.addAll(f.successorList);
        }

        // todo: do I really need to do that?
        // todo: this could be done in preprocessing
        for (ProgressionPlanStep f : ps.successorList) {
            potentialPredecessors.addAll(f.successorList);
        }

        while (true) {
            if (potentialFirst.isEmpty()) {
                break; // no first left
            }
            if (potentialPredecessors.isEmpty()) {
                break; // the next that are left are valid firstTasks
            }
            ProgressionPlanStep ps2 = potentialPredecessors.removeFirst();
            potentialPredecessors.addAll(ps2.successorList);
            potentialFirst.remove(ps2);
        }
        for (ProgressionPlanStep p : potentialFirst) {
            if (p.isPrimitive)
                res.unconstraintPrimitiveTasks.add(p);
            else
                res.unconstraintAbstractTasks.add(p);
        }
        if (printProgressionTrace) {
            res.progressionTrace += "\n";
            res.progressionTrace += res.toString();
        }
        return res;
    }

    public ProgressionNetwork insertAction(int action) {
        ProgressionNetwork res = this.clone();
        res.state = (BitSet) this.state.clone();

        res.solution = new SolutionStep(this.solution, action);
        res.numProgressionSteps++;

        assert (isApplicable(res.state, action));

        // transfer state
        for (int df : ProgressionNetwork.flatProblem.delLists[action])
            res.state.set(df, false);
        for (int af : ProgressionNetwork.flatProblem.addLists[action])
            res.state.set(af, true);

        if (printProgressionTrace) {
            res.progressionTrace += "\n";
            res.progressionTrace += res.toString();
        }
        return res;
    }
    private boolean isApplicable(BitSet state, int action) {
        for (int pre : ProgressionNetwork.flatProblem.precLists[action]) {
            if (!state.get(pre))
                return false;
        }
        return true;
    }

    public boolean isApplicable(int action) {
        for (int pre : ProgressionNetwork.flatProblem.precLists[action]) {
            if (!this.state.get(pre))
                return false;
        }
        return true;
    }

    public boolean isGoal() {
        if (!this.empty()) {
            return false;
        }
        for (int g : ProgressionNetwork.flatProblem.gList) {
            if (!state.get(g))
                return false;
        }
        return true;
    }

    @Override
    protected ProgressionNetwork clone() {
        ProgressionNetwork res = new ProgressionNetwork();
        res.numberOfPrimitiveTasks = this.numberOfPrimitiveTasks;
        res.numberOfTasks = this.numberOfTasks;
        res.unconstraintPrimitiveTasks = new LinkedList<>(); // todo: array or linkedList?
        res.unconstraintAbstractTasks = new LinkedList<>(); // todo: array or linkedList?
        res.unconstraintPrimitiveTasks.addAll(this.unconstraintPrimitiveTasks);
        res.unconstraintAbstractTasks.addAll(this.unconstraintAbstractTasks);
        res.solution = this.solution;
        if (printProgressionTrace)
            res.progressionTrace = this.progressionTrace;
        res.numDecompositionSteps = this.numDecompositionSteps;
        res.numProgressionSteps = this.numProgressionSteps;
        res.numSHOPProgressionSteps = this.numSHOPProgressionSteps;
        return res;
    }

    @Override
    public int compareTo(ProgressionNetwork other) {
        int c = (this.metric - other.metric);
        if (c == 0) {
            if (ProPlanningInstance.random.nextBoolean())
                c = 1;
            else c = -1;
        }
        return c;
    }

    public int getNumberOfTasks() {
        return this.numberOfTasks;
    }

    public int getNumberOfPrimitiveTasks() {
        return this.numberOfPrimitiveTasks;
    }

    public void writeToDisk(String path) {
        try {

            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(path)));

            flatProblem.writeToDisk(ps, false);
            this.writeToDisk(ps);
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToDisk(PrintStream bw) throws Exception {
        bw.print("\n;; Tasks\n");
        bw.print(this.indexToTask.length + "\n");
        for (int i = 0; i < this.indexToTask.length; i++) {
            if (indexToTask[i].isPrimitive())
                bw.print("0 ");
            else
                bw.print("1 ");
            bw.print(indexToTask[i].shortInfo());
            bw.print("\n");
        }
        bw.print("\n;; Initial Task\n");
        if (this.getFirstAbstractTasks().size() > 1)
            throw new Exception("More than one task in the initial task network.");

        bw.print(taskToIndex.get(this.getFirstAbstractTasks().get(0).getTask()) + "\n");

        bw.print("\n;; Methods\n");
        //bw.print(methods.keySet().size() + "\n");
        ArrayList<String> lines = new ArrayList<>();
        int numMethods = 0;
        for (Task t : methods.keySet()) {
            int task = taskToIndex.get(t);
            List<ProMethod> mSet = methods.get(t);
            for (ProMethod m : mSet) {
                numMethods++;
                lines.add(m.m.name() + "\n");
                lines.add(task + "\n");
                String line = "";
                for (int i = 0; i < m.subtasks.length; i++) {
                    Task subT = m.subtasks[i];
                    if (i > 0)
                        line += (" ");
                    line += (taskToIndex.get(subT));
                }
                if (m.subtasks.length > 0)
                    line += (" ");
                line += ("-1\n");
                lines.add(line);
                line = "";
                for (int i = 0; i < m.orderings.size(); i++) {
                    if (line.length() > 0)
                        line += " ";
                    line += m.orderings.get(i)[0] + " " + m.orderings.get(i)[1];
                }
                lines.add(line + "-1\n");
            }
        }
        bw.print(numMethods + "\n");
        for (String s : lines)
            bw.print(s);
    }
}
