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

package de.uniulm.ki.panda3.progression.heuristics.htn.RelaxedComposition;

import de.uniulm.ki.panda3.progression.htn.representation.ProMethod;
import de.uniulm.ki.panda3.progression.htn.search.ProgressionNetwork;
import de.uniulm.ki.panda3.progression.htn.search.ProgressionPlanStep;
import de.uniulm.ki.panda3.progression.TDGReachabilityAnalysis.TDGLandmarkFactory;
import de.uniulm.ki.panda3.progression.htn.representation.SasPlusProblem;
import de.uniulm.ki.panda3.symbolic.domain.Task;

import java.util.*;

/**
 * Created by dh on 05.05.17.
 */
public class RelaxedCompositionSAS extends RelaxedCompositionEncoding {
    int numTasks;
    int numAnM;
    int numExtenedStateFeatures;

    public ProMethod[] IndexToMethod;

    public int firstHtnIndex;
    public int firstBurIndex;
    public int lastOverallIndex;

    public int[] unreachable;
    public int[] reachable;
    public int[] unreached;
    public int[] reached;

    public BitSet s0mask;

    public RelaxedCompositionSAS(SasPlusProblem p) {
        assert (p.correctModel());

        this.numOfStateFeatures = p.numOfStateFeatures;
        this.numOfOperators = p.numOfOperators;
        this.firstIndex = p.firstIndex;
        this.lastIndex = p.lastIndex;
        this.indexToMutexGroup = p.indexToMutexGroup;
        this.precLists = p.precLists;
        this.addLists = p.addLists;
        this.delLists = p.delLists;
        this.expandedDelLists = p.expandedDelLists;
        this.precToTask = p.precToTask;
        this.addToTask = p.addToTask;
        this.numPrecs = p.numPrecs;
        this.s0List = p.s0List;
        this.costs = p.costs;
        this.opNames = p.opNames;
        this.varNames = p.varNames;
        this.values = p.values;
        this.gList = p.gList;
        this.factStrs = p.factStrs;
    }

