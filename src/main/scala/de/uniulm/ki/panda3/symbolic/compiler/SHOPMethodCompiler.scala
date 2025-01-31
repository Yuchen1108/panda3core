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

package de.uniulm.ki.panda3.symbolic.compiler

import de.uniulm.ki.panda3.symbolic.domain._
import de.uniulm.ki.panda3.symbolic.logic.{Formula, And}
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.plan.element.{OrderingConstraint, PlanStep}

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
object SHOPMethodCompiler extends DomainTransformerWithOutInformation {

  final val SHOP_METHOD_PRECONDITION_PREFIX = "__method_precondition_"

  /** transforms all SHOP2 style methods into ordinary methods */
  override def transform(domain: Domain, plan: Plan, info: Unit): (Domain, Plan) = {
    val compiledMethods: Seq[(SimpleDecompositionMethod, Seq[Task])] = domain.decompositionMethods.zipWithIndex map {
      case (sm@SimpleDecompositionMethod(_, _, _), _)                                        => (sm, Nil)
      case (SHOPDecompositionMethod(abstractTask, subPlan, precondition, effect, name), idx) =>
        if (precondition.isEmpty && effect.isEmpty) (SimpleDecompositionMethod(abstractTask, subPlan, name), Nil)
        else {
          // generate a new schema that represents the decomposition method
          val containedVariables = (precondition.containedVariables ++ effect.containedVariables).toSeq.distinct
          val preconditionTaskSchema = GeneralTask(SHOP_METHOD_PRECONDITION_PREFIX + name + "_" + idx + "_precondition", isPrimitive = true, containedVariables,
                                                   Nil, subPlan.variableConstraints.constraints filter { _.getVariables.toSet.toSet.subsetOf(containedVariables.toSet) },
                                                   precondition, effect, ConstantActionCost(0))
          // instantiate
          val preconditionPlanStep = new PlanStep(subPlan.getFirstFreePlanStepID, preconditionTaskSchema, preconditionTaskSchema.parameters)
          // make this plan step the first actual task in the method


          val newOrderingWithPrec = subPlan.orderingConstraints.addOrderings(OrderingConstraint.allAfter(preconditionPlanStep, subPlan.planStepsWithoutInitGoal :+ subPlan.goal: _*))
              .addOrdering(subPlan.init, preconditionPlanStep)

          val additionalPlanSteps: Seq[PlanStep] = preconditionPlanStep :: Nil


          (SimpleDecompositionMethod(abstractTask, new Plan(subPlan.planSteps ++ additionalPlanSteps,
                                                            subPlan.causalLinks, newOrderingWithPrec, subPlan.variableConstraints, subPlan.init,
                                                            subPlan.goal, subPlan.isModificationAllowed, subPlan.isFlawAllowed, subPlan.planStepDecomposedByMethod,
                                                            subPlan.planStepParentInDecompositionTree, subPlan.dontExpandVariableConstraints,
                                                            subPlan.ltlConstraint), name), additionalPlanSteps map { _.schema })
        }
    }
    (Domain(domain.sorts, domain.predicates, domain.tasks ++ (compiledMethods flatMap { _._2 }), compiledMethods map { _._1 }, domain.decompositionAxioms, domain.costValues,
            domain.mappingToOriginalGrounding, domain.sasPlusRepresentation), plan)
  }
}
