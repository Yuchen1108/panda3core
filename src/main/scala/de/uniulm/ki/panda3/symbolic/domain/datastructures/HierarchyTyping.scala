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

package de.uniulm.ki.panda3.symbolic.domain.datastructures

import de.uniulm.ki.panda3.symbolic.csp._
import de.uniulm.ki.panda3.symbolic.domain._
import de.uniulm.ki.panda3.symbolic.domain.datastructures.hierarchicalreachability.WithTopMethod
import de.uniulm.ki.panda3.symbolic.logic.{Constant, Sort, Variable}
import de.uniulm.ki.panda3.symbolic.{NONSIMPLEMETHOD, PrettyPrintable, noSupport}
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.plan.element.{GroundTask, PlanStep}
import de.uniulm.ki.panda3.symbolic.plan.modification.InsertPlanStepWithLink

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
case class HierarchyTyping(domain: Domain, initialPlan: Plan) extends WithHierarchyTyping {
  val omitTopDownStep: Boolean = false
}

trait WithHierarchyTyping extends WithTopMethod {

  def initialPlan: Plan

  def domain: Domain

  def omitTopDownStep: Boolean


  // propagates possible parameter values in a top down fashion

  lazy val cartTasksMap         = new mutable.HashMap[Task, Set[CartesianGroundTask]]().withDefaultValue(Set())
  lazy val cartMethodsMap       = new mutable.HashMap[CartesianGroundTask, Set[CartesianGroundMethod]]().withDefaultValue(Set())
  lazy val cartTaskInMethodsMap = new mutable.HashMap[CartesianGroundTask, Set[CartesianGroundMethod]]().withDefaultValue(Set())

  def dfs(currentGroundTask: CartesianGroundTask): Unit = if (!(cartTasksMap(currentGroundTask.task) contains currentGroundTask)) {
    // add the ground task to its map
    cartTasksMap(currentGroundTask.task) = cartTasksMap(currentGroundTask.task) + currentGroundTask
    //println("TASK " + currentGroundTask.task.name)
    // if the task is abstract, we have to ground it
    // we have a partial variable binding from the abstract task
    val possibleMethods: Seq[(SimpleDecompositionMethod, CartesianGroundMethod)] =
      (domain.methodsForAbstractTasks.getOrElse(currentGroundTask.task, Nil) ++ (if (topMethod.abstractTask == currentGroundTask.task) topMethod :: Nil else Nil)) map {
        case simpleMethod: SimpleDecompositionMethod =>
          //println("Method " + simpleMethod.name)
          val setParameter: mutable.Map[Variable, Set[Constant]] = new mutable.HashMap()
          // fill map
          simpleMethod.subPlan.variableConstraints.variables foreach {
            v =>
              val cspPossibleValues: Seq[Constant] = simpleMethod.subPlan.variableConstraints.reducedDomainOf(v)
              val reducedPossibleValues: Seq[Constant] = if (currentGroundTask.argumentMap contains v) cspPossibleValues intersect currentGroundTask.argumentMap(v) else cspPossibleValues

              setParameter(v) = reducedPossibleValues.toSet
          }
          // propagate equality
          var changed = true
          while (changed)
            changed = simpleMethod.subPlan.variableConstraints.constraints collect {
              case Equal(var1: Variable, var2: Variable)                                      =>
                val newDomain = setParameter(var1) intersect setParameter(var2)
                val innerChanged = newDomain != setParameter(var1) || newDomain != setParameter(var2)

                setParameter(var1) = newDomain
                setParameter(var2) = newDomain

                innerChanged
              case NotEqual(var1: Variable, var2: Variable) if setParameter(var1).size == 1 =>
                val innerChanged = setParameter(var2) contains setParameter(var1).head
                setParameter(var2) = setParameter(var2) - setParameter(var1).head
                innerChanged
              case NotEqual(var1: Variable, var2: Variable) if setParameter(var2).size == 1 =>
                val innerChanged = setParameter(var1) contains setParameter(var2).head
                setParameter(var1) = setParameter(var1) - setParameter(var2).head
                innerChanged
            } exists { x => x }

          if (setParameter exists { _._2.isEmpty }) None else Some((simpleMethod, CartesianGroundMethod(simpleMethod, setParameter.toMap)))
        case _                                       => noSupport(NONSIMPLEMETHOD)
      } collect { case Some(x) => x }

    // add the new methods to the map
    val flattenedPossibleMethods = possibleMethods map { _._2 }
    cartMethodsMap(currentGroundTask) = cartMethodsMap(currentGroundTask) ++ flattenedPossibleMethods
    flattenedPossibleMethods foreach {
      cartMethod =>
        cartMethod.subTasks foreach {
          subTask =>
            cartTaskInMethodsMap(subTask) = cartTaskInMethodsMap(subTask) + cartMethod
          //println(cartTaskInMethodsMap(subTask).size)
        }
    }


    // perform recursion
    flattenedPossibleMethods flatMap { _.subTasks } foreach dfs
  }

