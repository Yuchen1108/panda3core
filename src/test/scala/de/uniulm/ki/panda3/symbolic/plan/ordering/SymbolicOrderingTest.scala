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

package de.uniulm.ki.panda3.symbolic.plan.ordering

import de.uniulm.ki.panda3.symbolic.domain.{ConstantActionCost, ReducedTask, Task}
import de.uniulm.ki.panda3.symbolic.logic.{And, Literal}
import de.uniulm.ki.panda3.symbolic.plan.element.{OrderingConstraint, PlanStep}
import org.scalatest.FlatSpec


/**
  *
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
// scalastyle:off magic.number
class SymbolicOrderingTest extends FlatSpec {

  def getOrdering(i: Int, j: Int): OrderingConstraint = OrderingConstraint(getPlanStep(i), getPlanStep(j))

  def getPlanStep(i: Int): PlanStep = PlanStep(i, ReducedTask("", isPrimitive = false, Nil, Nil, Vector(), And[Literal](Vector()), And[Literal](Vector()), ConstantActionCost(0)), Vector())

  /** get a list of plansetps ranging von 0 to i-1 */
  def getPlanStepList(i: Int): Seq[PlanStep] = if (i == 0) Nil else getPlanStepList(i - 1) :+ getPlanStep(i - 1)

  "Orderings inference" must "allow simple inference" in {
    // a dummy plan
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2), getPlanStepList(3))

    assert(order.isConsistent)
    assert(order.lteq(getPlanStep(0), getPlanStep(2)))
  }



  it must "allow almost simple inference" in {
    // a dummy plan
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(3, 4) :+ getOrdering(3, 5) :+ getOrdering(6, 2) :+
                               getOrdering(2, 7) :+ getOrdering(7, 3), getPlanStepList(8))
    assert(order.isConsistent)
    assert(order.lteq(getPlanStep(0), getPlanStep(2)))
    assert(order.lteq(getPlanStep(0), getPlanStep(3)))
    assert(order.lteq(getPlanStep(0), getPlanStep(4)))
    assert(order.lteq(getPlanStep(0), getPlanStep(5)))
    assert(order.lteq(getPlanStep(6), getPlanStep(4)))
    assert(order.lteq(getPlanStep(6), getPlanStep(7)))
    assert(!order.lteq(getPlanStep(4), getPlanStep(5)))
    assert(!order.lteq(getPlanStep(5), getPlanStep(1)))
    assert(order.tryCompare(getPlanStep(6), getPlanStep(1)) === None)
    assert(order.tryCompare(getPlanStep(5), getPlanStep(7)) === Some(1))
    assert(order.tryCompare(getPlanStep(6), getPlanStep(4)) === Some(-1))
  }



  "Orderings update" must "allow incremental calculations" in {
    // a dummy plan
    val order1 = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2), getPlanStepList(3))

    val order2 = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(3, 4) :+ getOrdering(3, 5) :+ getOrdering(6, 2) :+ getOrdering(2, 7),
                              getPlanStepList(8))
    order2.initialiseExplicitly(5, 5, order1.arrangement())

    assert(order2.isConsistent)
    assert(order2.lteq(getPlanStep(0), getPlanStep(2)))
    assert(order2.lteq(getPlanStep(0), getPlanStep(3)))
    assert(order2.lteq(getPlanStep(0), getPlanStep(4)))
    assert(order2.lteq(getPlanStep(0), getPlanStep(5)))
    assert(order2.lteq(getPlanStep(6), getPlanStep(4)))
    assert(order2.lteq(getPlanStep(6), getPlanStep(7)))
    assert(order2.tryCompare(getPlanStep(6), getPlanStep(1)) === None)
    assert(order2.tryCompare(getPlanStep(5), getPlanStep(7)) === None)
    assert(order2.tryCompare(getPlanStep(6), getPlanStep(4)) === Some(-1))

    val order3 = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(3, 4) :+ getOrdering(3, 5) :+ getOrdering(6, 2) :+ getOrdering(2, 7) :+
                                getOrdering(7, 3), getPlanStepList(8))
    order3.initialiseExplicitly(1, 0, order2.arrangement())

    assert(order3.isConsistent)
    assert(order3.lteq(getPlanStep(0), getPlanStep(2)))
    assert(order3.lteq(getPlanStep(0), getPlanStep(3)))
    assert(order3.lteq(getPlanStep(0), getPlanStep(4)))
    assert(order3.lteq(getPlanStep(0), getPlanStep(5)))
    assert(order3.lteq(getPlanStep(6), getPlanStep(4)))
    assert(order3.lteq(getPlanStep(6), getPlanStep(7)))
    assert(order3.tryCompare(getPlanStep(6), getPlanStep(1)) === None)
    assert(order3.tryCompare(getPlanStep(5), getPlanStep(7)) === Some(1))
    assert(order3.tryCompare(getPlanStep(6), getPlanStep(4)) === Some(-1))

  }


  it must "allow add Ordering" in {
    // a dummy plan
    val order1 = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2), getPlanStepList(3))

    assert(order1.isConsistent)
    assert(order1.lteq(getPlanStep(0), getPlanStep(2)))

    val order2 = order1.addOrdering(getPlanStep(0), getPlanStep(1)).addOrdering(getPlanStep(1), getPlanStep(2)).addOrdering(getPlanStep(2), getPlanStep(3))
      .addOrdering(getPlanStep(3), getPlanStep(4)).addOrdering(getPlanStep(3), getPlanStep(5)).addOrdering(getPlanStep(6), getPlanStep(2)).addOrdering(getPlanStep(2), getPlanStep(7))

    order2.initialiseExplicitly(5, 5, order1.arrangement())

    assert(order2.isConsistent)
    assert(order2.lteq(getPlanStep(0), getPlanStep(2)))
    assert(order2.lteq(getPlanStep(0), getPlanStep(3)))
    assert(order2.lteq(getPlanStep(0), getPlanStep(4)))
    assert(order2.lteq(getPlanStep(0), getPlanStep(5)))
    assert(order2.lteq(getPlanStep(6), getPlanStep(4)))
    assert(order2.lteq(getPlanStep(6), getPlanStep(7)))
    assert(order2.tryCompare(getPlanStep(6), getPlanStep(1)) === None)
    assert(order2.tryCompare(getPlanStep(5), getPlanStep(7)) === None)
    assert(order2.tryCompare(getPlanStep(6), getPlanStep(4)) === Some(-1))

    val order3 = order2.addOrdering(getPlanStep(7), getPlanStep(3))

    order3.initialiseExplicitly(1, 0, order2.arrangement())

    assert(order3.isConsistent)
    assert(order3.lteq(getPlanStep(0), getPlanStep(2)))
    assert(order3.lteq(getPlanStep(0), getPlanStep(3)))
    assert(order3.lteq(getPlanStep(0), getPlanStep(4)))
    assert(order3.lteq(getPlanStep(0), getPlanStep(5)))
    assert(order3.lteq(getPlanStep(6), getPlanStep(4)))
    assert(order3.lteq(getPlanStep(6), getPlanStep(7)))
    assert(order3.tryCompare(getPlanStep(6), getPlanStep(1)) === None)
    assert(order3.tryCompare(getPlanStep(5), getPlanStep(7)) === Some(1))
    assert(order3.tryCompare(getPlanStep(6), getPlanStep(4)) === Some(-1))

  }


  "Orderings inconsistencies" must "find simple inconsistencies" in {
    // a dummy plan
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 0), getPlanStepList(3))

    assert(!order.isConsistent)
  }

  it must "find tricky inconsistencies" in {
    // a dummy plan
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 0), getPlanStepList(2))

    assert(!order.isConsistent)
  }

  it must "find complex inconsistencies" in {
    // a dummy plan
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(3, 4) :+ getOrdering(3, 5) :+ getOrdering(6, 2) :+ getOrdering(2, 7) :+
                               getOrdering(7, 3) :+ getOrdering(5, 1), getPlanStepList(8))
    assert(!order.isConsistent)
  }


  "Reduced Orderings" must "be computable" in {
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(3, 4) :+ getOrdering(3, 5) :+ getOrdering(6, 2) :+
                                           getOrdering(2, 7) :+ getOrdering(7, 3) :+ getOrdering(6, 5), getPlanStepList(8))

    val minimalOrdering = order.minimalOrderingConstraints()

    // check whether the reduction is correct
    assert(minimalOrdering.size == 7)
    assert(minimalOrdering contains getOrdering(0, 1))
    assert(minimalOrdering contains getOrdering(1, 2))
    assert(minimalOrdering contains getOrdering(2, 7))
    assert(minimalOrdering contains getOrdering(7, 3))
    assert(minimalOrdering contains getOrdering(3, 4))
    assert(minimalOrdering contains getOrdering(3, 5))
    assert(minimalOrdering contains getOrdering(6, 2))
  }

  "Removing Tasks" must "be correct" in {
    val order = TaskOrdering(Vector() :+ getOrdering(0, 1) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(3, 4), getPlanStepList(5))

    val order2Removed: TaskOrdering = order.removePlanStep(getPlanStep(2))

    assert(order2Removed.tasks.size == 4)
    assert(!order2Removed.tasks.contains(getPlanStep(2)))
    assert(order2Removed.lt(getPlanStep(0), getPlanStep(3)))
    assert(order2Removed.lt(getPlanStep(0), getPlanStep(4)))
    assert(order2Removed.lt(getPlanStep(1), getPlanStep(3)))
    assert(order2Removed.lt(getPlanStep(1), getPlanStep(4)))
  }

  it must "also be correct of the transitive hull was already computed" in {
    val order = TaskOrdering(Vector() :+ getOrdering(0, 2) :+ getOrdering(1, 2) :+ getOrdering(2, 3) :+ getOrdering(2, 4), getPlanStepList(5))

    // run the initialization
    order.initialiseExplicitly()

    val order2Removed: TaskOrdering = order.removePlanStep(getPlanStep(2))

    assert(order2Removed.tasks.size == 4)
    assert(!order2Removed.tasks.contains(getPlanStep(2)))
    assert(order2Removed.lt(getPlanStep(0), getPlanStep(3)))
    assert(order2Removed.lt(getPlanStep(0), getPlanStep(4)))
    assert(order2Removed.lt(getPlanStep(1), getPlanStep(3)))
    assert(order2Removed.lt(getPlanStep(1), getPlanStep(4)))
  }
}
