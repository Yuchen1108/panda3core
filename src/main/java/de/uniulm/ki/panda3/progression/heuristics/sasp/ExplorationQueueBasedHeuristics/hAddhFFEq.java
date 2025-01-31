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

package de.uniulm.ki.panda3.progression.heuristics.sasp.ExplorationQueueBasedHeuristics;

import de.uniulm.ki.panda3.progression.heuristics.sasp.SasHeuristic;
import de.uniulm.ki.panda3.progression.htn.representation.SasPlusProblem;
import de.uniulm.ki.panda3.util.fastIntegerDataStructures.UUIntPairPriorityQueue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Created by dh on 22.06.17.
 */
public class hAddhFFEq extends SasHeuristic {
    private final SasHeuristic.SasHeuristics heuristic;
    private final int[] precLessOps;
    private final int[] hValPropInit;
    SasPlusProblem p;
    private int[] unsatPrecs;

    @Override
    public String toString() {
        if (heuristic == SasHeuristics.hAdd)
            return "hAdd-EQ";
        else
            return "hFF-EQ";
    }

    private int numGoals;
    private int[] hValOp;
    private int[] hValProp;
    private int[] reachedBy;

    public hAddhFFEq(SasPlusProblem p, SasHeuristic.SasHeuristics heuristic) {
        this.heuristic = heuristic;
        this.helpfulOps = new BitSet();
        this.p = p;
        List<Integer> tempPrecLess = new ArrayList<>();
        for (int i = 0; i < p.numOfOperators; i++) {
            if (p.precLists[i].length == 0)
                tempPrecLess.add(i);
        }
        precLessOps = new int[tempPrecLess.size()];
        for (int i = 0; i < tempPrecLess.size(); i++)
            precLessOps[i] = tempPrecLess.get(i);
        this.hValPropInit = new int[p.numOfStateFeatures];
        for (int i = 0; i < hValPropInit.length; i++)
            hValPropInit[i] = cUnreachable;//todo: precless actions
    }

    @Override
    public int calcHeu(BitSet s0, BitSet g) {
        if (g.cardinality() == 0)
            return 0;
        if (heuristic == SasHeuristics.hFF)
            helpfulOps = new BitSet(p.numOfOperators);
        this.unsatPrecs = p.numPrecs.clone();
        this.numGoals = g.cardinality();

        this.hValOp = p.costs.clone();
        this.hValProp = hValPropInit.clone();
        this.reachedBy = new int[p.numOfStateFeatures];

        UUIntPairPriorityQueue queue = new UUIntPairPriorityQueue();
        for (int f = s0.nextSetBit(0); f >= 0; f = s0.nextSetBit(f + 1)) {
            queue.add(0, f);
            hValProp[f] = 0;
        }
        // actions without preconditions
        for (int a : precLessOps) {
            for (int f : p.addLists[a]) {
                if (hValProp[f] > p.costs[a]) {
                    hValProp[f] = p.costs[a];
                    queue.add(hValProp[f], f);
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] pair = queue.minPair();
            int pVal = pair[0];
            int prop = pair[1];
            if (hValProp[prop] < pVal)
                continue;
            if (g.get(prop) && (--numGoals == 0)) {
                if (heuristic == SasHeuristics.hAdd)
                    return getAddVal(g);
                else
                    return getFFVal(s0, g);
            }
            for (int op : p.precToTask[prop]) {
                hValOp[op] += pVal;
                if (--unsatPrecs[op] == 0) {
                    for (int f : p.addLists[op]) {
                        if (hValOp[op] < hValProp[f]) {
                            hValProp[f] = hValOp[op];
                            reachedBy[f] = op; // only used by FF
                            queue.add(hValProp[f], f);
                        }
                    }
                }
            }
        }
        return cUnreachable;
    }

    private int getAddVal(BitSet g) {
        int hVal = 0;
        for (int f = g.nextSetBit(0); f >= 0; f = g.nextSetBit(f + 1)) {
            assert hValProp[f] != cUnreachable;
            hVal += hValProp[f];
        }

        return hVal;
    }

    private int getFFVal(BitSet s0, BitSet g) {
        BitSet markedFs = (BitSet) s0.clone();
        BitSet markedOps = new BitSet();
        for (int f = g.nextSetBit(0); f >= 0; f = g.nextSetBit(f + 1)) {
            assert hValProp[f] != cUnreachable;

            // adapted HTN version of helpful actions
            //System.out.println(p.opNames[reachedBy[f]]);
            helpfulOps.set(reachedBy[f]);

            markRelaxedPlan(markedFs, markedOps, f);
        }

        int hGoal = 0;
        for (int op = markedOps.nextSetBit(0); op >= 0; op = markedOps.nextSetBit(op + 1)) {
            hGoal += p.costs[op];
        }
        return hGoal;
    }

    private void markRelaxedPlan(BitSet markedFs, BitSet markedOps, int f) {
        if (!markedFs.get(f)) {
            markedFs.set(f);
            for (int prec : p.precLists[reachedBy[f]]) {
                markRelaxedPlan(markedFs, markedOps, prec);
            }
            markedOps.set(reachedBy[f]);

            // classical version of helpful actions
                /*int op = reachedBy[f];
                if (hValOp[op] == p.costs[op]) { // the preconditions of op are free (i.e. in s0)
                    helpfulOps.set(op);
                }*/
        }
    }
}
