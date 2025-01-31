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

import de.uniulm.ki.panda3.symbolic.compiler.SHOPMethodCompiler
import de.uniulm.ki.panda3.symbolic.domain.{Domain, Task}
import de.uniulm.ki.panda3.symbolic.logic.Predicate
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.sat.IntProblem
import de.uniulm.ki.util.{Dot2PdfCompiler, TimeCapsule}

import scala.collection.{Seq, mutable}

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait SOGClassicalEncoding extends SOGEncoding with EncodingWithLinearPlan with LeafMappingBasedEncoding {
  //lazy val taskSequenceLength: Int = taskSequenceLengthQQ

  override lazy val linearPlan: scala.Seq[Map[Task, String]] = {
    val allTasksArePossibleEverywhere: Set[Task] = primitivePaths flatMap { _._2 } toSet

    Range(0, taskSequenceLength) map { case i => allTasksArePossibleEverywhere map { t => t -> { action(K - 1, i, t) } } toMap }
  }


  override lazy val linearStateFeatures: scala.Seq[Map[Predicate, String]] = {
    // there is one more state
    Range(0, taskSequenceLength + 1) map { case i => domain.predicates map { p => p -> { statePredicate(K - 1, i, p) } } toMap }
  }


  override lazy val noAbstractsFormula: Seq[Clause] = noAbstractsFormulaOfLength(taskSequenceLength)

  def restrictionPathsPerPosition(pathsPerPosition: Map[Int, Seq[(Int, Int, String)]]): Seq[Clause]

  def ifActionAtPositionThenConnected(actionAtoms: Seq[(String, Task)], pathsPerPosition: Map[Int, Seq[(Int, Int, String)]], position: Int): Seq[Clause]

  protected lazy val connectionFormula: Seq[Clause] = {
    // force computation of SOG
    sog

    //Dot2PdfCompiler.writeDotToFile(sog.dotString(DirectedGraphDotOptions(), { case (p, t) => t map { _.name } mkString ";" }), "sog.pdf")


    //////
    // select mapping
    /////

    val pathAndPosition: Seq[(Int, Int, String)] =
      primitivePaths.zipWithIndex flatMap { case ((path, _), pindex) => Range(0, taskSequenceLength) map { position => (pindex, position, pathToPos(path, position)) } }

    // for every path the set of positions it can be linked to
    val positionsPerPath: Map[Int, Seq[(Int, Int, String)]] = pathAndPosition groupBy { _._1 }
    // for every position the paths it can be matched to
    val pathsPerPosition: Map[Int, Seq[(Int, Int, String)]] = pathAndPosition groupBy { _._2 }

    // each position can be mapped to at most one path and vice versa
    val atMostOneConstraintsA = restrictionPathsPerPosition(pathsPerPosition)
    val atMostOneConstraintsB = positionsPerPath.toSeq flatMap { case (a, s) => atMostOneOf(s map { _._3 }) }
    val atMostOneConstraints = atMostOneConstraintsA ++ atMostOneConstraintsB
    println("A " + atMostOneConstraintsA.size + " and " + atMostOneConstraintsB.size)


    // if the path is part of a solution, then it must contain a task
    val selected = primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      val actionAtoms = tasks.toSeq map { pathAction(path.length, path, _) }
      val pathString = pathActive(path)
      notImpliesAllNot(pathString :: Nil, actionAtoms).+:(impliesRightOr(pathString :: Nil, actionAtoms))
    }
    println("B " + selected.length)

    // if a path contains an action it has to be mapped to a position
    val onlySelectableIfChosen = primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      val pathString = pathActive(path)
      notImpliesAllNot(pathString :: Nil, positionsPerPath(pindex) map { _._3 }) :+ impliesRightOr(pathString :: Nil, positionsPerPath(pindex) map { _._3 })
    }
    println("C " + onlySelectableIfChosen.length)

    // positions may only contain primitive tasks if mapped to a path
    val onlyPrimitiveIfChosen = Range(0, taskSequenceLength) flatMap { case position =>
      val actionAtoms = domain.primitiveTasks filterNot ignoreActionInStateTransition map { t => (action(K - 1, position, t), t) }
      val ifPresentConnected = ifActionAtPositionThenConnected(actionAtoms, pathsPerPosition, position)
      val onlyIfConnected = notImpliesAllNot(pathsPerPosition(position) map { _._3 }, actionAtoms map { _._1 }) // TODO can be better

      ifPresentConnected ++ onlyIfConnected
    }
    println("D " + onlyPrimitiveIfChosen.length)

    // if a path contain an action, then the position it is mapped to contains the same action
    val sameAction = primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      tasks.toSeq map { t => (t, pathAction(path.length, path, t)) } flatMap { case (t, actionAtom) =>
        positionsPerPath(pindex) map { case (_, position, connectionAtom) =>
          impliesRightAndSingle(actionAtom :: connectionAtom :: Nil, action(K - 1, position, t))
        }
      }
    }
    println("E " + sameAction.length)

    val connection = atMostOneConstraints ++ selected ++ onlySelectableIfChosen ++ onlyPrimitiveIfChosen ++ sameAction

    connection.toSeq
  }
}

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait SOGClassicalForbiddenEncoding extends SOGClassicalEncoding {

  def useImplicationForbiddenness: Boolean

  protected override val omitMethodPreconditionActions = true

  protected def pathPosForbidden(path: Seq[Int], position: Int): String = "forbidden_" + path.mkString(";") + "-" + position

  protected def pathToPosMethod(path: Seq[Int], position: Int): String = "method_path_to_pos_" + path.mkString(";") + "-" + position

  protected def pathToPosMethodForbiddenAfter(path: Seq[Int], position: Int): String = "forbidden_method_path_to_pos_after_" + path.mkString(";") + "-" + position

  protected def pathToPosMethodForbiddenBefore(path: Seq[Int], position: Int): String = "forbidden_method_path_to_pos_before_" + path.mkString(";") + "-" + position

  def forbiddennessSubtractor: Int = 1

  override lazy val stateTransitionFormula: Seq[Clause] = {
    // force computation of SOG
    sog

    //////
    val connFormula = connectionFormula

    /////////////////
    // forbid certain connections if disallowed by the SOG
    /////////////////
    val forbiddenConnections: Seq[Clause] = primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      val successors = if (!useImplicationForbiddenness) sog.reachable.find(_._1._1 == path).get._2.toSeq else sog.edges.find(_._1._1 == path).get._2
      assert(!successors.exists(_._1 == path))

      // start from 1 as we have to access the predecessor position
      Range(forbiddennessSubtractor, taskSequenceLength) flatMap { pos =>
        impliesRightAnd(pathToPos(path, pos) :: Nil, successors map { case (succP, _) => pathPosForbidden(succP, pos - forbiddennessSubtractor) })
      }
    }
    println("F " + forbiddenConnections.length)

    val forbiddennessImplications: Seq[Clause] = if (!useImplicationForbiddenness) Nil
    else primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      val successors = if (!useImplicationForbiddenness) sog.reachable.find(_._1._1 == path).get._2.toSeq else sog.edges.find(_._1._1 == path).get._2
      assert(!successors.exists(_._1 == path))

      Range(0, taskSequenceLength) flatMap { pos =>
        impliesRightAnd(pathPosForbidden(path, pos) :: Nil, successors map { case (succP, _) => pathPosForbidden(succP, pos) })
      }
    }
    println("G " + forbiddennessImplications.length)


    val forbiddennessGetsInherited: Seq[Clause] = primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      // start from 1 as we have to access the predecessor position
      Range(1, taskSequenceLength) map { pos => impliesSingle(pathPosForbidden(path, pos), pathPosForbidden(path, pos - 1)) }
    }
    println("H " + forbiddennessGetsInherited.length)

    val forbiddenActuallyDoesSomething = primitivePaths.zipWithIndex flatMap { case ((path, tasks), pindex) =>
      Range(0, taskSequenceLength) map { pos => impliesNot(pathPosForbidden(path, pos), pathToPos(path, pos)) }
    }
    println("I " + forbiddenActuallyDoesSomething.length)

    val forbiddenness = forbiddenConnections ++ forbiddennessImplications ++ forbiddennessGetsInherited ++ forbiddenActuallyDoesSomething


    val methodPrecs = if (omitMethodPreconditionActions) {
      def methodPrecDFS(node: PathDecompositionTree[SOG]): Seq[Clause] = if (node.possibleMethods.isEmpty) Nil else {
        // matchable positions
        val allMatchings = Range(0, taskSequenceLength) map { pos => pathToPosMethod(node.path, pos) }
        val atLeastOneAssignment = node.possibleMethods map { case (m, mid) =>
          val methodAtom = method(node.layer, node.path, mid)
          impliesRightOr(methodAtom :: Nil, allMatchings)
        }

        val primitivePathsBelowMeSet = node.primitivePaths.map(_._1).toSet

        val predecessors: Seq[Seq[Int]] =
          (node.primitivePaths flatMap { case (path, _) =>
            val nodePred = sog.inverseGraph.reachable.find(_._1._1 == path).get._2.toSeq.map(_._1).filterNot(primitivePathsBelowMeSet)
            assert(!nodePred.contains(path))
            nodePred
          }).distinct

        // some matches are forbidden
        val notToEarly = Range(0, taskSequenceLength) flatMap { pos =>
          val inheritence = impliesSingle(pathToPosMethodForbiddenBefore(node.path, pos), pathToPosMethodForbiddenBefore(node.path, pos - 1)) :: Nil

          val generation = node.primitivePaths flatMap { case (path, _) =>

            predecessors map { pred => impliesSingle(pathToPos(pred, pos), pathToPosMethodForbiddenBefore(node.path, pos)) }
          }

          inheritence ++ generation
        }


        val notToLate = Range(0, taskSequenceLength) flatMap { pos =>
          val inheritence = impliesSingle(pathToPosMethodForbiddenAfter(node.path, pos), pathToPosMethodForbiddenAfter(node.path, pos + 1)) :: Nil

          val generation = node.primitivePaths map { case (path, _) =>
            //Range(pos + 1, taskSequenceLength) map { laterPos =>
            impliesSingle(pathToPos(path, pos), pathToPosMethodForbiddenAfter(node.path, pos + 1))

            //}
          }
          inheritence ++ generation
        }


        val forbiddenIsForbidden = node.primitivePaths flatMap { case (path, _) =>
          Range(0, taskSequenceLength) flatMap { pos =>
            impliesNot(pathToPosMethodForbiddenAfter(node.path, pos), pathToPosMethod(node.path, pos)) ::
              impliesNot(pathToPosMethodForbiddenBefore(node.path, pos), pathToPosMethod(node.path, pos)) ::
              Nil
          }
        }


        // if assigned and method, prec is true
        val methodPrecsAreTrue = node.possibleMethods flatMap { case (m, mid) =>
          if (!m.subPlan.planStepSchemaArrayWithoutMethodPreconditions.isEmpty) {
            val methodAtom = method(node.layer, node.path, mid)

            val prec = m.subPlan.orderingConstraints.fullGraph.sources.filter(ps => ps.schema.isPrimitive && ps.schema.effect.isEmpty &&
              ps.schema.name.contains(SHOPMethodCompiler.SHOP_METHOD_PRECONDITION_PREFIX)).
              flatMap(_.schema.posPreconditionAsPredicate)

            prec flatMap { p =>
              Range(0, taskSequenceLength) map { pos =>
                val pathPosAtom = pathToPosMethod(node.path, pos)
                val stateAtom = statePredicate(K - 1, pos, p)

                val condition = methodAtom :: pathPosAtom :: Nil
                impliesRightAndSingle(condition, stateAtom)
              }
            }
          } else Nil
        }

        val recursiveCalls = node.children flatMap methodPrecDFS


        if (methodPrecsAreTrue.isEmpty)
          recursiveCalls
        else
        //recursiveCalls ++ notToEarly ++ notToLate ++ atLeastOneAssignment ++ methodPrecsAreTrue
          recursiveCalls ++ notToEarly ++ notToLate ++ forbiddenIsForbidden ++ atLeastOneAssignment ++ methodPrecsAreTrue
      }

      methodPrecDFS(pdt)
    } else Nil

    println("J " + methodPrecs.size)

    // this generates the actual state transition formula
    val primitiveSequence = stateTransitionFormulaProvider

    primitiveSequence ++ connFormula ++ forbiddenness ++ methodPrecs
  }

  def stateTransitionFormulaProvider: Seq[Clause]

  override lazy val numberOfPrimitiveTransitionSystemClauses = stateTransitionFormulaProvider.length
}

