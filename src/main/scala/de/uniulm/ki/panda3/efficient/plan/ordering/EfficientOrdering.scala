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

package de.uniulm.ki.panda3.efficient.plan.ordering

import de.uniulm.ki.panda3.symbolic.plan.ordering.TaskOrdering._

/**
  * The assumption is, that there are exactly sz(orderingConstraints) many tasks, which are numbered 0..sz(orderingConstraints)-1
  *
  * The matrix contained in this object describes the relation between two tasks using the constants of the companion object [[de.uniulm.ki.panda3.symbolic.plan.ordering.TaskOrdering]]
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
class EfficientOrdering(val orderingConstraints: Array[Array[Byte]] = Array(), var isConsistent: Boolean = true) extends PartialOrdering[Int] {

  /**
    * Propagate newly inserted ordering constraints using Bellman-Ford
    */
  private def propagate(edgesFrom: Array[Int], edgesTo: Array[Int]): Unit = {
    assert(edgesFrom.length == edgesTo.length)
    var edge = 0
    //println("E " + edgesFrom.length + " " + edgesTo.length + " " + orderingConstraints.length*orderingConstraints.length)
    while (edge < edgesFrom.length && isConsistent) {
      var from = 0
      val ordEdgeFromEdgeTo = orderingConstraints(edgesFrom(edge))(edgesTo(edge))
      while (from < orderingConstraints.length && isConsistent) {
        val ordFromEdgeFrom = orderingConstraints(from)(edgesFrom(edge))
        if (ordFromEdgeFrom != DONTKNOW) {
          var to = 0
          while (to < orderingConstraints.length && isConsistent) {
            // check whether from -> edgesFrom[i] -> edgesTo[i] -> to entails something
            val ordEdgeToTo = orderingConstraints(edgesTo(edge))(to)
            if (ordEdgeToTo != DONTKNOW) {
              // check whether "before" is implied
              if (ordFromEdgeFrom <= SAME && ordEdgeFromEdgeTo <= SAME && ordEdgeToTo <= SAME) {
                val inferredOrdering = math.min(ordFromEdgeFrom, math.min(ordEdgeFromEdgeTo, ordEdgeToTo))
                if (orderingConstraints(from)(to) == DONTKNOW || orderingConstraints(from)(to) == inferredOrdering) orderingConstraints(from)(to) = inferredOrdering.toByte
                else isConsistent = false
              }
              // check whether "after" is implied
              if (ordFromEdgeFrom >= SAME && ordEdgeFromEdgeTo >= SAME && ordEdgeToTo >= SAME) {
                val inferredOrdering = math.max(ordFromEdgeFrom, math.max(ordEdgeFromEdgeTo, ordEdgeToTo))
                if (orderingConstraints(from)(to) == DONTKNOW || orderingConstraints(from)(to) == inferredOrdering) orderingConstraints(from)(to) = inferredOrdering.toByte
                else isConsistent = false
              }
            }
            to += 1
          }
        }
        from += 1
      }
      edge += 1
    }
  }

  // Functions needed to be a partial ordering
  override def tryCompare(x: Int, y: Int): Option[Int] =
    if (x >= orderingConstraints.length || y >= orderingConstraints.length) None
    else if (orderingConstraints(x)(y) == DONTKNOW) None else Some(orderingConstraints(x)(y))

  override def lteq(x: Int, y: Int): Boolean = if (x >= orderingConstraints.length || y >= orderingConstraints.length) false else orderingConstraints(x)(y) <= SAME


  /**
    * deep-clone this CSP
    */
  def copy(): EfficientOrdering = addPlanSteps(0)

  /**
    * add the given amount of plan steps without any connection to the rest of the plan steps
    *
    * the new plan steps will be numbered sz(orderingConstraints) .. sz(orderingConstraints) + newPlanSteps - 1
    */
  def addPlanSteps(newPlanSteps: Int): EfficientOrdering = addPlanStepsWithMaybeFromBase(fromBase = false, -1, None, newPlanSteps)

  /**
    * indexBaseInNew will assume the index of oldPlanStep, while the newly added plan steps will receive the next sz(internalOrdering)-1 numbers
    */
  def addPlanStepsFromBase(basePlanStep: Int, newPlanSteps: Int, precomputedOrderingMatrix: Array[Array[Byte]]): EfficientOrdering = {
    assert(precomputedOrderingMatrix.length == newPlanSteps)
    addPlanStepsWithMaybeFromBase(fromBase = true, basePlanStep, Some(precomputedOrderingMatrix), newPlanSteps)
  }


  /**
    * adds the ordering constraint before < after to the ordering. This will automatically re-compute the transitive hull of the ordering relation
    */
  def addOrderingConstraint(before: Int, after: Int): Unit =
    if (orderingConstraints(before)(after) == SAME || orderingConstraints(before)(after) == AFTER) isConsistent = false
    else {
      orderingConstraints(before)(after) = BEFORE
      orderingConstraints(after)(before) = AFTER
      propagate(Array(before, after), Array(after, before))
    }

  /**
    * removes a plan step entirely from the ordering, while preserving all ordering constraints that went through it.
    * All plan steps with an index greater than ps will be renumbered s.t. the numbers are again contiguous, i.e. 1 will be substracted from them
    *
    * E.g. if 1 is removed from {0<1<2,3}, the result will be {0<1,2}, not {0,1,2}
    */
  def removePlanStep(ps: Int): EfficientOrdering = {
    val newOrdering = new Array[Array[Byte]](orderingConstraints.length - 1)
    var i = 0
    while (i < newOrdering.length) {
      newOrdering(i) = new Array[Byte](newOrdering.length)
      var j = 0
      while (j < newOrdering.length) {
        newOrdering(i)(j) = orderingConstraints(i + (if (i >= ps) 1 else 0))(j + (if (j >= ps) 1 else 0))
        j += 1
      }
      i += 1
    }
    new EfficientOrdering(newOrdering, isConsistent)
  }


  /**
    * internal function that actually performs the copying-around needed to copy, add, delete and replace plan steps.
    */
  private def addPlanStepsWithMaybeFromBase(fromBase: Boolean, base: Int, internalOrdering: Option[Array[Array[Byte]]], newPlanSteps: Int): EfficientOrdering = {
    val originalSize = orderingConstraints.length
    val newOrdering = new Array[Array[Byte]](originalSize + (if (fromBase) internalOrdering.get.length else newPlanSteps))
    var i = 0
    while (i < originalSize) {
      newOrdering(i) = new Array[Byte](newOrdering.length)
      var j = 0
      while (j < originalSize) {
        // copy the original matrix
        newOrdering(i)(j) = orderingConstraints(i)(j)
        j += 1
      }
      while (j < newOrdering.length) {
        // copy the original matrix
        if (fromBase && i != base) newOrdering(i)(j) = orderingConstraints(i)(base) else newOrdering(i)(j) = DONTKNOW
        j += 1
      }
      i += 1
    }
    while (i < newOrdering.length) {
      newOrdering(i) = new Array[Byte](newOrdering.length)
      var j = 0
      while (j < newOrdering.length) {
        if (i == j) newOrdering(i)(j) = SAME
        else if (!fromBase) newOrdering(i)(j) = DONTKNOW
        else if (j < originalSize) {
          // new planStep vs old planstep
          if (j != base) newOrdering(i)(j) = orderingConstraints(base)(j) else newOrdering(i)(j) = DONTKNOW
        } else {
          // both planSteps are new
          val iIndexOnNewOrdering = i - originalSize
          val jIndexOnNewOrdering = j - originalSize
          // apply
          newOrdering(i)(j) = internalOrdering.get(iIndexOnNewOrdering)(jIndexOnNewOrdering)
        }
        j += 1
      }
      i += 1
    }
    new EfficientOrdering(newOrdering, isConsistent)
  }

  def minimalOrderingConstraintsWithoutInitAndGoal(): Array[(Int, Int)] = if (!isConsistent) Array()
  else {
    val ord = orderingConstraints.map(_.clone())

    // run floyd-warshall backwards
    for (from <- ord.indices; middle <- ord.indices)
      if (ord(from)(middle) != DONTKNOW)
        for (to <- ord.indices)
          if (from != middle && to != middle && from != to)
            (ord(from)(middle), ord(middle)(to)) match {
              case (AFTER, AFTER) | (SAME, AFTER) | (AFTER, SAME)     => ord(from)(to) = DONTKNOW
              case (BEFORE, BEFORE) | (SAME, BEFORE) | (BEFORE, SAME) => ord(from)(to) = DONTKNOW
              case (_, _)                                             => ()
            }
    (for (from <- 2 until ord.length; to <- 2 until ord.length; if ord(from)(to) == BEFORE) yield (from, to)).toArray
  }


  def existsLinearisationWithPropertyFold[A](initialValue: A, foldOperation: (A, Int) => (A, Boolean)): Boolean = {
    // sources
    val inDegree: Array[Int] = new Array[Int](orderingConstraints.length)
    orderingConstraints.indices foreach { i => orderingConstraints(i).indices foreach { j => if (orderingConstraints(i)(j) == BEFORE)  inDegree(j) += 1 } }

    def dfs(sources: Array[Int], value: A, processed: Int): Boolean = if (processed == inDegree.length) true
    else {
      var i = 0
      var found = false
      while (i < sources.length && !found) {
        if (sources(i) == 0) {
          // iterate through source i
          val (newValue, ok) = foldOperation(value, i)
          if (ok) {
            val newSources = sources.clone()

            var j = 0
            while (j < sources.length) {
              if (orderingConstraints(i)(j) == BEFORE) newSources(j) -= 1
              j += 1
            }
            // not a source any more
            newSources(i) = -1
            // if there is a linearisation from here: we found one
            found = dfs(newSources, newValue, processed + 1)
          }
        }
        i += 1
      }
      found
    }


    dfs(inDegree, initialValue, 0)
  }
}