  // start the cartesian process from the artificial grounding top task
  lazy val cartesianTop = CartesianGroundTask(groundedTopTask.task.asInstanceOf[ReducedTask], groundedTopTask.arguments map { x => Set(x) })

  def initialise(): Unit = if (!omitTopDownStep) {
    dfs(cartesianTop)
    /*println("Hierarchy Typing")

    cartTasksMap foreach { case (t, cts) =>
      println("Task " + t.name)
      cts foreach { ct =>
        println("\tCT")
        ct.argumentMap foreach { case (v, cs) => println("\t\t" + v.name + ": " + cs.map(_.name).mkString(" ")) }
        println()
      }
    }*/

  } else {
    // don't use top-down analysis, just fill the carthesian maps naively
    cartTasksMap(cartesianTop.task) = Set(cartesianTop)
    //cartMethodsMap(cartesianTop) = Set(CartesianGroundMethod(topMethod, topMethod.subPlan.variableConstraints.variables map { v => v -> v.sort.elements.toSet } toMap))

    domain.tasks foreach { case t => cartTasksMap(t) = Set(CartesianGroundTask(t, t.parameters map { _.sort.elements.toSet })) }
    //(domain.decompositionMethods) foreach { case m =>
    (domain.decompositionMethods :+ topMethod) foreach { case m =>
      val cartTask = cartTasksMap(m.abstractTask).head
      val cartMethod = CartesianGroundMethod(m, m.subPlan.variableConstraints.variables map { v =>
        m.subPlan.variableConstraints.constraints find { case Equal(`v`, c: Constant) => true; case _ => false } match {
          case Some(Equal(_, c: Constant)) => v -> Set(c)
          case None                        => v -> m.subPlan.variableConstraints.reducedDomainOf(v).toSet.filterNot(c => m.subPlan.variableConstraints.constraints.contains(NotEqual(v, c)))
        }
        /*v.sort.elements.toSet*/
      } toMap, omitTopDownStep = true)
      cartMethodsMap(cartTask) = cartMethodsMap(cartTask) + cartMethod

      m.subPlan.planStepsWithoutInitGoal foreach { st =>
        val subTask = cartTasksMap(st.schema).head
        cartTaskInMethodsMap(subTask) = cartTaskInMethodsMap(subTask) + cartMethod

      }
    }
  }