case class SOGKautzSelmanForbiddenEncoding(timeCapsule: TimeCapsule, domain: Domain, initialPlan: Plan, intProblem: IntProblem,
                                           taskSequenceLengthQQ: Int, offsetToK: Int, overrideK: Option[Int] = None,
                                           useImplicationForbiddenness: Boolean, usePDTMutexes: Boolean)
  extends SOGClassicalForbiddenEncoding with KautzSelmanMappingEncoding[SOG, NonExpandedSOG] {

  lazy val taskSequenceLength: Int = if (taskSequenceLengthQQ != -1) taskSequenceLengthQQ else primitivePaths.length
}


case class SOGExistsStepForbiddenEncoding(timeCapsule: TimeCapsule, domain: Domain, initialPlan: Plan, intProblem: IntProblem,
                                          numberOfTimesteps: Int, maxNumberOfActionsArg: Int,
                                          offsetToK: Int, overrideK: Option[Int] = None,
                                          useImplicationForbiddenness: Boolean, usePDTMutexes: Boolean, additionalDisablingGraphEdges: Seq[AdditionalEdgesInDisablingGraph])
  extends SOGClassicalForbiddenEncoding with ExsitsStepMappingEncoding[SOG, NonExpandedSOG] {

  override lazy val maxNumberOfActions: Int = maxNumberOfActionsArg

  override def forbiddennessSubtractor: Int = 0

  // TODO: determine this size more intelligently
  lazy val taskSequenceLength: Int = if (numberOfTimesteps != -1) {
    // determine the maximum number of actions that can be used to achieve this cost
    val minimumCostsPerPath: Array[Int] = (primitivePaths map { _._2.map(_.cost.getFixedCost).min }).sorted.array

    var l = 0
    var c = 0
    while (l < minimumCostsPerPath.length && c < numberOfTimesteps) {
      c += minimumCostsPerPath(l)
      if (c <= numberOfTimesteps) l += 1
    }
    println("Maximum plan length for cost bound " + numberOfTimesteps + ": " + l)
    l
  } else {
    //val pathNumToUse = ((if (expansionPossible) primitivePaths.length else primitivePaths.length) * 0.66 + 0.5).toInt
    val pathNumToUse = if (expansionPossible) primitivePaths.length else primitivePaths.length

    Math.max(if (pathNumToUse == 0) 0 else 1, pathNumToUse - 0)
  }

  override def ignoreActionInStateTransition(task: Task): Boolean = taskOccurenceMap.getOrElse(0, Set()).contains(task)

}

