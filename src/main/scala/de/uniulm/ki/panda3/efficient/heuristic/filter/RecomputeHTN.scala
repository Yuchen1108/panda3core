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

package de.uniulm.ki.panda3.efficient.heuristic.filter

import de.uniulm.ki.panda3.efficient.plan.EfficientPlan

/**
  * Returns true if, according to this test, the problem is potentially solvable and false if
  * it can *prove* that it is not.
  *
  * Semantically, it returns false (i.e., provably unsolvable)
  * if and only if the TDG-c or TDG-m heuristics return infinity.
  *
  * Technically, the test works a bit differently for efficiency reasons, but it still results
  * in the same result. It works as follows:
  * 1. First, a TDG is built in a preprocessing step (i.e., before planning, not for each node).
  * 2. For each plan, this TDG is traversed to identify the set of primitive tasks reachable
  *    from the abstract tasks in the current plan.
  * 3. With these reachable actions, a new mutex-free planning graph is built.
  *    Then, with this novel, plan-dependent planning graph, several tests are performed:
  * A. The planning goals are reachable (i.e., plan.goalPotentiallyReachable).
  * B. Are all primitive SoCS-2013 landmarks (which were identified based on the new traversal
  *    from the current plan) are still applicable (i..e, plan.allLandmarksApplicable).
  * C. All primitive tasks are containted in the new planning graph (plan.allContainedApplicable).
  * D. Do all abstract tasks admit a decomposition into a set of primitive plans?
  *    (i.e., plan.allAbstractTasksAllowed).
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
object RecomputeHTN extends Filter {
  override def isPossiblySolvable(plan: EfficientPlan): Boolean = plan.goalPotentiallyReachable && plan.allLandmarksApplicable && plan.allContainedApplicable && plan.allAbstractTasksAllowed
}
