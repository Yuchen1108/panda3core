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

import de.uniulm.ki.panda3.configuration.SATReductionMethod
import de.uniulm.ki.panda3.symbolic.domain.{Task, Domain}
import de.uniulm.ki.panda3.symbolic.logic.Predicate
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.util._

import scala.collection.Seq
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * This is an encoding that uses POCL criteria to ensure that the resulting plan is executable
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
abstract class SOGPOCLEncoding extends SOGPartialNoPath with NumberOfActionsRestrictionViaAutomaton[SOG, NonExpandedSOG] {

  protected def preconditionOfPath(path: Seq[Int], precondition: Predicate): String = "prec^" + path.mkString(";") + "_" + precondition.name

  protected def effectOfPath(path: Seq[Int], precondition: Predicate): String = "eff^" + path.mkString(";") + "_" + precondition.name

  protected def supporter(pathA: Seq[Int], pathB: Seq[Int], precondition: Predicate): String = "supp^" + pathA.mkString(";") + "_" + pathB.mkString(";") + "_" + precondition.name

  override lazy val stateTransitionFormula: Seq[Clause] = {

    //val stringA = sog.dotString(options = DirectedGraphDotOptions(),
                                //nodeRenderer = {case (path, tasks) => tasks map { _.name } mkString ","})
    //                            nodeRenderer = {case (path, tasks) => tasks.count(_.isPrimitive) + " " + path})
    //Dot2PdfCompiler.writeDotToFile(stringA, "sogExt.pdf")


    // init and goal must be contained in the final plan
    val initAndGoalMustBePresent = Clause(pathAction(1, initVertex._1, initVertex._2.head)) :: Clause(pathAction(1, goalVertex._1, goalVertex._2.head)) :: Nil

    // for every present task, its preconditions must be supported
    val preconditionsMustBeSupportedTemp = extendedSOG.vertices flatMap { case node@(path, tasks) => tasks flatMap { t =>
      t.preconditionsAsPredicateBool map { case (prec, true) => (impliesSingle(pathAction(path.length, path, t), preconditionOfPath(path, prec)), (node, prec)) }
    }
    }

    val ifEffectThenPresent = extendedSOG.vertices flatMap { case node@(path, tasks) =>
      val allEffects: Set[Predicate] = tasks flatMap { _.effectsAsPredicateBool collect { case (eff, true) => eff } }

      allEffects map { case eff =>
        val producers = tasks.toSeq filter { _.effectsAsPredicateBool contains ((eff, true)) } map { t => pathAction(path.length, path, t) }
        impliesRightOr(effectOfPath(path, eff) :: Nil, producers)
      }
    }

    val preconditionsMustBeSupported = preconditionsMustBeSupportedTemp map { _._1 }
    val preconditions = preconditionsMustBeSupportedTemp map { _._2 } distinct

    println("Preconditions must be supported: " + preconditionsMustBeSupported.length + " clauses")
    println("Number of distinct preconditions: " + preconditions.length)

    // if a precondition is supported it must be supported by some action that can actually support it ...
    val supportedPreconditionsMustHaveSupporterTemp = preconditions map { case (n@(path, _), prec) =>
      // go over all task that are potentially ordered before ..
      val excludedTasks = extendedSOG.reachable(n) + n

      val potentialSupportingTasks: Seq[((Seq[Int], Set[Task]), Task)] = extendedSOG.vertices filterNot excludedTasks flatMap { case supporter@(sPath, sTasks) =>
        sTasks filter { _.effectsAsPredicateBool exists { case (p, s) => s && p == prec } } map { t => (supporter, t) }
      }
      val supporterLiterals: Seq[(String, Seq[Int])] = (potentialSupportingTasks map { _._1._1 } map { p => (supporter(p, path, prec), p) }).distinct

      val supportedPrecMustHaveSupporter = impliesRightOr(preconditionOfPath(path, prec) :: Nil, supporterLiterals.map(_._1).distinct)
      val supportLeadsToProduction = supporterLiterals map { case (supportLiteral, p) => impliesSingle(supportLiteral, effectOfPath(p, prec)) }

      (supportLeadsToProduction :+ supportedPrecMustHaveSupporter, potentialSupportingTasks map { x => (x._1._1, path, prec) })
    }

    val supportedPreconditionsMustHaveSupporter = supportedPreconditionsMustHaveSupporterTemp flatMap { _._1 }
    println("Supported preconditions must have supporter: " + supportedPreconditionsMustHaveSupporter.length + " clauses")
    val supporterLiterals: Seq[(Seq[Int], Seq[Int], Predicate)] = supportedPreconditionsMustHaveSupporterTemp flatMap { _._2 } distinct

    println("possible causal links: " + supporterLiterals.length)

    // output supporter graph
    //val supporterGraph = SimpleDirectedGraph(extendedSOG.vertices map {_._1}, supporterLiterals map {case (a,b,_)=> (a,b)} distinct)
    //Dot2PdfCompiler.writeDotToFile(supporterGraph, "clgraph.pdf")
    //Dot2PdfCompiler.writeDotToFile(supporterGraph.condensation, "clgraph-condensation.pdf")

    val pathOnlyExtendedSOG = extendedSOG.map(_._1)

    val supportImpliesOrder =
      supporterLiterals filterNot { case (p1, p2, _) => pathOnlyExtendedSOG.reachable(p1) contains p2 } map { case (p1, p2, prec) => impliesSingle(supporter(p1, p2, prec), before(p1, p2)) }
    println("Support implies order: " + supportImpliesOrder.length + " clauses")


    var startTime = System.currentTimeMillis()
    val orderMustBeTransitive: Array[Clause] = transitiveOrderClauses
    var endTime = System.currentTimeMillis()
    println("Order is transitive and respects SOG: " + orderMustBeTransitive.length + " clauses, time needed " + (endTime - startTime).toDouble./(1000))

    val noCausalThreat: Seq[Clause] = causalThreatsFormula(supporterLiterals)
    val restrictNumberOfActions: Seq[Clause] = numberOfActionsFormula(sog.topologicalOrdering.get)

    initAndGoalMustBePresent ++ preconditionsMustBeSupported ++ supportedPreconditionsMustHaveSupporter ++
      supportImpliesOrder ++ ifEffectThenPresent ++ orderMustBeTransitive ++ noCausalThreat ++ restrictNumberOfActions
  }

  override lazy val numberOfPrimitiveTransitionSystemClauses = stateTransitionFormula.length


  def causalThreatsFormula(supporterLiterals: Seq[(Seq[Int], Seq[Int], Predicate)]): Seq[Clause]

}
