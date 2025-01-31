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

package de.uniulm.ki.panda3.symbolic.domain.datastructures.hierarchicalreachability

import de.uniulm.ki.panda3.symbolic._
import de.uniulm.ki.panda3.symbolic.domain._
import de.uniulm.ki.panda3.symbolic.domain.datastructures.GroundedPrimitiveReachabilityAnalysis
import de.uniulm.ki.panda3.symbolic.logic.Variable
import de.uniulm.ki.panda3.symbolic.logic.Constant
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.plan.element.GroundTask

import scala.collection.mutable

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
case class TopDownTaskDecompositionGraph(domain: Domain, initialPlan: Plan, groundedReachabilityAnalysis: GroundedPrimitiveReachabilityAnalysis, prunePrimitive: Boolean,
                                         messageFunction : String => Unit) extends
  TaskDecompositionGraph {

  // apparently I can't annotate a type here (Map[Task, Set[GroundTask]], Map[GroundTask, Seq[GroundedDecompositionMethod]])
  lazy val (abstractTaskGroundings, groundedDecompositionMethods) = {
    // here we rely on side-effects for speed and readability

    val abstractTasksMap = new mutable.HashMap[Task, Set[GroundTask]]().withDefaultValue(Set())
    val alreadyGroundedMethods = new mutable.HashMap[DecompositionMethod, Set[Map[Variable, Constant]]].withDefaultValue(Set())
    val methodsMap = new mutable.HashMap[GroundTask, Set[GroundedDecompositionMethod]]().withDefaultValue(Set())

    def dfs(currentGroundTask: GroundTask): Unit = if (!(abstractTasksMap(currentGroundTask.task) contains currentGroundTask) && currentGroundTask.task.isAbstract) {
      // add the ground task to its map
      abstractTasksMap(currentGroundTask.task) = abstractTasksMap(currentGroundTask.task) + currentGroundTask

      // if the task is abstract, we have to ground it
      // we have a partial variable binding from the abstract task
      val possibleMethods = (domain.methodsForAbstractTasks.getOrElse(currentGroundTask.task, Nil) ++ (if (topMethod.abstractTask == currentGroundTask.task) topMethod :: Nil else Nil)) map {
        case simpleMethod: SimpleDecompositionMethod =>
          val candidateGroundings = simpleMethod.groundWithAbstractTaskGrounding(currentGroundTask)
          val entryMap = alreadyGroundedMethods(simpleMethod)
          val newGroundings = candidateGroundings filterNot { grounding => entryMap.contains(grounding.variableBinding) }
          (simpleMethod, newGroundings)
        case _                                       => noSupport(NONSIMPLEMETHOD)
      }
      // add the new methods to the map
      val flattenedPossibleMethods = possibleMethods flatMap { _._2 }
      methodsMap(currentGroundTask) = methodsMap(currentGroundTask) ++ flattenedPossibleMethods
      possibleMethods foreach { case (m, groundings) => alreadyGroundedMethods(m) = alreadyGroundedMethods(m) ++ (groundings map { _.variableBinding }) }

      // perform recursion
      flattenedPossibleMethods flatMap { _.subPlanPlanStepsToGrounded.values } foreach dfs
    }

    val time000 = System.currentTimeMillis()
    dfs(groundedTopTask)
    val time001 = System.currentTimeMillis()
    //println("Time: " + (time001 - time000))

    (abstractTasksMap.toMap, methodsMap.toMap)
  }
}
