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

import de.uniulm.ki.panda3.symbolic.domain.{ActionCost, Task}

import scala.collection.Seq

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait NumberOfActionsRestrictionViaAutomaton[P, I] extends PathBasedEncoding[P, I] {

  def restrictionMethod: RestrictionMethod

  def numberOfActionsFormula(vertexOrder: Seq[(Seq[Int], Set[Task])]): Seq[Clause] = numberOfActionsFormula2(vertexOrder)

  protected def actionsRemainingAtPath(path: Seq[Int], remaining: Int): String = "act_remain^" + path.mkString(";") + "_" + remaining

  protected def actionPositionChosen(path: Seq[Int], position: Int): String = "act_chosen^" + path.mkString(";") + "_" + position


  def numberOfActionsFormula1(vertexOrder: Seq[(Seq[Int], Set[Task])]): Seq[Clause] = if (taskSequenceLength <= 0) Nil else {


    restrictionMethod match {
      case SlotOverTimeRestriction =>
        val notPossibleImpliesNextNotPossible = vertexOrder.zip(vertexOrder.drop(1)) flatMap { case ((path1, _), (path2, _)) =>
          Range(0, taskSequenceLength) map { l => notImpliesNot(actionsRemainingAtPath(path1, l) :: Nil, actionsRemainingAtPath(path2, l)) }
        }

        val nextNotPossible = vertexOrder.zip(vertexOrder.drop(1)) flatMap { case ((path1, _), (path2, _)) =>
          Range(0, taskSequenceLength) map { l => impliesNot(actionPositionChosen(path1, l) :: Nil, actionsRemainingAtPath(path2, l)) }
        }

        val currentPossible = vertexOrder flatMap { case (path, _) => Range(0, taskSequenceLength) map { l => impliesSingle(actionPositionChosen(path, l), actionsRemainingAtPath(path, l)) }
        }

        val selectSlot: Seq[Clause] = vertexOrder flatMap { case node@(path, tasks) =>
          val selected = Range(0, taskSequenceLength) map { l => actionPositionChosen(path, l) }
          tasks map { t => impliesRightOr(pathAction(path.length, path, t) :: Nil, selected) }
        }

        notPossibleImpliesNextNotPossible ++ selectSlot ++ nextNotPossible ++ currentPossible

      case SlotGloballyRestriction =>
        val selectSlot: Seq[Clause] = vertexOrder flatMap { case node@(path, tasks) =>
          val selected = Range(0, taskSequenceLength) map { l => actionPositionChosen(path, l) }
          tasks map { t => impliesRightOr(pathAction(path.length, path, t) :: Nil, selected) }
        }

        val currentPossible: Seq[Clause] = vertexOrder flatMap { case (thisPath, _) => Range(0, taskSequenceLength) flatMap { thisPosition =>
          vertexOrder collect { case (thatPath, _) if thisPath != thatPath =>
            impliesNot(actionPositionChosen(thisPath, thisPosition), actionPositionChosen(thatPath, thisPosition))
          }
        }
        }

        selectSlot ++ currentPossible
    }
  }


  def actionAt(path: Seq[Int]): String = "action@" + path.mkString(";")

  def numberOfActionsBetween(pathA: Int, pathB: Int, num: Int) = "number_of_actions=" + num + "-" + pathA + "-" + pathB

  def numberOfActionsFormula2(vertexOrder: Seq[(Seq[Int], Set[Task])]): Seq[Clause] = if (taskSequenceLength == -1) Nil else {
    val actionAtClauses = vertexOrder flatMap { case (path, ts) =>
      val ifPresent = ts map { t =>
        if (t.cost.hasCostOne)
          impliesSingle(pathAction(path.length, path, t), actionAt(path))
        else if (t.cost.hasCostZero)
          impliesNot(pathAction(path.length, path, t), actionAt(path))
        else {assert(false, "can only handle costs 0 and 1"); ??? }
      }
      val ifNotPresent = notImpliesAllNot(actionAt(path) :: Nil, ts.toSeq filter { _.cost.hasCostOne } map { pathAction(path.length, path, _) })
      ifPresent ++ ifNotPresent
    }

    // counting actions
    val paths = vertexOrder map { _._1 } toArray
    val layers = paths.indices.find(x => 1 << x >= vertexOrder.length).get
    println("Layers: " + layers + " paths " + paths.length)

    // base case
    val countingBase =
      paths.zipWithIndex flatMap { case (p, i) =>
        val elementary = impliesSingle(actionAt(p), numberOfActionsBetween(i, i, 1)) :: notImplies(actionAt(p) :: Nil, numberOfActionsBetween(i, i, 0)) :: Nil
        val nonPresent = Range(2, paths.length + 1) map { x => Clause((numberOfActionsBetween(i, i, x), false)) }
        elementary ++ nonPresent
      }


    // group in layers
    val upwardsPropagation = Range(1, layers + 1) flatMap { l =>
      Range(0, Math.ceil(paths.length.toDouble / (1 << l)).toInt) flatMap { g =>
        // compute first last index per group
        val first = g * (1 << l)
        val last = Math.min((g + 1) * (1 << l), paths.length) - 1
        val middle = (g * 2 + 1) * (1 << (l - 1)) - 1 // last in the first part


        if (middle == last) Nil else {
          print("Layer " + l + " g " + g + " = " + first + " - " + last + " / " + middle)

          val x = Range(0, paths.length + 1) flatMap { target =>
            Range(0, target + 1) flatMap { left =>
              impliesRightAnd(numberOfActionsBetween(first, middle, left) :: numberOfActionsBetween(middle + 1, last, target - left) :: Nil,
                              numberOfActionsBetween(first, last, target) :: Nil)
            }
          }
          println(" clauses " + x.length)
          x
        }
      }
    }

    actionAtClauses ++ countingBase ++ upwardsPropagation
  }

  def numberOfActionsFormula3(vertexOrder: Seq[(Seq[Int], Set[Task])]): Seq[Clause] = if (taskSequenceLength == -1) Nil else {
    restrictionMethod match {
      case SlotOverTimeRestriction =>
        val notPossibleImpliesNextNotPossible = vertexOrder.zip(vertexOrder.drop(1)) flatMap { case ((path1, _), (path2, _)) =>
          Range(0, taskSequenceLength) map { l => notImpliesNot(actionsRemainingAtPath(path1, l) :: Nil, actionsRemainingAtPath(path2, l)) }
        }

        val nextNotPossible = vertexOrder.zip(vertexOrder.drop(1)) flatMap { case ((path1, _), (path2, _)) =>
          Range(0, taskSequenceLength) map { l => impliesNot(actionPositionChosen(path1, l) :: Nil, actionsRemainingAtPath(path2, l)) }
        }

        val currentPossible = vertexOrder flatMap { case (path, _) => Range(0, taskSequenceLength) map { l => impliesSingle(actionPositionChosen(path, l), actionsRemainingAtPath(path, l)) }
        }

        val selectSlot: Seq[Clause] = vertexOrder flatMap { case node@(path, tasks) =>
          val selected = Range(0, taskSequenceLength) map { l => actionPositionChosen(path, l) }
          tasks map { t => impliesRightOr(pathAction(path.length, path, t) :: Nil, selected) }
        }

        notPossibleImpliesNextNotPossible ++ selectSlot ++ nextNotPossible ++ currentPossible

      case SlotGloballyRestriction =>
        val selectSlot: Seq[Clause] = vertexOrder flatMap { case node@(path, tasks) =>
          val selected = Range(0, taskSequenceLength) map { l => actionPositionChosen(path, l) }
          tasks map { t => impliesRightOr(pathAction(path.length, path, t) :: Nil, selected) }
        }

        val currentPossible: Seq[Clause] = vertexOrder flatMap { case (thisPath, _) => Range(0, taskSequenceLength) flatMap { thisPosition =>
          vertexOrder collect { case (thatPath, _) if thisPath != thatPath =>
            impliesNot(actionPositionChosen(thisPath, thisPosition), actionPositionChosen(thatPath, thisPosition))
          }
        }
        }

        selectSlot ++ currentPossible
    }
  }

  override def planLengthDependentFormula(actualPlanLength: Int): Seq[Clause] = if (actualPlanLength == -1) Nil else
    Range(actualPlanLength + 1, primitivePaths.length + 1) map { l => Clause((numberOfActionsBetween(0, primitivePaths.length - 1, l), false)) }
}

sealed trait RestrictionMethod

object SlotOverTimeRestriction extends RestrictionMethod

object SlotGloballyRestriction extends RestrictionMethod
