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

package de.uniulm.ki.panda3.symbolic.sat.verify

import de.uniulm.ki.panda3.configuration.Timings
import de.uniulm.ki.panda3.symbolic.domain.{DecompositionMethod, Task}
import de.uniulm.ki.panda3.symbolic.plan.element.PlanStep
import de.uniulm.ki.panda3.symbolic.sat.verify.sogoptimiser.{GreedyNumberOfChildrenFromTotallyOrderedOptimiser, SOGOptimiser}
import de.uniulm.ki.util.SimpleDirectedGraph

import scala.collection.{Seq, mutable}

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait TreeEncoding extends PathBasedEncoding[Unit, Unit]{

  def optimiser : SOGOptimiser

    override protected def computeTaskSequenceArrangement(possibleMethods: Array[DecompositionMethod],
                                                        possiblePrimitives: scala.Seq[Task]): (Array[Array[Int]], Array[Int], Array[Set[Task]], Unit) = {
    val methodTaskGraphs = (possibleMethods map { _.subPlan.orderingConstraints.fullGraph }) ++ (
      possiblePrimitives map { t => SimpleDirectedGraph(PlanStep(-1, t, Nil) :: Nil, Nil) })

    // TODO we are currently mapping plansteps, maybe we should prefer plansteps with identical tasks to be mapped together
    //println("MINI " + possibleMethods.length + " " + possiblePrimitives.length)
    val lb = if (methodTaskGraphs.nonEmpty) methodTaskGraphs map { _.vertices count { _.schema.isAbstract } } max else 0
    // TODO what to do?
    timeCapsule start Timings.SOG_OPTIMISATION
    //val g = DirectedGraph.minimalInducedSuperGraph[PlanStep](methodTaskGraphs) //, minimiseAbstractTaskOccurencesMetric)
    //val g = GreedyNumberOfAbstractChildrenOptimiser.minimalSOG(methodTaskGraphs)
    val g = optimiser.minimalSOG(methodTaskGraphs)
    //val g = NativeOptimiser.minimalSOG(methodTaskGraphs)
    //println("done")
    timeCapsule stop Timings.SOG_OPTIMISATION
    val minimalSuperGraph = g._1
    val planStepToIndexMappings: Seq[Map[PlanStep, Int]] = g._2
    val topologicalOrdering = minimalSuperGraph.topologicalOrdering.get

    val (methodMappings, primitiveMappings) = planStepToIndexMappings map { m => m map { case (ps, node) => (ps, topologicalOrdering.indexOf(node)) } } splitAt possibleMethods.length

    val childrenIndicesToPossibleTasks = minimalSuperGraph.vertices map { _ => new mutable.HashSet[Task]() }

    val tasksPerMethodToChildrenMapping = methodMappings.zipWithIndex map { case (mapping, methodIndex) =>
      val methodPlanSteps = possibleMethods(methodIndex).subPlan.planStepsWithoutInitGoal
      (methodPlanSteps map { ps =>
        childrenIndicesToPossibleTasks(mapping(ps)) add ps.schema
        mapping(ps)
      }).toArray
    } toArray

    val childrenForPrimitives = primitiveMappings.zipWithIndex map { case (mapping, primitiveIndex) =>
      assert(mapping.size == 1)
      childrenIndicesToPossibleTasks(mapping.head._2) add mapping.head._1.schema
      mapping.head._2
    } toArray

    //println("\n\nGraph minisation")
    //println(childrenIndicesToPossibleTasks map {s => s map {t => t.name + " " + t.isAbstract} mkString " "} mkString "\n")

    (tasksPerMethodToChildrenMapping, childrenForPrimitives, childrenIndicesToPossibleTasks map { _.toSet } toArray, ())
  }

}
