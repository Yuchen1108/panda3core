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
import de.uniulm.ki.panda3.symbolic.compiler.SHOPMethodCompiler
import de.uniulm.ki.panda3.symbolic.domain.{DecompositionMethod, Domain, Task}
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.plan.element.PlanStep
import de.uniulm.ki.panda3.symbolic.sat.verify.sogoptimiser.{GreedyNumberOfAbstractChildrenOptimiser, OptimalBranchAndBoundOptimiser}
import de.uniulm.ki.util.{DirectedGraph, DirectedGraphDotOptions, Dot2PdfCompiler, SimpleDirectedGraph}

import scala.collection.{Seq, mutable}

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait SOGEncoding extends PathBasedEncoding[SOG, NonExpandedSOG] with LinearPrimitivePlanEncoding {

  assert(initialPlan.planStepsWithoutInitGoal.length == 1, "This formula is only correct if the initial plan has been replaced by an artificial top task, but it contained\n" + initialPlan
    .planStepsWithoutInitGoal.map(_.schema.name).mkString("\n"))

  // this is only needed in the tree encoding
  override protected def additionalClausesForMethod(layer: Int, path: Seq[Int], method: DecompositionMethod, methodString: String, methodChildrenPositions: Map[Int, Int]): Seq[Clause] = Nil

  override lazy val goalState: Seq[Clause] = goalStateOfLength(taskSequenceLength)

  //TODO: is this correct?
  override val numberOfChildrenClauses: Int = 0

  // TODO: we don't support this (yet)
  override def givenActionsFormula: Seq[Clause] = ???

  override protected def initialPayload(possibleTasks: Set[Task], path: Seq[Int]): SOG = SOG(SimpleDirectedGraph((path, possibleTasks) :: Nil, Nil))


  def minimiseAbstractTaskOccurencesMetric(g: DirectedGraph[Int], mapping: Seq[Map[PlanStep, Int]]): Int = if (g.vertices.isEmpty) 0
  else {
    val abstractTasks = g.vertices map { _ => new mutable.HashSet[Task]() } toArray

    mapping foreach { m => m filter { _._1.schema.isAbstract } foreach { case (ps, i) => abstractTasks(i) add ps.schema } }

    abstractTasks map { _.size } sum
  }

  def minimiseChildrenWithAbstractTasks(g: DirectedGraph[Int], mapping: Seq[Map[PlanStep, Int]]): Int = if (g.vertices.isEmpty) 0
  else {
    val abstractTasks = g.vertices map { _ => new mutable.HashSet[Task]() } toArray

    mapping foreach { m => m filter { _._1.schema.isAbstract } foreach { case (ps, i) => abstractTasks(i) add ps.schema } }

    (abstractTasks count { _.nonEmpty }) * 1000 + g.vertices.length
  }


  protected def computeTaskSequenceArrangement(possibleMethods: Array[DecompositionMethod], possiblePrimitives: Seq[Task]):
  (Array[Array[Int]], Array[Int], Array[Set[Task]], NonExpandedSOG) = {
    val methodTaskGraphs = (possibleMethods map { m =>
      val baseGraph = m.subPlan.orderingConstraints.fullGraph
      // TODO: hack!!
      val methodPrecs = if (omitMethodPreconditionActions && m.subPlan.planStepSchemaArrayWithoutMethodPreconditions.nonEmpty)
        baseGraph.sources filter { _.schema.effect.isEmpty } filter { _.schema.isPrimitive } filter { _.schema.name.contains(SHOPMethodCompiler.SHOP_METHOD_PRECONDITION_PREFIX) }
      else Nil

      baseGraph.filter(ps => !methodPrecs.contains(ps))
    }) ++ (
      possiblePrimitives map { t => SimpleDirectedGraph(PlanStep(-1, t, Nil) :: Nil, Nil) })

    // TODO we are currently mapping plansteps, maybe we should prefer plansteps with identical tasks to be mapped together
    //print("MINI " + possibleMethods.length + " " + possiblePrimitives.length + " ... ")
    //val lb = methodTaskGraphs map { _.vertices count { _.schema.isAbstract } } max
    timeCapsule start Timings.SOG_OPTIMISATION
    val optimiser =
    //OptimalBranchAndBoundOptimiser(minimiseChildrenWithAbstractTasks, lowerBound = lb) //, minimiseAbstractTaskOccurencesMetric)
      GreedyNumberOfAbstractChildrenOptimiser

    val g = optimiser.minimalSOG(methodTaskGraphs)
    timeCapsule stop Timings.SOG_OPTIMISATION
    //val met = minimiseChildrenWithAbstractTasks(g._1,g._2)

    //val check = OptimalBranchAndBoundOptimiser(minimiseChildrenWithAbstractTasks, lowerBound = lb).minimalSOG(methodTaskGraphs)
    //val metOp = minimiseChildrenWithAbstractTasks(check._1,check._2)

    //println("done")
    //println("OP " + met + " of " + metOp)
    val minimalSuperGraph = g._1
    val planStepToIndexMappings: Seq[Map[PlanStep, Int]] = g._2
    // TODO: hack!
    possibleMethods zip planStepToIndexMappings foreach { case (m, map) => m.subPlan.planStepsWithoutInitGoal foreach { ps => if (!ps.schema.effect.isEmpty) assert(map contains ps) } }

    val (methodMappings, primitiveMappings) = planStepToIndexMappings.splitAt(possibleMethods.length)

    val childrenIndicesToPossibleTasks = minimalSuperGraph.vertices map { _ => new mutable.HashSet[Task]() }

    val tasksPerMethodToChildrenMapping: Array[Array[Int]] = methodMappings.zipWithIndex map { case (mapping, methodIndex) =>
      val methodPlanSteps = possibleMethods(methodIndex).subPlan.planStepsWithoutInitGoal
      (methodPlanSteps collect {
        case ps if !(omitMethodPreconditionActions && possibleMethods(methodIndex).subPlan.planStepSchemaArrayWithoutMethodPreconditions.nonEmpty) ||
          ps.schema.isAbstract || !ps.schema.effect.isEmpty ||
          !possibleMethods(methodIndex).subPlan.orderingConstraints.fullGraph.sources.contains(ps) || !ps.schema.name.contains(SHOPMethodCompiler.SHOP_METHOD_PRECONDITION_PREFIX) =>
          // TODO: hack
          childrenIndicesToPossibleTasks(mapping(ps)) add ps.schema
          mapping(ps)
      }).toArray
    } toArray

    val childrenForPrimitives = primitiveMappings.zipWithIndex map { case (mapping, primitiveIndex) =>
      assert(mapping.size == 1)
      childrenIndicesToPossibleTasks(mapping.head._2) add mapping.head._1.schema
      mapping.head._2
    } toArray

    //println("\n\nGraph minimisation")
    //println(childrenIndicesToPossibleTasks map {s => s map {t => t.name + " " + t.isAbstract} mkString " "} mkString "\n")

    val maxVertex = if (minimalSuperGraph.vertices.isEmpty) -1 else minimalSuperGraph.vertices.max
    assert(minimalSuperGraph.vertices.length - 1 == maxVertex, "SOG has " + minimalSuperGraph.vertices.length + " vertices, but maximum vertex is " + maxVertex)

    val childrenTasks: Array[Set[Task]] = childrenIndicesToPossibleTasks map { _.toSet } toArray

    //println("SOG " + childrenTasks.length + " " + minimalSuperGraph.vertices.size + " " + possibleMethods.size + " " + possiblePrimitives.size)
    //println(childrenTasks.map(_.map(_.name).mkString(", ")).mkString("\n"))

    //tasksPerMethodToChildrenMapping zip

    (tasksPerMethodToChildrenMapping, childrenForPrimitives, childrenTasks, NonExpandedSOG(minimalSuperGraph))
  }

  protected def combinePayloads(childrenPayload: Seq[SOG], intermediate: NonExpandedSOG): SOG = {
    assert(childrenPayload.size == intermediate.ordering.vertices.size)
    val vertices = childrenPayload flatMap { _.ordering.vertices }
    val internalEdges = childrenPayload flatMap { _.ordering.edgeList }

    val connectingEdges = intermediate.ordering.edgeList flatMap { case (from, to) =>
      val fromVertices = childrenPayload(from).ordering.vertices
      val toVertices = childrenPayload(to).ordering.vertices

      for (x <- fromVertices; y <- toVertices) yield (x, y)
    }

    SOG(SimpleDirectedGraph(vertices, internalEdges ++ connectingEdges))
  }

  override protected def minimisePathDecompositionTree(pdt: PathDecompositionTree[SOG]): PathDecompositionTree[SOG] = {
    val dontRemovePrimitives: Seq[Set[Task]] = pdt.primitivePaths.toSeq map { _ => Set[Task]() }

    pdt.restrictPathDecompositionTree(dontRemovePrimitives)
  }

  lazy val sog: DirectedGraph[(Seq[Int], Set[Task])] = {
    println("Final SOG has " + rootPayload.ordering.vertices.length + " vertices")
    // assert correctness

    val pl: DirectedGraph[(Seq[Int], Set[Task])] = rootPayload.ordering.map(
      {
        case (path, tasks) =>
          val matchingPaths = primitivePaths.filter(_._1 == path)
          val x = if (matchingPaths.isEmpty)
            (path, Set[Task]())
          else
            (path, matchingPaths.head._2)

          x
      }) filter { _._2.nonEmpty }

    print("Compute Transitive reduction ... ")
    //val sog = rootPayload.ordering.transitiveReduction
    val ss = pl.transitiveClosure
    val nodesToKeep = ss.vertices filter { _._2.exists(_.isPrimitive) } toSet
    val finalSOG: DirectedGraph[(Seq[Int], Set[Task])] =
      SimpleDirectedGraph(nodesToKeep.toSeq, ss.edgeList filter { case (a, b) => (nodesToKeep contains a) && (nodesToKeep contains b) }).transitiveReduction
    println("done")


    /*val string = finalSOG.dotString(options = DirectedGraphDotOptions(),
                                    //nodeRenderer = {case (path, tasks) => tasks map { _.name } mkString ","})
                                    nodeRenderer = {case (path, tasks) => tasks.count(_.isPrimitive) + " " + tasks.size + " " + path})
    Dot2PdfCompiler.writeDotToFile(string, "sog.pdf")*/

    println("TREE P: " + primitivePaths.length + " S: " + taskSequenceLength)


    assert(primitivePaths forall { p => finalSOG.vertices contains p })

    finalSOG
  }

}

case class NonExpandedSOG(ordering: DirectedGraph[Int])

case class SOG(ordering: DirectedGraph[(Seq[Int], Set[Task])]) {}