case class SOGClassicalN4Encoding(timeCapsule: TimeCapsule,
                                  domain: Domain, initialPlan: Plan, intProblem: IntProblem, taskSequenceLengthQQ: Int, offsetToK: Int, usePDTMutexes: Boolean,
                                  overrideK: Option[Int] = None)
  extends SOGClassicalEncoding {

  lazy val taskSequenceLength: Int = primitivePaths.length

  override def restrictionPathsPerPosition(pathsPerPosition: Map[Int, Seq[(Int, Int, String)]]): Seq[Clause] =
    pathsPerPosition.toSeq flatMap { case (a, s) => atMostOneOf(s map { _._3 }) }

  def ifActionAtPositionThenConnected(actionAtoms: Seq[(String, Task)], pathsPerPosition: Map[Int, Seq[(Int, Int, String)]], position: Int): Seq[Clause] =
    atMostOneOf(actionAtoms map { _._1 })

  // this generates the actual state transition formula
  lazy val primitiveSequence = stateTransitionFormulaOfLength(taskSequenceLength)

  override lazy val stateTransitionFormula: Seq[Clause] = {
    // force computation of SOG
    sog

    /////////////////
    // forbid certain connections if disallowed by the SOG
    /////////////////
    val forbiddenness: Array[Clause] = {
      val builder = new mutable.ArrayBuffer[Clause]()

      primitivePaths foreach { case (pathBefore, _) =>
        val successors = sog.reachable.find(_._1._1 == pathBefore).get._2.toSeq

        successors foreach { case (pathAfter, _) =>
          // all positions
          Range(0, taskSequenceLength) foreach { case position1 =>
            Range(position1 + 1, taskSequenceLength) foreach { case position2 =>
              builder append Clause((pathToPos(pathBefore, position2), false) :: (pathToPos(pathAfter, position1), false) :: Nil)
            }
          }
        }
      }
      builder.toArray
    }


    //System exit 0
    primitiveSequence ++ connectionFormula ++ forbiddenness
  }

  override lazy val numberOfPrimitiveTransitionSystemClauses = primitiveSequence.length
}

