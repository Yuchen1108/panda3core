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

package de.uniulm.ki.panda3.symbolic.domain.updates

import de.uniulm.ki.panda3.symbolic.csp.VariableConstraint
import de.uniulm.ki.panda3.symbolic.domain.{DecompositionMethod, Task}
import de.uniulm.ki.panda3.symbolic.logic.{Literal, Predicate, Sort, Variable}
import de.uniulm.ki.panda3.symbolic.plan.element.PlanStep

/**
  * represents a generic domain update.
  *
  * This is an empty marker trait
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait DomainUpdate {}

case class AddTask(newTasks: Seq[Task]) extends DomainUpdate {}

case class AddVariableConstraints(newConstraints: Seq[VariableConstraint]) extends DomainUpdate {}

case class AddMethod(newMethods: Seq[DecompositionMethod]) extends DomainUpdate {}

case class AddLiteralsToInitAndGoal(literalsInit: Seq[Literal], literalsGoal: Seq[Literal], variableConstraints: Seq[VariableConstraint]) extends DomainUpdate {}

object AddLiteralsToInit {
  def apply(literals: Seq[Literal], variableConstraints: Seq[VariableConstraint]): AddLiteralsToInitAndGoal = AddLiteralsToInitAndGoal(literals, Nil, variableConstraints)
}

object AddLiteralsToGoal {
  def apply(literals: Seq[Literal], variableConstraints: Seq[VariableConstraint]): AddLiteralsToInitAndGoal = AddLiteralsToInitAndGoal(Nil, literals, variableConstraints)
}

case class AddPredicate(newPredicates: Seq[Predicate]) extends DomainUpdate

case class AddVariables(newVariables: Seq[Variable]) extends DomainUpdate

case class RemoveVariables(removedVariables: Seq[Variable]) extends DomainUpdate

case class ExchangePlanSteps(exchangeMap: Map[PlanStep, PlanStep]) extends DomainUpdate

object ExchangePlanSteps {
  def apply(oldPS: PlanStep, newPS: PlanStep): ExchangePlanSteps = ExchangePlanSteps(Map(oldPS -> newPS))
}

case class ExchangeSorts(exchangeMap: Map[Sort, Sort]) extends DomainUpdate

case class ExchangeTask(exchange: Map[Task, Task]) extends DomainUpdate

case class ExchangeTaskSchemaInMethods(exchange: Map[Task, Task]) extends DomainUpdate

case class ReduceFormula() extends DomainUpdate

case class ReduceTasks() extends DomainUpdate

case class ExchangeVariable(oldVariable: Variable, newVariable: Variable) extends DomainUpdate

case class ExchangeVariables(exchangeMap: Map[Variable, Variable]) extends DomainUpdate

case class ExchangeLiteralsByPredicate(replacement: Map[Predicate, (Predicate, Predicate)], invertedTreatment: Boolean) extends DomainUpdate

case class RemoveEffects(unnecessaryEffects: Set[(Predicate, Boolean)], invertedTreatment: Boolean) extends DomainUpdate

case class RemovePredicate(unnecessaryPredicates: Set[Predicate]) extends DomainUpdate

case class PropagateEquality(protectedVariables: Set[Variable]) extends DomainUpdate

case class SetExpandVariableConstraintsInPlans(dontExpand: Boolean) extends DomainUpdate

object DeleteCausalLinks extends DomainUpdate

object RemoveNoops extends DomainUpdate

object NoUpdate extends DomainUpdate