    public void generateTaskCompGraph(HashMap<Task, List<ProMethod>> methods,
                                      List<ProgressionPlanStep> initialTasks) {
        this.numOfNonHtnActions = numOfOperators;
        this.numAnM = createMethodLookupTable(methods);
        this.numTasks = ProgressionNetwork.indexToTask.length;
        tdRechability = new TDGLandmarkFactory(methods, initialTasks, this.numTasks, this.numOfOperators);

        unreachable = new int[numOfOperators];
        reachable = new int[numOfOperators];
        unreached = new int[numTasks];
        reached = new int[numTasks];

        // create reachability variables
        int nextStateBit = this.numOfStateFeatures;
        for (int i = 0; i < numOfOperators; i++) {
            unreachable[i] = nextStateBit++;
            reachable[i] = nextStateBit++;
        }
        firstBurIndex = nextStateBit;
        for (int i = 0; i < numTasks; i++) {
            unreached[i] = nextStateBit++;
            reached[i] = nextStateBit++;
        }

        s0mask = new BitSet();
        for (int i = 0; i < numOfNonHtnActions; i++) {
            s0mask.set(unreachable[i]);
        }
        for (int i = 0; i < numTasks; i++) {
            s0mask.set(unreached[i]);
        }

        this.firstHtnIndex = this.numOfStateFeatures;
        this.lastOverallIndex = nextStateBit - 1;
        this.numExtenedStateFeatures = this.lastOverallIndex + 1;

        int[][] tPrecLists = new int[numAnM][];
        int[] tNumPrecs = new int[numAnM];
        int[][] tAddLists = new int[numAnM][];
        int[][] tDelLists = new int[numAnM][];
        String[] tOpNames = new String[numAnM];
        int[] tCosts = new int[numAnM];

        /**
         * The original actions get an additional prec that represents its top-down-reachability and an additional add
         * effect that marks this action (aka primitive task) as reached via composition.
         */
        for (int iA = 0; iA < this.numOfOperators; iA++) {
            tNumPrecs[iA] = this.precLists[iA].length + 1;

            tPrecLists[iA] = new int[tNumPrecs[iA]];
            int iP;
            for (iP = 0; iP < tNumPrecs[iA] - 1; iP++) {
                tPrecLists[iA][iP] = this.precLists[iA][iP];
            }
            tPrecLists[iA][iP] = reachable[iA];

            tAddLists[iA] = new int[this.addLists[iA].length + 1];
            int iE;
            for (iE = 0; iE < this.addLists[iA].length; iE++) {
                tAddLists[iA][iE] = this.addLists[iA][iE];
            }
            tAddLists[iA][iE] = reached[iA];


            tDelLists[iA] = new int[this.delLists[iA].length + 1];
            int iD;
            for (iD = 0; iD < this.delLists[iA].length; iD++) {
                tDelLists[iA][iD] = this.delLists[iA][iD];
            }
            tDelLists[iA][iD] = unreached[iA];

            tOpNames[iA] = opNames[iA];
            tCosts[iA] = costs[iA];
        }

        /**
         * Prepare one action for every method. Its preconditions represent one subtask that has been reached through
         * composition. Its effect marks the task decomposed by the method as composed.
         */
        for (int iM = this.numOfOperators; iM < numAnM; iM++) {
            ProMethod m = this.getMethod(iM);
            tNumPrecs[iM] = m.subtasks.length;
            tPrecLists[iM] = new int[tNumPrecs[iM]];

            for (int iSubTask = 0; iSubTask < m.subtasks.length; iSubTask++) {
                Task t = m.subtasks[iSubTask];
                int taskIndex = ProgressionNetwork.taskToIndex.get(t);
                tPrecLists[iM][iSubTask] = reached[taskIndex];
            }

            int compTaskIndex = ProgressionNetwork.taskToIndex.get(m.m.abstractTask());
            tAddLists[iM] = new int[1];
            tAddLists[iM][0] = reached[compTaskIndex];
            tDelLists[iM] = new int[1];
            tDelLists[iM][0] = unreached[compTaskIndex];

            tOpNames[iM] = m.m.name() + "@" + m.m.abstractTask().shortInfo();
            tCosts[iM] = this.methodCosts;
        }
        this.numPrecs = tNumPrecs;
        this.precLists = tPrecLists;
        this.addLists = tAddLists;
        this.opNames = tOpNames;
        this.costs = tCosts;

        // extend delete lists with empty arrays (new ops have no del effects)
        /*for (int i = 0; i < this.delLists.length; i++)
            tDelLists[i] = this.delLists[i];
        for (int i = this.delLists.length; i < tDelLists.length; i++)
            tDelLists[i] = new int[0];*/
        this.delLists = tDelLists;

        this.numOfStateFeatures = numExtenedStateFeatures;
        this.numOfOperators = numAnM;

        int[] tIndexToMutexGroup = new int[numExtenedStateFeatures];
        for (int i = 0; i < indexToMutexGroup.length; i++)
            tIndexToMutexGroup[i] = indexToMutexGroup[i];

        int group = indexToMutexGroup[indexToMutexGroup.length - 1] + 1;
        for (int i = indexToMutexGroup.length; i < tIndexToMutexGroup.length; i += 2) { // new vars have 2 vals each
            tIndexToMutexGroup[i] = group;
            tIndexToMutexGroup[i + 1] = group++;
        }
        indexToMutexGroup = tIndexToMutexGroup;

        this.removeDublicates(false);
        this.calcMutexGroupIndices();
        this.calcRanges();
        this.calcInverseMappings();
        this.calcExtendedDelLists();

        String[] tFactStrs = new String[numOfStateFeatures];

        for (int i = 0; i < firstHtnIndex; i++) {
            tFactStrs[i] = factStrs[i];
        }

        int current = firstHtnIndex;
        for (int i = 0; i < numOfNonHtnActions; i++) {
            assert current == unreachable[i];
            tFactStrs[current++] = "unreachable-" + opNames[i];
            assert current == reachable[i];
            tFactStrs[current++] = "reachable-" + opNames[i];
        }

        assert current == firstBurIndex;
        for (int i = 0; i < numTasks; i++) {
            assert current == unreached[i];
            tFactStrs[current++] = "unreached-" + ProgressionNetwork.indexToTask[i].shortInfo();
            assert current == reached[i];
            tFactStrs[current++] = "reached-" + ProgressionNetwork.indexToTask[i].shortInfo();
        }
        assert (current - 1) == lastOverallIndex;

        factStrs = tFactStrs;

        int varI = varNames.length - 1;
        int varnum = Integer.parseInt(varNames[varI].substring(3));
        String[] tNames = new String[numOfVars];
        for (int i = 0; i < varNames.length; i++)
            tNames[i] = varNames[i];
        for (int i = varNames.length; i < tNames.length; i++)
            tNames[i] = "var" + ++varnum;
        varNames = tNames;
        //System.out.println(this.toString());
        assert (this.correctModel(true));
    }

    @Override
    public BitSet initS0() {
        return (BitSet) this.s0mask.clone();
    }

    @Override
    public void setReachable(BitSet bSet, int i) {
        bSet.set(this.reachable[i]);
        bSet.set(this.unreachable[i], false);
    }

    @Override
    public void setReached(BitSet bSet, int i) {
        bSet.set(this.reached[i]);
        bSet.set(this.unreached[i], false);
    }

    private int createMethodLookupTable(HashMap<Task, List<ProMethod>> methods) {
        int methodID = this.numOfOperators;
        HashMap<ProMethod, Integer> MethodToIndex = new HashMap<>();

        // count methods, create array
        int anzMethods = 0;
        for (List<ProMethod> val : methods.values())
            anzMethods += val.size();

        this.IndexToMethod = new ProMethod[anzMethods];

        for (List<ProMethod> val : methods.values())
            for (ProMethod m : val) {
                assert (!MethodToIndex.containsKey(m));
                MethodToIndex.put(m, methodID);
                m.methodID = methodID;
                this.setMethodIndex(methodID, m);
                methodID++;
            }

        return methodID;
    }

    private ProMethod getMethod(int index) {
        return IndexToMethod[index - this.numOfOperators];
    }

    private void setMethodIndex(int index, ProMethod m) {
        IndexToMethod[index - this.numOfOperators] = m;
    }
}
