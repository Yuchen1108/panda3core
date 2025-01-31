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

import de.uniulm.ki.panda3.symbolic.logic.Predicate
import de.uniulm.ki.panda3.symbolic.sat.IntProblem
import de.uniulm.ki.panda3.symbolic.sat.verify.{Clause, EncodingWithLinearPlan, ExistsStep, LinearPrimitivePlanEncoding}

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
case class AlternatingAutomatonFormulaEncoding(automaton: AlternatingAutomaton, id: String) extends LTLFormulaEncoding[LTLFormula, PositiveBooleanFormula]
                                                                                                    with MattmüllerDisablingGraphExtension  {

  val stateRestrictionsToIndex: Map[PositiveBooleanFormula, Int] = automaton.transitions.values.flatMap(_.values).flatMap(_.allSubformulae).toSeq.distinct.zipWithIndex.toMap

  //println(automataStatesToIndices map {case (f,i) => i + " " + f.longInfo} mkString "\n")

  protected def stateRestriction(formula: PositiveBooleanFormula, position: Int) = id + "_auto_state_restriction_" + stateRestrictionsToIndex(formula) + "_" + position

  def apply(linearEncoding: LinearPrimitivePlanEncoding): Seq[Clause] = {
    println(automaton.vertices map {_.longInfo} mkString "\n")

    val transitionRules: Seq[Clause] = linearEncoding.linearPlan.zipWithIndex flatMap { case (taskMap, position) =>
      automaton.vertices flatMap { s =>

        s.allStatesAndCounter flatMap { case (primitiveState, negativeState) =>
          val stateTrue = primitiveState map { p => linearEncoding.linearStateFeatures(position)(p) } toSeq
          val stateFalse = negativeState map { p => linearEncoding.linearStateFeatures(position)(p) } toSeq

          taskMap map { case (task, atom) =>
            val nextStateNotLast = automaton.transitions(s)((task, false, primitiveState))
            val notLast = linearEncoding.impliesLeftTrueAndFalseImpliesTrue((state(s, position) :: atom :: Nil) ++ stateTrue,
                                                                            stateFalse,
                                                                            stateRestriction(nextStateNotLast, position + 1))

            notLast
          }
        }
      }
    }

    val lastAutomationTransition: Seq[Clause] = {
      val position = linearEncoding.linearPlan.length

      val transitionRule: Seq[Clause] = automaton.vertices flatMap { s =>
        val relevantStateFeatures = s.allPredicates
        val allStates = s.allStates

        allStates flatMap { primitiveState =>
          val (stateTrue, stateFalse) = relevantStateFeatures map { l => (linearEncoding.linearStateFeatures(position)(l), primitiveState contains l) } partition { _._2 }
          // last Action, last state is handled differently ...
          val nextStateLast = automaton.transitions(s)((TaskAfterLastOne, true, primitiveState))
          val last = linearEncoding.impliesLeftTrueAndFalseImpliesTrue((state(s, position) :: Nil) ++ stateTrue.toSeq.map(_._1),
                                                                       stateFalse.toSeq map { _._1 },
                                                                       stateRestriction(nextStateLast, position + 1))

          //notLast :: last :: Nil
          last :: Nil
        }
      }

      transitionRule
    }

    val restrictionRules: Seq[Clause] = Range(1, linearEncoding.linearPlan.length + 2) flatMap { position =>
      stateRestrictionsToIndex.keys.toSeq flatMap {
        case f@PositiveElementary(elem) => linearEncoding.impliesSingle(stateRestriction(f, position), state(elem, position)) :: Nil
        case f@PositiveAnd(elems)       => linearEncoding.impliesRightAnd(stateRestriction(f, position) :: Nil, elems map { e => stateRestriction(e, position) })
        case f@PositiveOr(elems)        => linearEncoding.impliesRightOr(stateRestriction(f, position) :: Nil, elems map { e => stateRestriction(e, position) }) :: Nil
      }
    }

    val init = Clause(state(automaton.initialState, 0)) :: Nil
    val goal = automaton.vertices map {
      case LTLTrue => Clause(state(LTLTrue, linearEncoding.linearPlan.length + 1), true)
      case s       => Clause(state(s, linearEncoding.linearPlan.length + 1), false)
    }

    maintainStateAtNoPresent(linearEncoding) ++ transitionRules ++ lastAutomationTransition ++ restrictionRules ++ init ++ goal
  }

  override lazy val lTLFormula = automaton.initialState
}