  /*case class PossibleArgumentPair(var1: Variable, const1: Constant, var2: Variable, const2: Constant) {
    assert(var1.id < var2.id)
  }

  val allowedParameters           : mutable.Map[Task, Map[Variable, Set[Constant]]] = new mutable.HashMap()
  val allowedParameterCombinations: mutable.Map[Task, Set[PossibleArgumentPair]]    = new mutable.HashMap()


  def propagateMethod(method: DecompositionMethod): Unit = {
    println("Method " + method.name)
    val initialVariableMap = allowedParameters(method.abstractTask)
    val allowedCombinations = allowedParameterCombinations(method.abstractTask)

    // we treat each task in isolation
    val tasksToPropagate = method.subPlan.planStepsWithoutInitGoal flatMap { case PlanStep(_, task, arguments) =>
      println("\tPlanStep " + task.name + " of " + method.name)
      // project arguments back onto the initial variables (for every argument the set of parameters it is *equal* to)
      val mappedToOriginal: Map[Variable, (Variable, Seq[Variable])] = // parameter of the task -> var in planCSP, identical vars of abstract task
        arguments.zip(task.parameters) map { case (v, p) => p -> (v, initialVariableMap.keys.toSeq filter { method.subPlan.parameterVariableConstraints.equal(v, _) }) } toMap
      // determine possible values for each variable
      val possibleValues: Map[Variable, Seq[Constant]] = mappedToOriginal map {
        case (p, (v, Nil))    => p -> method.subPlan.variableConstraints.reducedDomainOf(v)
        case (p, (v, topVar)) => p -> topVar.foldLeft(method.subPlan.variableConstraints.reducedDomainOf(v))({ case (consts, nextVar) => consts intersect initialVariableMap(nextVar).toSeq })
      }

      val possiblePairs: Seq[PossibleArgumentPair] = arguments.zip(task.parameters) flatMap { case (v1, p1) =>
        arguments.zip(task.parameters) flatMap {
          case (_, p2) if p1.id >= p2.id => Nil
          case (v2, p2)                  =>
            possibleValues(p1) flatMap { c1 =>
              possibleValues(p2) flatMap {
                case c2 =>
                  // for the assignment (p1->c1,p2->c2) to be valid, multiple conditions have to hold
                  val methodLocallyConsistent = method.subPlan.variableConstraints.isLocallyConsistent(Map(v1 -> c1, v2 -> c2))
                  val possibleFromAbstract =
                    mappedToOriginal(p1)._2 forall { a1 =>
                      mappedToOriginal(p2)._2 forall { a2 =>
                        if (a1.id < a2.id) allowedCombinations.contains(PossibleArgumentPair(a1, c1, a2, c2))
                        else if (a1.id > a2.id) allowedCombinations.contains(PossibleArgumentPair(a2, c1, a1, c2))
                        else true // equal vars
                      }
                    }


                  if (methodLocallyConsistent && possibleFromAbstract) PossibleArgumentPair(p1, c1, p2, c2) :: Nil else Nil
              }
            }
        }
      }

      // filter new ones
      val newlyPossibleValues = possibleValues map { case (v, cs) => v -> (cs filterNot allowedParameters.getOrElse(task, Map()).getOrElse(v, Set()).contains) }
      val newlyPossiblePairs = possiblePairs filterNot allowedParameterCombinations.getOrElse(task, Set()).contains
      // write back

      val newValues =
        newlyPossibleValues.keys.foldLeft(allowedParameters.getOrElse(task, Map()))({ case (curMap, v) => curMap + (v -> (curMap.getOrElse(v, Set()) ++ newlyPossibleValues(v))) })

      allowedParameters(task) = newValues
      allowedParameterCombinations(task) = allowedParameterCombinations.getOrElse(task, Set()) ++ newlyPossiblePairs

      // if new things occurred, recurse
      if (newlyPossibleValues.nonEmpty || newlyPossiblePairs.nonEmpty) task :: Nil else Nil
    }

    tasksToPropagate.distinct foreach treatTask

  }

  def treatTask(task: Task): Unit = {
    print("Run task " + task.name + " @ ")
    println(allowedParameterCombinations(task).size)
    if (task.isAbstract) domain.methodsForAbstractTasks(task) foreach propagateMethod
  }

  // initialise
  initialPlan.planStepsWithoutInitGoal foreach { case PlanStep(_, task, arguments) =>
    val currentAllowed = allowedParameters.getOrElse(task, Map())
    // new single alloweds
    val mySingleMap = arguments.zip(task.parameters) map { case (v, p) => p -> initialPlan.variableConstraints.reducedDomainOf(v) } toMap
    // new pairs
    val myPairs: Seq[PossibleArgumentPair] = arguments.zip(task.parameters) flatMap { case (v1, p1) =>
      arguments.zip(task.parameters) flatMap {
        case (_, p2) if p1.id >= p2.id => Nil
        case (v2, p2)                  =>
          mySingleMap(p1) flatMap { c1 =>
            mySingleMap(p2) collect {
              case c2 if initialPlan.variableConstraints.isLocallyConsistent(Map(v1 -> c1, v2 -> c2)) => PossibleArgumentPair(p1, c1, p2, c2)
            }
          }
      }
    }


    val newSingleMap = task.parameters.foldLeft(currentAllowed)({ case (curMap, v) => curMap + (v -> (curMap.getOrElse(v, Set()) ++ mySingleMap(v))) })

    allowedParameters(task) = newSingleMap
    allowedParameterCombinations(task) = allowedParameterCombinations.getOrElse(task, Set()) ++ myPairs
  }

  // run typing
  initialPlan.planStepsWithoutInitAndGoalTasksSet foreach treatTask
  println("done")

*/
  def checkPartialAssignment(task: Task, assignment: Map[Variable, Constant]): Boolean =
    if (initialPlan.isModificationAllowed(InsertPlanStepWithLink(null, null, null, null)) && task.isPrimitive) true
    else cartTasksMap(task) exists { ct => ct.isCompatible(assignment) }


