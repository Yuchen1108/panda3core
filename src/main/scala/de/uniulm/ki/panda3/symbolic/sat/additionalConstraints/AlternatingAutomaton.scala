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

package de.uniulm.ki.panda3.symbolic.sat.additionalConstraints

import de.uniulm.ki.panda3.symbolic.domain.{Domain, Task}
import de.uniulm.ki.panda3.symbolic.logic.Predicate

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
case class AlternatingAutomaton(initialState: LTLFormula, transitions: Map[LTLFormula, Map[(Task, Boolean, Set[Predicate]), PositiveBooleanFormula]])
  extends LTLAutomaton[LTLFormula, PositiveBooleanFormula] {
  /** adjacency list of the graph */
  val edges: Map[LTLFormula, Seq[LTLFormula]] = transitions map { case (a, b) => a -> b.values.flatMap(_.elementaryExpressions).toSeq.distinct }

  def transitionTest(edgeTo: PositiveBooleanFormula, to: LTLFormula): Boolean = edgeTo.elementaryExpressions contains to

  lazy val prune: AlternatingAutomaton = {
    val remainingStates = reachable(initialState) + initialState

    AlternatingAutomaton(initialState, remainingStates map { r => r -> transitions(r) } toMap)
  }
}

trait PositiveBooleanFormula {
  def elementaryExpressions: Set[LTLFormula]

  def allSubformulae: Set[PositiveBooleanFormula]
}

case class PositiveElementary(ltlFormula: LTLFormula) extends PositiveBooleanFormula {
  lazy val elementaryExpressions: Set[LTLFormula] = Set(ltlFormula)

  def allSubformulae: Set[PositiveBooleanFormula] = Set(this)
}

case class PositiveAnd(subformulae: Seq[PositiveBooleanFormula]) extends PositiveBooleanFormula {
  lazy val elementaryExpressions: Set[LTLFormula] = subformulae.flatMap(_.elementaryExpressions).toSet

  def allSubformulae: Set[PositiveBooleanFormula] = Set(this) ++ subformulae.flatMap(_.allSubformulae)
}

case class PositiveOr(subformulae: Seq[PositiveBooleanFormula]) extends PositiveBooleanFormula {
  lazy val elementaryExpressions: Set[LTLFormula] = subformulae.flatMap(_.elementaryExpressions).toSet

  def allSubformulae: Set[PositiveBooleanFormula] = Set(this) ++ subformulae.flatMap(_.allSubformulae)
}

object AlternatingAutomaton {
  def apply(domain: Domain, formula: LTLFormula): AlternatingAutomaton = {

    // construct the automaton
    /*def dfs(states: Set[LTLFormula], newStates: Set[LTLFormula], transitions: Map[LTLFormula, Map[(Task, Boolean, Set[Predicate]), LTLFormula]]):
    (Set[LTLFormula], Map[LTLFormula, Map[(Task, Boolean, Set[Predicate]), LTLFormula]]) = {
      // get the transitions for every new state
      val newTransitions: Map[LTLFormula, Map[(Task, Boolean, Set[Predicate]), LTLFormula]] = newStates map { state =>
        val allPrimitiveStates = state.allStates
        // iterate over all tasks that might be executed
        val transition: Map[(Task, Boolean, Set[Predicate]), LTLFormula] = (domain.primitiveTasks :+ TaskAfterLastOne) flatMap { nextTask =>
          allPrimitiveStates flatMap { primState =>
            true :: false :: Nil map { last => ((nextTask, last, primState), state.delta(nextTask, primState, last).simplify) }
          }
        } toMap

        state -> transition
      } toMap

      val targetStates = (newTransitions flatMap { _._2.values } filterNot states.contains).toSet

      if (targetStates.isEmpty) (states, transitions ++ newTransitions)
      else dfs(states ++ targetStates, targetStates.toSet, transitions ++ newTransitions)
    }*/

    //val (states, transitions) = dfs(Set(formula), Set(formula), Map())

    val states = formula.allSubformulae + LTLTrue + LTLFalse

    val transition: Map[LTLFormula, Map[(Task, Boolean, Set[Predicate]), PositiveBooleanFormula]] = states map { state =>
      val allPrimitiveStates = state.allStates
      // iterate over all tasks that might be executed
      val transition: Map[(Task, Boolean, Set[Predicate]), PositiveBooleanFormula] = (domain.primitiveTasks :+ TaskAfterLastOne) flatMap { nextTask =>
        allPrimitiveStates flatMap { primState =>
          true :: false :: Nil map { last =>

            val s = state.delta(nextTask, primState, last).simplify.toPositiveBooleanFormula
            s.elementaryExpressions foreach { e => assert(states contains e) }

            //println("to " + s + " FROM " + state + " on " + nextTask + " last: " + last + " at " + primState)
            ((nextTask, last, primState), s)
          }
        }
      } toMap

      state -> transition
    } toMap

    val automaton = AlternatingAutomaton(formula, transition)

    automaton.prune
  }

}
