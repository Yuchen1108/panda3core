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

import java.io.{File, FileInputStream}

import de.uniulm.ki.panda3.symbolic.compiler.pruning.PruneHierarchy
import de.uniulm.ki.panda3.symbolic.compiler.{ClosedWorldAssumption, Grounding, SHOPMethodCompiler, ToPlainFormulaRepresentation}
import de.uniulm.ki.panda3.symbolic.domain.Domain
import de.uniulm.ki.panda3.symbolic.domain.datastructures.hierarchicalreachability.NaiveGroundedTaskDecompositionGraph
import de.uniulm.ki.panda3.symbolic.domain.datastructures.primitivereachability.{GroundedForwardSearchReachabilityAnalysis, LiftedForwardSearchReachabilityAnalysis}
import de.uniulm.ki.panda3.symbolic.parser.xml.XMLParser
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.writer.hpddl.HPDDLWriter
import de.uniulm.ki.util._

import scala.collection.Seq
import scala.io.Source

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
object SATPlanner {

 /* def main(args: Array[String]) {
    import sys.process._

    //val domFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/sat/simpleDomain.hddl"
    //val probFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/sat/simpleProblem.hddl"
    //val domFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/SmartPhone-HierarchicalNoAxioms.xml"
    //val probFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/OrganizeMeeting_VeryVerySmall.xml"
    //val probFile = "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/OrganizeMeeting_VerySmall.xml"

    //val domFile = "/home/gregor/Workspace/panda2-system/domains/XML/UM-Translog/domains/UMTranslog.xml"
    //val probFile = "/home/gregor/Workspace/panda2-system/domains/XML/UM-Translog/problems/UMTranslog-P-1-Airplane.xml"

    //val domFile = "/home/gregor/Workspace/panda2-system/domains/XML/Satellite/domains/satellite2.xml"
    //val probFile = "/home/gregor/Workspace/panda2-system/domains/XML/Satellite/problems/4--1--3.xml"

    val domFile = "/home/gregor/Workspace/panda2-system/domains/XML/Woodworking/domains/woodworking.xml"
    val probFile = "/home/gregor/Workspace/panda2-system/domains/XML/Woodworking/problems/p01-hierarchical.xml"

    val domAndInitialPlan: (Domain, Plan) = XMLParser.asParser.parseDomainAndProblem(new FileInputStream(domFile), new FileInputStream(probFile))

    val sortExpansion = domAndInitialPlan._1.expandSortHierarchy()

    val parsedDom = domAndInitialPlan._1.update(sortExpansion)
    val parsedProblem = domAndInitialPlan._2.update(sortExpansion)

    // apply the CWA
    val cwaApplied = ClosedWorldAssumption.transform(parsedDom, parsedProblem)
    val simpleMethod = SHOPMethodCompiler.transform(cwaApplied, ())
    val flattened = ToPlainFormulaRepresentation.transform(simpleMethod, ())


    var beginMillis :Long = System.currentTimeMillis()
    val liftedRelaxedInitialState = flattened._2.init.schema.effectsAsPredicateBool
    val liftedReachabilityAnalysis = LiftedForwardSearchReachabilityAnalysis(flattened._1, liftedRelaxedInitialState.toSet)
    println()
    println("lifted analysis")
    println("" + liftedReachabilityAnalysis.reachableLiftedPrimitiveActions.size + " of " + flattened._1.primitiveTasks.size + " primitive tasks reachable")
    println("" + liftedReachabilityAnalysis.reachableLiftedLiterals.size + " of " + 2 * flattened._1.predicates.size + " lifted literals reachable")
    println("Time: " + (System.currentTimeMillis() - beginMillis))
    beginMillis = System.currentTimeMillis()


    val groundedInitialState = flattened._2.groundedInitialState
    val groundedReachabilityAnalysis = GroundedForwardSearchReachabilityAnalysis(flattened._1, groundedInitialState.toSet)()

    println()
    println("grounded analysis - I")
    println("" + groundedReachabilityAnalysis.reachableLiftedPrimitiveActions.size + " of " + flattened._1.primitiveTasks.size + " primitive tasks reachable")
    println("" + groundedReachabilityAnalysis.reachableLiftedLiterals.size + " of " + 2 * flattened._1.predicates.size + " lifted literals reachable")
    println("" + groundedReachabilityAnalysis.reachableGroundPrimitiveActions.size + " grounded primitive tasks reachable")
    println("" + groundedReachabilityAnalysis.reachableGroundLiterals.size + " grounded literals reachable")
    println("Time: " + (System.currentTimeMillis() - beginMillis))
    beginMillis = System.currentTimeMillis()

    val disallowedTasks = flattened._1.primitiveTasks filterNot groundedReachabilityAnalysis.reachableLiftedPrimitiveActions.contains
    val prunedDomain = PruneHierarchy.transform(flattened, disallowedTasks.toSet)

    val tdg = NaiveGroundedTaskDecompositionGraph(prunedDomain._1, prunedDomain._2, groundedReachabilityAnalysis, true)

    println()
    println("TDG:")
    println("reachable ground abstract tasks: " + tdg.reachableGroundAbstractActions.size)
    println("reachable ground primitive tasks: " + tdg.reachableGroundPrimitiveActions.size)
    println("reachable methods: " + tdg.reachableGroundMethods.size)
    println("Time: " + (System.currentTimeMillis() - beginMillis))
    beginMillis = System.currentTimeMillis()
    // try reachability again

    System.exit(0)

    val secondGroundedReachabilityAnalysis = GroundedForwardSearchReachabilityAnalysis(flattened._1, groundedInitialState.toSet)(tdg.reachableGroundedTasks)
    println()
    println("grounded analysis - II")
    println("" + groundedReachabilityAnalysis.reachableLiftedPrimitiveActions.size + " of " + flattened._1.primitiveTasks.size + " primitive tasks reachable")
    println("" + groundedReachabilityAnalysis.reachableLiftedLiterals.size + " of " + 2 * flattened._1.predicates.size + " lifted literals reachable")
    println("" + groundedReachabilityAnalysis.reachableGroundPrimitiveActions.size + " grounded primitive tasks reachable")
    println("" + groundedReachabilityAnalysis.reachableGroundLiterals.size + " grounded literals reachable")
    println("Time: " + (System.currentTimeMillis() - beginMillis))
    beginMillis = System.currentTimeMillis()


    // ground the domain ...
    val groundedDomain = Grounding.transform(prunedDomain, tdg)
    println(groundedDomain._1.statisticsString)

    val (dom, iniPlan) = groundedDomain

    writeStringToFile(HPDDLWriter("foo", "Bar").writeDomain(dom), new File("/home/gregor/groundedDom.hpddl"))
    writeStringToFile(HPDDLWriter("foo", "Bar").writeProblem(dom, iniPlan), new File("/home/gregor/groundedProf.hpddl"))

    println(dom.statisticsString)
    //val p1 = dom.primitiveTasks.find({ _.name == "p1" }).get
    //val p2 = dom.primitiveTasks.find({ _.name == "p2" }).get
    //val p3 = dom.primitiveTasks.find({ _.name == "p3" }).get

    // TODO still coded very badly
    val verifySeq = Range(0, 14) map { _ => null }

    val encoder = VerifyEncoding(dom, iniPlan, verifySeq,0)

    println("K " + encoder.K + " DELTA " + encoder.DELTA)

    val startTime = System.currentTimeMillis()
    val usedFormula = encoder.decompositionFormula ++ encoder.stateTransitionFormula ++ encoder.initialState ++ encoder.goalState ++ encoder.noAbstractsFormula //++ encoder.givenActionsFormula
    val formulaTime = System.currentTimeMillis()
    println("Variables : " + encoder.atoms.length + " Constraints: " + encoder.decompositionFormula.length)
    println("Time needed to compute the formula: " + (formulaTime - startTime) + "ms")
    val encodedString = encoder.miniSATString(usedFormula)
    val stringTime = System.currentTimeMillis()
    println("Time needed to convert the formula into a string: " + (stringTime - formulaTime) + "ms")

    writeStringToFile(usedFormula mkString "\n", new File("/home/gregor/formula"))
    writeStringToFile(encodedString, new File("/home/gregor/foo"))
    val stringToFileTime = System.currentTimeMillis()
    println("Time needed to wirte the string to file: " + (stringToFileTime - stringTime) + "ms")

    try {
      println("Starting minisat")
      "minisat /home/gregor/foo /home/gregor/res.txt" !
    } catch {
      case rt: RuntimeException => println("Minisat exitcode problem ...")
    }
    val minisatOutput = Source.fromFile("/home/gregor/res.txt").mkString
    val minisatResult = minisatOutput.split("\n")(0)
    println("MiniSAT says: " + minisatResult)
    if (minisatResult == "SAT") {
      val minisatAssignment = minisatOutput.split("\n")(1)
      val literals: Set[Int] = (minisatAssignment.split(" ") filter { _ != 0 } map { _.toInt }).toSet

      // iterate through layers
      val nodes = Range(-1, encoder.numberOfLayers) flatMap { layer => Range(0, encoder.numberOfActionsPerLayer) map { pos =>
        dom.tasks map { task =>
          val actionString = encoder.action(layer, pos, task)
          val isPres = if (encoder.atoms contains actionString) literals contains (1 + (encoder.atoms indexOf actionString)) else false
          (actionString, isPres)
        } find { _._2 }
      } filter { _.isDefined } map { _.get._1 }
      }

      val edges: Seq[(String, String)] = Range(-1, encoder.numberOfLayers) flatMap { layer => Range(0, encoder.numberOfActionsPerLayer) flatMap { pos =>
        Range(0, encoder.numberOfActionsPerLayer) flatMap { father =>
          Range(0, encoder.DELTA) flatMap { childIndex =>
            val childString = encoder.childWithIndex(layer, pos, father, childIndex)
            if ((encoder.atoms contains childString) && (literals contains (1 + (encoder.atoms indexOf childString)))) {
              // find parent and myself
              val fatherStringOption = nodes find { _.startsWith("action^" + (layer - 1) + "_" + father) }
              assert(fatherStringOption.isDefined, "action^" + (layer - 1) + "_" + father + " is not present but is a father")
              val childStringOption = nodes find { _.startsWith("action^" + layer + "_" + pos) }
              assert(childStringOption.isDefined, "action^" + layer + "_" + pos + " is not present but is a child")
              (fatherStringOption.get, childStringOption.get) :: Nil
            } else Nil
            //literals contains (1 + (encoder.atoms indexOf actionString)) else false
            //(actionString, isPres)
          }
        }
      }
      }

      val decompGraph = SimpleDirectedGraph(nodes, edges)
      Dot2PdfCompiler.writeDotToFile(decompGraph, "/home/gregor/decomp.pdf")
      val allTrueAtoms = encoder.atoms.zipWithIndex filter { case (atom, index) => literals contains (index + 1) } map { _._1 }
      writeStringToFile(allTrueAtoms mkString "\n", new File("/home/gregor/true.txt"))


      // isolate the primitive actions
      val primitiveActionStrings = nodes filter { _.startsWith("action^" + (encoder.numberOfLayers - 1)) } map { _.split("\\_")(1).split(",") } map { arr => (arr(0).toInt, arr(1).toInt) }
      //println(primitiveActionStrings mkString "\n")
      val primitiveActions = primitiveActionStrings.sorted map { case (idx, taskid) => (idx, dom.tasks(taskid).name + " primitive: " + dom.tasks(taskid).isPrimitive) }
      println(primitiveActions mkString "\n")

      // determine the states
      //println(allTrueAtoms filter { _.startsWith("predicate") } mkString "\n")
      val trueStateVariables = allTrueAtoms filter { _.startsWith("predicate") } map { _.split("\\_")(1) } map { _.split(",") } map { arr => (arr(0).toInt, arr(1).toInt) }
      val trueStateVariablesWithContent = trueStateVariables map { case (i, s) => (i, dom.predicates(s)) }
      //println(trueStateVariablesWithContent mkString "\n")
    }

    // print action mapping to numbers:
    //println(dom.tasks map { t => t.name + " -> " + encoder.taskIndex(t) } mkString "\n")
    //println(dom.predicates map { t => t.name + " -> " + encoder.predicateIndex(t) } mkString "\n")

    //println(encoder.atoms mkString "\n")

    //println(encodedString)
  }*/
}
