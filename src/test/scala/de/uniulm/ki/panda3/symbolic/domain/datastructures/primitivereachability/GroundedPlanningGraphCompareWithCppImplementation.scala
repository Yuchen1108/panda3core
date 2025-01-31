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

import java.io.FileInputStream
import java.io.File

import de.uniulm.ki.panda3.symbolic.compiler._
import de.uniulm.ki.panda3.symbolic.domain.{ReducedTask, Domain}
import de.uniulm.ki.panda3.symbolic.domain.datastructures.hierarchicalreachability.EverythingIsHiearchicallyReachable
import de.uniulm.ki.panda3.symbolic.logic.Literal
import de.uniulm.ki.panda3.symbolic.parser.hddl.HDDLParser
import de.uniulm.ki.panda3.symbolic.plan.Plan
import org.scalatest.FlatSpec

import scala.util.Random
import sys.process._
import de.uniulm.ki.util._

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
class GroundedPlanningGraphCompareWithCppImplementation extends FlatSpec {

  def writeDomainAndProblemToSimpleFormat(domain: Domain, problem: Plan): String = {
    domain.predicates foreach { p => assert(p.argumentSorts.isEmpty) }

    val builder = new StringBuilder

    // write initial state
    val (positiveInit, negativeInit) = domain.predicates partition { p => problem.groundedInitialState exists { gp => gp.predicate == p && gp.isPositive } }
    positiveInit.zipWithIndex foreach { case (p, i) => builder.append((if (i != 0) "," else "") + p.name.replace(',', '!')) }
    builder.append(";")
    negativeInit.zipWithIndex foreach { case (p, i) => builder.append((if (i != 0) "," else "") + p.name.replace(',', '!')) }
    builder.append("\n")

    def addLiteralList(literals: Seq[Literal]): Unit = literals.zipWithIndex foreach { case (l, i) => builder.append((if (i != 0) "," else "") + l.predicate.name.replace(',', '!')) }

    // goal state
    val (positiveGoal, negativeGoal) = problem.goal.substitutedPreconditions partition { _.isPositive }
    addLiteralList(positiveGoal)
    builder.append(";")
    addLiteralList(negativeGoal)
    builder.append("\n")

    // actions
    domain.tasks foreach { case ReducedTask(name, true, Nil, _, _, precondition, effect,_) =>
      builder.append(name.replace(',', '!') + ";")

      // precon
      precondition.conjuncts foreach { l => assert(l.isPositive) }
      addLiteralList(precondition.conjuncts)
      builder.append(";;")
      // effects
      val (positiveEffects, negativeEffects) = effect.conjuncts partition { _.isPositive }
      addLiteralList(positiveEffects)
      builder.append(";")
      addLiteralList(negativeEffects)

      builder.append("\n")
    }

    builder.toString()
  }

  def runComparisonWithDomain(domainFile: String, problemFile: String, useBuckets : Boolean): Unit = {

    // we assume that the domain is grounded
    val parsedDomainAndProblem = HDDLParser.parseDomainAndProblem(new FileInputStream(domainFile), new FileInputStream(problemFile))
    val sortsExpanded = ExpandSortHierarchy.transform(parsedDomainAndProblem, ())

    // cwa
    val cwaAppliedDomainAndProblem = ClosedWorldAssumption.transform(sortsExpanded, info = (true,Set[String]()))
    val plain = ToPlainFormulaRepresentation.transform(cwaAppliedDomainAndProblem, ())
    val negPre = RemoveNegativePreconditions.transform(plain, ())
    val (domain, initialPlan) = Grounding.transform(negPre, (EverythingIsHiearchicallyReachable(negPre._1, negPre._2), Map()))

    val domainString = writeDomainAndProblemToSimpleFormat(domain, initialPlan)
    val runID = Random.nextInt()
    writeStringToFile(domainString, new File("__probleminput" + runID))

    // compile the program
    "g++ -O2 src/test/resources/de/uniulm/ki/panda3/symbolic/domain/primitivereachability/planninggraph.cpp" !!

    //"cat __probleminput" #| "./a.out" !
    val cppPlanningGraphOutput: Seq[Seq[Int]] = (("cat __probleminput" + runID) #| "./a.out" !!) split "\n" map { _ split " " map { _.toInt } toSeq } toSeq

    val groundedInitialState = negPre._2.groundedInitialState filter { _.isPositive }
    val planningGraph = new GroundedPlanningGraph(negPre._1, groundedInitialState.toSet, GroundedPlanningGraphConfiguration(buckets = useBuckets))

    planningGraph.layerWithMutexes.drop(1) zip cppPlanningGraphOutput foreach { case ((a, b, c, d), cppRes) =>
      val newB = (b map { case (a, b) => if (a.task.name != b.task.name) {
        if (a.task.name < b.task.name) (a, b) else (b, a)
      }
      else {
        val argCompare = (a.arguments zip b.arguments).foldLeft[Option[Boolean]](None) {
                                                                                         case (Some(x), _)   => Some(x)
                                                                                         case (None, (x, y)) => if (x == y) None else if (x.name < y.name) Some(true) else Some(false)
                                                                                       }
        if (argCompare getOrElse true) (a, b) else (b, a)
      }
      }).toSeq.distinct
      println(a.size + " " + newB.size + " " + c.size + " " + d.size + " vs " + (cppRes mkString " "))
      println(a map {x => x.task.name + (x.arguments map { _.name }).mkString("(", ",", ")")} mkString "\n")
      println(newB.toSeq map {case (x,y) => x.task.name + (x.arguments map { _.name }).mkString("(", ",", ")") + "," + y.task.name + (y.arguments map { _.name }).mkString("(", ",", ")")}
                mkString "\n")
      val as = a.size
      val bs = newB.size
      val cs = c.size
      val ds = d.size
      assert(as == cppRes.head)
      //println(newB map { case (a, b) =>
      //  a.task.name + ((a.arguments map { _.name }).mkString("(", ",", ")")) + "!" + b.task.name + ((b.arguments map { _.name }).mkString("(", ",", ")"))
      //} mkString " ")
      assert(bs == cppRes(1))
      assert(cs == cppRes(2))
      assert(ds == cppRes(3))
    }
    "rm __probleminput" + runID + " a.out" !!
  }

  false :: true :: Nil foreach { useBuckets =>

    //
    "01" :: "02" :: "03" :: "04" :: "05" :: "06" :: Nil foreach { problemID =>
      //"03" :: Nil foreach { problemID =>
      "The grounded planning graph" + (if (useBuckets) " with buckets") must "produce the same result as Gregor's C++ implementation in TC " + problemID in {
        val domainFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/domain/primitivereachability/planningGraphTest" + problemID + "_domain.hddl"
        val problemFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/domain/primitivereachability/planningGraphTest" + problemID + "_problem.hddl"
        runComparisonWithDomain(domainFile, problemFile, useBuckets)
      }
    }

    // doing all 4 tests takes a very long time (~ 30 min, but they worked just after commit d99e80690007695f282e007dbde21e27384b491f)
    //"01" :: "02" :: "03" :: "04" :: Nil foreach { id =>
    "01" :: Nil foreach { id =>
      it must "produce the same result in PEGSOL " + id in {
        val domFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/pddl/IPC6/pegsol-strips/domain/p" + id + "-domain.pddl"
        val probFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/pddl/IPC6/pegsol-strips/problems/p" + id + ".pddl"
        runComparisonWithDomain(domFile, probFile, useBuckets)
      }
    }
  }
}
