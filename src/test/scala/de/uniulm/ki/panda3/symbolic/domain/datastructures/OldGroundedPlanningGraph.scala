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

package de.uniulm.ki.panda3.symbolic.domain.datastructures.primitivereachability

import de.uniulm.ki.panda3.symbolic.csp._
import de.uniulm.ki.panda3.symbolic.domain.datastructures.LayeredGroundedPrimitiveReachabilityAnalysis
import de.uniulm.ki.panda3.symbolic.domain.{ConstantActionCost, Domain, ReducedTask, Task}
import de.uniulm.ki.panda3.symbolic.logic._
import de.uniulm.ki.panda3.symbolic.plan.element.GroundTask

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
case class OldGroundedPlanningGraph(domain: Domain, initialState: Set[GroundLiteral], computeMutexes: Boolean, isSerial: Boolean,
                                    disallowedTasks: Either[Seq[GroundTask], Seq[Task]] = Left(Nil)) extends LayeredGroundedPrimitiveReachabilityAnalysis {

  initialState foreach { f => assert(f.isPositive) }

  lazy          val graphSize: Int                                        = layerWithMutexes.size
  // This function should compute the actual planning graph
  override lazy val layer    : Seq[(Set[GroundTask], Set[GroundLiteral])] = {
    val computedLayer = layerWithMutexes map { case (groundTasks, groundTaskMutexes, groundLiterals, groundLiteralMutexes) =>
      (groundTasks filterNot {
        _.task.name.startsWith("NO-OP")
      }, groundLiterals)
    }
    // check assertions only when computing the layers
    computedLayer foreach { case (_, b) => b foreach { gl => assert(gl.isPositive) } }
    computedLayer foreach { case (_, b) => assert(initialState forall b.contains) }

    computedLayer
  }


  lazy val layerWithMutexes: Seq[(Set[GroundTask], Set[(GroundTask, GroundTask)], Set[GroundLiteral], Set[(GroundLiteral, GroundLiteral)])] =
    buildGraph(Seq.empty[(Set[GroundTask], Set[(GroundTask, GroundTask)], Set[GroundLiteral], Set[(GroundLiteral, GroundLiteral)])],
               initialState, Set.empty[(GroundLiteral, GroundLiteral)], firstLayer = true, Map())

  def buildGraph(graph: Seq[(Set[GroundTask], Set[(GroundTask, GroundTask)], Set[GroundLiteral], Set[(GroundLiteral, GroundLiteral)])],
                 addedPropositions: Set[GroundLiteral], deletedMutexes: Set[(GroundLiteral, GroundLiteral)], firstLayer: Boolean, oldPreconMap: Map[Predicate, Set[GroundLiteral]]):
  Seq[(Set[GroundTask], Set[(GroundTask, GroundTask)], Set[GroundLiteral], Set[(GroundLiteral, GroundLiteral)])] = {

    val previousLayer = if (firstLayer) {
      (Set.empty[GroundTask], Set.empty[(GroundTask, GroundTask)], initialState, Set.empty[(GroundLiteral, GroundLiteral)])
    } else {
      graph.last
    }
    val updatedPrecondMap = fillPreconMap(oldPreconMap, addedPropositions)
    /*println("addedPropositions:")
    for (x <- addedPropositions) {
      println("gl: " + x.predicate.name + ", parameter: " + x.parameter.toString())
    }
    println("deletedMutexes:")
    for (x <- deletedMutexes) {
      println("gl: " + x._1.predicate.name + ", parameter: " + x._1.parameter.toString() + "||" + "gl: " + x._2.predicate.name + ", parameter: " + x._2.parameter.toString())
    }*/

    // determine which grounded literals in the last state layer may cause new grounded actions to be applicable
    //TODO: tasks that will be considered because of deleted mutexes should contain both grounded literals as preconditions, otherwise it can happen that the same action gets initialised
    // twice.
    val changedPropositions: Set[GroundLiteral] = addedPropositions ++ (deletedMutexes flatMap { mutex => Set(mutex._1, mutex._2) })
    val tasksToBeConsidered: Set[(GroundLiteral, Seq[ReducedTask])] = changedPropositions map { groundLiteral => (groundLiteral, domain.consumersOf(groundLiteral.predicate) filter {
      _
        .isPrimitive
    })
    }


    // create the newly applicable grounded actions
    val newGroundTasksFromPreconditions: Set[GroundTask] = tasksToBeConsidered flatMap { case (groundLiteral, tasks) => tasks flatMap { task => disallowedTasks match {
      case Right(forbiddenLiftedTasks) if forbiddenLiftedTasks contains task => Set.empty[GroundTask]
      case _                                                                 =>
        task.precondition.conjuncts filter { literal => literal.predicate == groundLiteral.predicate } flatMap { startLiteral =>
          createActionInstances(task, groundLiteral, startLiteral, task.precondition.conjuncts, previousLayer._4, updatedPrecondMap)
        }
    }
    }
    }
    // special treatment for tasks without preconditions. the are always applicable in the first action layer
    val newGroundTasksFromParameters: Set[GroundTask] = firstLayer match {
      case true  => createActionInstancesForTasksWithoutPreconditions(domain.primitiveTasks collect {
        case t: ReducedTask => t
      } filter { reducedTask => reducedTask.precondition.conjuncts.isEmpty })
      case false => Set.empty[GroundTask]
    }
    // round up
    val newInstantiatedGroundTasks: Set[GroundTask] = newGroundTasksFromPreconditions ++ newGroundTasksFromParameters
    val newNoOps: Set[GroundTask] = addedPropositions map { groundLiteral => createNOOP(groundLiteral) }
    val newTasks: Set[GroundTask] = (newInstantiatedGroundTasks ++ newNoOps) -- previousLayer._1
    val allGroundTasks: Set[GroundTask] = previousLayer._1 ++ newTasks
    //println("DUP: " + previousLayer._1.size + " + " + newTasks.size + " = " + (previousLayer._1.size + newTasks.size) + " vs " + allGroundTasks.size)
    /*println("previous")
    for (x <- previousLayer._1) {
      println("Task: " + x.task.name + ",Parameter: " + x.arguments.toString)
    }
    println("new")
    for (x <- newTasks) {
      println("Task: " + x.task.name + ",Parameter: " + x.arguments.toString)
    }*/

    // compute task mutexes anew
    // TODO here we could to things a lot more efficiently
    //println("TASKS " + allGroundTasks.size)
    val orderedNewTasks = newTasks.toVector.sorted
    val groundTaskPairs: Set[(GroundTask, GroundTask)] = (for (x <- 0 until orderedNewTasks.size - 1; y <- x until orderedNewTasks.size) yield
      (orderedNewTasks(x), orderedNewTasks(y))) ++
      (for (x <- newTasks; y <- previousLayer._1) yield if ((x compare y) < 0) (x, y) else (y, x)) filter { case (a, b) => a != b } toSet

    val newTaskMutexes: Set[(GroundTask, GroundTask)] = {
      if (computeMutexes) {
        val normalMutexes = groundTaskPairs filter { case (groundTask1, groundTask2) =>
          (groundTask1.substitutedDelEffects exists { substitutedEffect =>
            (groundTask2.substitutedAddEffects ++ groundTask2.substitutedPreconditions) exists {
              _.=!=(substitutedEffect)
            }
          }) ||
            (groundTask2.substitutedDelEffects exists { substitutedEffect =>
              (groundTask1.substitutedAddEffects ++ groundTask1.substitutedPreconditions) exists {
                _.=!=(substitutedEffect)
              }
            }) ||
            (for (x <- groundTask1.substitutedPreconditions; y <- groundTask2.substitutedPreconditions) yield if ((x compare y) < 0) (x, y) else (y, x)).exists(previousLayer._4.contains)
        }
        if (isSerial) {
          (groundTaskPairs filterNot { case (groundTask1, groundTask2) => groundTask1.task.name.startsWith("NO-OP") || groundTask2.task.name.startsWith("NO-OP") }) union normalMutexes
        } else {
          normalMutexes
        }
      } else {
        Set.empty[(GroundTask, GroundTask)]
      }
    }

    val remainingOldMutexes: Set[(GroundTask, GroundTask)] = {
      if (computeMutexes) {
        previousLayer._2 filter { case (groundTask1, groundTask2) =>
          (groundTask1.substitutedDelEffects exists { substitutedEffect =>
            (groundTask2.substitutedAddEffects ++ groundTask2.substitutedPreconditions) exists {
              _.=!=(substitutedEffect)
            }
          }) ||
            (groundTask2.substitutedDelEffects exists { substitutedEffect =>
              (groundTask1.substitutedAddEffects ++ groundTask1.substitutedPreconditions) exists {
                _.=!=(substitutedEffect)
              }
            }) ||
            (for (x <- groundTask1.substitutedPreconditions; y <- groundTask2.substitutedPreconditions) yield if ((x compare y) < 0) (x, y) else (y, x)).exists(previousLayer._4.contains)
        }
      } else {
        Set.empty[(GroundTask, GroundTask)]
      }
    }
    val allTaskMutexes = newTaskMutexes ++ remainingOldMutexes
    //println("TASK MUTEXES: " + allTaskMutexes.size)

    // determine new propositions
    val newPropositions: Set[GroundLiteral] = (newInstantiatedGroundTasks flatMap { newGroundTask => newGroundTask.substitutedAddEffects }) -- previousLayer._3
    val allPropositions: Set[GroundLiteral] = newPropositions ++ previousLayer._3
    //println("PROPOS " + allPropositions.size)

    // compute proposition mutexes anew
    // TODO here we could to things a lot more efficiently
    val propositionProducers: Map[GroundLiteral, Set[GroundTask]] = (allPropositions map { groundLiteral => (groundLiteral, allGroundTasks filter {
      groundTask => groundTask.substitutedAddEffects contains groundLiteral
    })
    }).toMap
    val sortedPropositions: Vector[GroundLiteral] = newPropositions.toVector.sorted

    val propositionPairs: Set[(GroundLiteral, GroundLiteral)] = (for (x <- 0 until sortedPropositions.size - 1; y <- x + 1 until sortedPropositions.size) yield (sortedPropositions(x),
      sortedPropositions(y))) ++ (for (x <- previousLayer._3; y <- sortedPropositions) yield if ((x compare y) < 0) (x, y) else (y, x)) toSet

    //println("TO CHECK: " + propositionPairs.size + " DUP " + sortedPropositions.size + " vs " + (allPropositions.size - previousLayer._3.size))

    val propositionMutexes: Set[(GroundLiteral, GroundLiteral)] = computeMutexes match {
      case true  => (propositionPairs ++ previousLayer._4) filter { case (groundLiteral1, groundLiteral2) =>
        (for (x <- propositionProducers(groundLiteral1); y <- propositionProducers(groundLiteral2)) yield if ((x compare y) < 0) (x, y) else (y, x)) forall allTaskMutexes.contains
      }
      case false => Set.empty[(GroundLiteral, GroundLiteral)]
    }
    //println("PROPO MUTEX " + propositionMutexes.size)

    // termination and looping checks. Loop if something has changed compared with the previous layer of the PG
    val thisLayer = (allGroundTasks, allTaskMutexes, allPropositions, propositionMutexes)
    if (newPropositions.isEmpty && previousLayer._4.size == propositionMutexes.size) {
      if (previousLayer._1.size != allGroundTasks.size || previousLayer._2.size != allTaskMutexes.size || (previousLayer._3 == initialState && previousLayer._4.isEmpty)) {
        graph :+ thisLayer
      } else {
        graph
      }
    } else {
      buildGraph(graph :+ thisLayer, newPropositions, previousLayer._4 diff propositionMutexes, firstLayer = false, updatedPrecondMap)
    }
  }

  /**
    * Does the actual instantiation magic, iterals over pairs of preconditions (the current one is the argument literal) and a matching ground literal (currently groundLiteral).
    * Keeps the current assignment of arguments in the assignMap
    */
  def createActionInstances(task: ReducedTask, groundLiteral: GroundLiteral, literal: Literal, unassignedPrecons: Seq[Literal], mutexes: Set[(GroundLiteral, GroundLiteral)],
                            preconMap: Map[Predicate, Set[GroundLiteral]], assignMap: Map[Variable, Constant] = Map(), groundLiterals: Seq[GroundLiteral] = Seq.empty[GroundLiteral]):
  Set[GroundTask] =

    if (groundLiteral.parameter zip literal.parameterVariables exists { case (c, v) => !(v.sort.elements contains c) }) {
      Set()
    }
    else {
      assignMap foreach { case (v, c) => assert(v.sort.elements contains c) }

      val updatedPrecons: Seq[Literal] = unassignedPrecons filterNot {
        _ == literal
      }
      val updatedGroundLiterals = groundLiterals :+ groundLiteral
      val assignPairs: Seq[(Variable, Constant)] = literal.parameterVariables zip groundLiteral.parameter
      assignPairs foreach { case (v, c) => assert(v.sort.elements contains c) }
      val updatedAssignMap: Map[Variable, Constant] = assignPairs.foldLeft(assignMap) { case (aMap, (variable, constant)) => aMap + (variable -> constant) }

      // check whether we might have violated parameter constraints
      val taskConstraintsOK = task.parameterConstraints forall {
        case Equal(var1, var2: Variable)     => if ((updatedAssignMap contains var1) && (updatedAssignMap contains var2)) updatedAssignMap(var1) == updatedAssignMap(var2) else true
        case Equal(var1, const: Constant)    => if (updatedAssignMap contains var1) updatedAssignMap(var1) == const else true
        case NotEqual(var1, var2: Variable)  => if ((updatedAssignMap contains var1) && (updatedAssignMap contains var2)) updatedAssignMap(var1) != updatedAssignMap(var2) else true
        case NotEqual(var1, const: Constant) => if (updatedAssignMap contains var1) updatedAssignMap(var1) != const else true
        case OfSort(vari, sort)              => if (updatedAssignMap contains vari) sort.elements contains updatedAssignMap(vari) else true
        case NotOfSort(vari, sort)           => if (updatedAssignMap contains vari) !(sort.elements contains updatedAssignMap(vari)) else true
      }
      if (taskConstraintsOK) {

        //Check if all preconditions of the task have been assigned
        //println("CAI: TASK NAME:" + task.name)
        if (updatedPrecons.isEmpty) {

          //Check if all parameters of the task have been assigned
          if (task.parameters.size == updatedAssignMap.keys.size) {
            //println("Created!")
            val arguments: Seq[Constant] = task.parameters map { variable => updatedAssignMap(variable) }
            val newGroundTask = GroundTask(task, arguments)
            disallowedTasks match {
              case Left(disallowedGroundTasks)  => if (disallowedGroundTasks contains newGroundTask) Set.empty[GroundTask] else Set(newGroundTask)
              case Right(disallowedLiftedTasks) => Set(newGroundTask)
            }
          } else {
            val unassignedVariables: Seq[Variable] = task.parameters filterNot { variable => updatedAssignMap.keySet contains variable }

            val possibleSubstitutionCombinations: Seq[Seq[Constant]] = computeAllPossibleSubstitutionCombinations(unassignedVariables)

            val possibleSubstitutionCombinationsWithVariables: Seq[Seq[(Variable, Constant)]] = possibleSubstitutionCombinations map { seq => unassignedVariables.zip(seq) }
            val allArgumentCombinations: Seq[Seq[Constant]] = possibleSubstitutionCombinationsWithVariables map { combination =>
              task.parameters map { variable => updatedAssignMap.getOrElse(variable, combination.find { case (v, c) => v == variable }.get._2) }
            }
            (allArgumentCombinations collect { case arguments if task areParametersAllowed arguments => GroundTask(task, arguments) }).toSet
          }
        } else {
          val nextLiteral = updatedPrecons.head

          val possiblePrecs = preconMap.getOrElse(nextLiteral.predicate, Set.empty[GroundLiteral]) filter { gLCandidate => isMutexFree(updatedGroundLiterals, mutexes, gLCandidate) &&
            checkCorrectAssignment(nextLiteral, gLCandidate, updatedAssignMap)
          }

          possiblePrecs flatMap {
            nextGroundLiteral => createActionInstances(task, nextGroundLiteral, nextLiteral, updatedPrecons, mutexes, preconMap, updatedAssignMap, updatedGroundLiterals)
          }
        }
      } else Set()
    }

  def fillPreconMap(preconMap: Map[Predicate, Set[GroundLiteral]], propositions: Set[GroundLiteral]): Map[Predicate, Set[GroundLiteral]] =
    propositions
      .foldLeft(preconMap) { case (pMap, groundLiteral) => pMap + (groundLiteral.predicate -> (pMap.getOrElse(groundLiteral.predicate, Set.empty[GroundLiteral]) + groundLiteral)) }

  def checkCorrectAssignment(checkedLiteral: Literal, potentialGroundLiteral: GroundLiteral, assignmentMap: Map[Variable, Constant]): Boolean = {
    (checkedLiteral.parameterVariables zip potentialGroundLiteral.parameter) forall { case (variable, constant) => assignmentMap.getOrElse(variable, constant) == constant }
  }

  def isMutexFree(groundLiterals: Seq[GroundLiteral], mutexes: Set[(GroundLiteral, GroundLiteral)], potentialGroundLiteral: GroundLiteral): Boolean = {
    val allGroundLiterals = (groundLiterals :+ potentialGroundLiteral).toVector.sorted
    val groundLiteralPairs = for (a <- 0 until allGroundLiterals.size - 1; b <- a + 1 until allGroundLiterals.size) yield (allGroundLiterals(a), allGroundLiterals(b))
    //println("MutexCheck| -> groundLiteralPairs: " + groundLiteralPairs.size)
    for (x <- groundLiteralPairs) {
      //println("(" + x._1.predicate.name + "," + x._2.predicate.name + ")")
    }
    groundLiteralPairs forall { potentialMutex => !(mutexes contains potentialMutex) }
  }

  //TODO: can be improved, somewhere in Gregor's code is the solution
  def computeAllPossibleSubstitutionCombinations(variables: Seq[Variable]): Seq[Seq[Constant]] = {
    val variablesWithPossibleSubstitutions: Seq[(Variable, Seq[Constant])] = variables map { variable => (variable, variable.sort.elements) }
    def computeSubstitutionCombinations[A](sets: Seq[Seq[A]]): Seq[Seq[A]] = sets match {
      case Nil     => List(Nil)
      case s +: ss => computeSubstitutionCombinations(ss).flatMap(s2 => s.map(_ +: s2))
    }
    computeSubstitutionCombinations(variablesWithPossibleSubstitutions map { case (v, c) => c })
  }

  def createActionInstancesForTasksWithoutPreconditions(tasks: Seq[ReducedTask]): Set[GroundTask] = {
    (tasks flatMap { task => computeAllPossibleSubstitutionCombinations(task.parameters) map { combination => GroundTask(task, combination) } }).toSet
  }

  def createNOOP(groundLiteral: GroundLiteral): GroundTask = {
    val parameters: Seq[Variable] = groundLiteral.parameter.zipWithIndex map { case (constant, id) => Variable(id, "no-op",
                                                                                                               (domain.sorts find { sort => sort.elements contains constant }).get)
    }
    val literal: Literal = Literal(groundLiteral.predicate, isPositive = true, parameters)
    val task: ReducedTask = ReducedTask("NO-OP[" + groundLiteral.predicate.name + "]",
                                        isPrimitive = true, parameters, Nil, Seq.empty[VariableConstraint], And(Vector(literal)), And(Vector(literal)), ConstantActionCost(0))
    GroundTask(task, groundLiteral.parameter)
  }
}

object OldGroundedPlanningGraph {
  def apply(domain: Domain, initialState: Seq[GroundLiteral], computeMutexes: Boolean, isSerial: Boolean): OldGroundedPlanningGraph =
    OldGroundedPlanningGraph(domain, initialState toSet, computeMutexes, isSerial, Left(Nil))
}