  def checkGrounding(groundAction: GroundTask): Boolean =
    if (initialPlan.isModificationAllowed(InsertPlanStepWithLink(null, null, null, null)) && groundAction.task.isPrimitive) true else {
      cartTasksMap(groundAction.task) exists { ct => ct.isCompatible(groundAction) }
      /*(groundAction.arguments.zip(groundAction.task.parameters) forall { case (c1, v1) =>
        groundAction.arguments.zip(groundAction.task.parameters) forall { case (c2, v2) =>
          v1.id >= v2.id || allowedParameterCombinations(groundAction.task).contains(PossibleArgumentPair(v1, c1, v2, c2))
        }
      })*/
    }
}

// scalastyle:off

case class CartesianGroundMethod(method: DecompositionMethod, parameter: Map[Variable, Set[Constant]], omitTopDownStep: Boolean = false) {
  lazy val subTasks  : Seq[CartesianGroundTask]           = subTaskMap.values.toSeq
  lazy val subTaskMap: Map[PlanStep, CartesianGroundTask] = method.subPlan.planStepsWithoutInitGoal map { case ps@PlanStep(_, schema: ReducedTask, arguments) =>
    ps -> CartesianGroundTask(schema,
                              if (!omitTopDownStep) arguments map parameter else ps.schema.parameters map { _.sort.elements.toSet })
  } toMap

  lazy val subCartesianToPlanSteps: Map[CartesianGroundTask, Seq[PlanStep]] = subTaskMap.toSeq groupBy { _._2 } map { case (cart, seq) => cart -> seq.map(_._1) }

  lazy val abstractTask: CartesianGroundTask = CartesianGroundTask(method.abstractTask, method.abstractTask.parameters map parameter)

  private val constraintsPerVariable: Map[Variable, Array[VariableConstraint]] = method.subPlan.variableConstraints.variables.toSeq map { v =>
    v -> (method.subPlan.variableConstraints.constraints filter { _ containsVariable v } toArray)
  } toMap


  // : Seq[(Variable, Seq[Constant])]
  private val (nonBindableVariables, boundByEquality) = {
    val planUF = SymbolicUnionFind.constructVariableUnionFind(method.subPlan)
    val planArguments = method.subPlan.planStepsWithoutInitGoal flatMap { _.arguments } toSet

    val nonBound = parameter.keySet -- planArguments
    val potentialEquality = nonBound map { v => (v, planArguments.find(arg => planUF(v) == planUF(arg))) }

    val (bindByEquality, nonBindable) = potentialEquality partition { case (_, Some(_)) => true; case _ => false }

    (nonBindable map { case (v, _) => (v, parameter(v).toSeq) } toSeq, bindByEquality map { case (v1, Some(v2)) => (v1, v2) })
  }

  def groundWithPossibleTasks(possibleTasks: mutable.Map[CartesianGroundTask, Set[GroundTask]], causingTask: Option[Seq[GroundTask]]): Seq[GroundedDecompositionMethod] =
    causingTask match {
      case None        => groundWithPossibleTasks(possibleTasks, None, None)
      case Some(tasks) =>
        val matchingPlanSteps = method.subPlan.planStepsWithoutInitGoal.filter(_.schema == tasks.head.task)
        matchingPlanSteps flatMap { ps => groundWithPossibleTasks(possibleTasks, Some(tasks.toSet), Some(ps)) } distinct
    }

  def planStepDifficulty(possibleTasks: mutable.Map[CartesianGroundTask, Set[GroundTask]], ps: PlanStep): Int =
  //possibleTasks(subTaskMap(ps)).size
    -ps.arguments.length

  def groundWithPossibleTasks(possibleTasks: mutable.Map[CartesianGroundTask, Set[GroundTask]],
                              causingTask: Option[Set[GroundTask]], planStep: Option[PlanStep]): Seq[GroundedDecompositionMethod] = {
    // treat plansteps in increasing order of groundings
    val planStepsSortedByDifficulty = planStep match {
      case None     => method.subPlan.planStepsWithoutInitGoal sortBy { ps => planStepDifficulty(possibleTasks, ps) }
      case Some(ps) =>
        (ps :: Nil) ++ (method.subPlan.planStepsWithoutInitGoal.filter(_ != ps) sortBy { ps => planStepDifficulty(possibleTasks, ps) })
    }
    //println(planStepsSortedByDifficulty map { _.schema.name } mkString " ")

    val time000 = System.currentTimeMillis()
    val (_, possibleTasksMapsSeq) = planStepsSortedByDifficulty.foldLeft[(Set[Variable], Seq[(PlanStep, Seq[Variable], Array[Int], Map[Seq[Constant], Array[GroundTask]])])]((Set(), Nil))(
      { case ((boundVariables, mapsSoFar), nextPlanStep) =>
        val (commonVariables, newVariables) = nextPlanStep.arguments.zipWithIndex partition { case (v, _) => boundVariables contains v }
        val commonVariablesIndex = commonVariables map { _._2 }
        val groundingsPerInstantiation: Map[Seq[Constant], Array[GroundTask]] =
          possibleTasks(subTaskMap(nextPlanStep)) groupBy { groundTask => commonVariablesIndex map groundTask.argumentArray } map { case (a, b) => (a, b.toArray)
          } withDefaultValue new Array[GroundTask](0)

        val nextEntry = (nextPlanStep, commonVariables map { _._1 }, newVariables map { _._2 } toArray, groundingsPerInstantiation)

        (boundVariables ++ nextPlanStep.arguments, mapsSoFar :+ nextEntry)
      })

    val possibleTasksMaps: Array[(PlanStep, Seq[Variable], Array[Int], Map[Seq[Constant], Array[GroundTask]])] = possibleTasksMapsSeq.toArray
    val time001 = System.currentTimeMillis()

    val groundigs = new ArrayBuffer[GroundedDecompositionMethod]()

    val variableBinding: mutable.Map[Variable, Constant] = new mutable.HashMap[Variable, Constant]()
    val planStepBinding: mutable.Map[PlanStep, GroundTask] = new mutable.HashMap[PlanStep, GroundTask]()

    // recursively match the tasks
    def matchRecursively(position: Int): Unit = {
      if (position == possibleTasksMaps.length) {
        val equalityBindable = variableBinding.toMap ++ (boundByEquality map { case (v, eqV) => (v, variableBinding(eqV)) })

        // instantiate all variables that do not occur in the methods subplan (usually some rough arguments of the abstract task)
        Sort.allPossibleInstantiationsWithVariables(nonBindableVariables) map { b => (b, equalityBindable ++ b) } foreach {
          case (binding, fullBinding) if !(binding exists { case (nv, c) => if (equalityBindable.contains(nv)) c != equalityBindable(nv) else false }) &&
            method.areParametersAllowed(fullBinding) =>
            val groundMethod = GroundedDecompositionMethod(method, fullBinding, Some(planStepBinding.toMap))
            if (groundMethod.isCorrentlyInheriting) groundigs.append(groundMethod)
          case _                                     =>
        }
      } else {
        val (nextPlanStep, commonVariables, newVariableIndices, groundAccessMap) = possibleTasksMaps(position)
        val commonVariablesValues = commonVariables map variableBinding

        val allPossibleGroundings: Array[GroundTask] = groundAccessMap(commonVariablesValues)
        val possibleGroundings: Array[GroundTask] = if (position == 0 && causingTask.isDefined) allPossibleGroundings.filter(causingTask.get.contains) else allPossibleGroundings

        var i = 0
        while (i < possibleGroundings.length) {
          val groundTask = possibleGroundings(i)
          planStepBinding(nextPlanStep) = groundTask

          var groundingConsistent = true
          var j = 0
          while (j < newVariableIndices.length && groundingConsistent) {
            val newVariable = nextPlanStep.arguments(newVariableIndices(j))
            val newValue = groundTask.argumentArray(newVariableIndices(j))
            variableBinding(newVariable) = newValue
            // check variableConstraints
            val constraintsOfSetVariable = constraintsPerVariable(newVariable)
            var c = 0
            while (c < constraintsOfSetVariable.length && groundingConsistent) {
              groundingConsistent = constraintsOfSetVariable(c) match {
                case Equal(var1, var2: Variable)     => if (variableBinding.contains(var1) && variableBinding.contains(var2)) variableBinding(var1) == variableBinding(var2) else true
                case Equal(vari, const: Constant)    => variableBinding(vari) == const
                case NotEqual(var1, var2: Variable)  => if (variableBinding.contains(var1) && variableBinding.contains(var2)) variableBinding(var1) != variableBinding(var2) else true
                case NotEqual(vari, const: Constant) => variableBinding(vari) != const
                case OfSort(vari, sort)              => sort.elements contains variableBinding(vari)
                case NotOfSort(vari, sort)           => !(sort.elements contains variableBinding(vari))
              }
              c += 1
            }
            j += 1
          }

          // inefficient, actually we only have to check the constraints pertaining to newly added variables
          if (groundingConsistent) matchRecursively(position + 1)

          // clear variables again
          j = 0
          while (j < newVariableIndices.length) {
            val newVariable = nextPlanStep.arguments(newVariableIndices(j))
            variableBinding.remove(newVariable)
            j += 1
          }

          i += 1
        }
      }
    }

    matchRecursively(0)
    val time002 = System.currentTimeMillis()
    //println("Matching: " + (time001 - time000) + " + " + (time002 - time001) + " = " + (time002 - time000))

    groundigs
  }

  override def equals(o: scala.Any): Boolean =
    if (o.isInstanceOf[CartesianGroundMethod] && this.hashCode == o.hashCode()) {productIterator.sameElements(o.asInstanceOf[CartesianGroundMethod].productIterator) } else false

  override val hashCode: Int = method.hashCode + parameter.toSeq.sortBy({ case (v, c) => v.hashCode }).foldLeft(0)(
    { case (h, (v, cs)) => ((h + cs.map(_.hashCode).sum) * 13 + v.hashCode) * 13 })
}

case class CartesianGroundTask(task: Task, parameter: Seq[Set[Constant]]) extends PrettyPrintable {
  val argumentMap: Map[Variable, Seq[Constant]] = task.parameters zip (parameter map { _.toSeq }) toMap

  override def shortInfo: String = task.name + (parameter.zip(task.parameters) map { case (l, p) => p.name + ":" + l.size }).mkString("(", "; ", ")")

  override def mediumInfo: String = task.name + (parameter map { l => l.map(_.name).mkString("(", ",", ")") }).mkString(";")

  override def longInfo: String = mediumInfo

  lazy val parameterArray: Array[Set[Constant]] = parameter.toArray

  def isCompatible(groundTask: GroundTask): Boolean = {
    var compatible = true
    var i = 0
    while (i < parameterArray.length && compatible) {
      compatible = parameterArray(i) contains groundTask.argumentArray(i)
      i += 1
    }
    compatible
  }

  def isCompatible(partialAssignment: Map[Variable, Constant]): Boolean =
    partialAssignment forall { case (v, c) =>
      val vInd = task.parameters.indexOf(v)
      parameter(vInd) contains c
    }
}