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

import java.io.{BufferedWriter, File, FileWriter}
import java.util.UUID
import java.util.concurrent.Semaphore

import de.uniulm.ki.panda3.configuration.Timings._
import de.uniulm.ki.panda3.symbolic.domain._
import de.uniulm.ki.panda3.symbolic.logic.{And, GroundLiteral}
import de.uniulm.ki.panda3.configuration._
import de.uniulm.ki.panda3.symbolic.compiler.SHOPMethodCompiler
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.sat.additionalConstraints._
import de.uniulm.ki.panda3.symbolic.plan.element.{GroundTask, PlanStep}
import de.uniulm.ki.panda3.symbolic.sat.IntProblem
import de.uniulm.ki.util._

import scala.collection.{JavaConversions, Seq}
import scala.io.Source

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
// scalastyle:off method.length cyclomatic.complexity
case class SATRunner(domain: Domain, initialPlan: Plan, intProblem: IntProblem,
                     satSolver: Solvertype, solverPath: Option[String],
                     büchiAutomata: Seq[LTLAutomaton[_, _]], ltlFormulaAndEncoding: Seq[AdditionalSATConstraint],
                     pureLTLFormulae: Seq[LTLFormula],
                     referencePlan: Option[Seq[Task]], planDistanceMetric: Seq[PlanDistanceMetric],
                     reductionMethod: SATReductionMethod, usePDTMutexes: Boolean,
                     timeCapsule: TimeCapsule, informationCapsule: InformationCapsule,
                     encodingToUse: POEncoding, extractSolutionWithHierarchy: Boolean,
                     randomSeed: Long, solverThreads: Int) {

  private val fileDir = System.getProperty("os.name").toLowerCase() match {
    case osname if osname startsWith "windows"  => ""
    case osname if osname startsWith "mac os x" => "./"
    //case _                                      => "/dev/shm/" // normal OSes
    case _                                      => "./" // normal OSes
  }


  import sys.process._

  private var satProcess: Option[Process] = None

  private var expansionPossible = true

  private var solverLastStarted: Long = 0

  private def getPID(): Int = {
    val rt = java.lang.management.ManagementFactory.getRuntimeMXBean
    val jvm = rt.getClass.getDeclaredField("jvm")
    jvm.setAccessible(true)
    val vmManager = jvm.get(rt).asInstanceOf[sun.management.VMManagement]
    val method = vmManager.getClass.getDeclaredMethod("getProcessId")
    method.setAccessible(true)

    method.invoke(vmManager).asInstanceOf[Int]
  }

  def runWithTimeLimit(timelimit: Long, timeLimitForLastRun: Long, planLength: Int, offsetToK: Int, includeGoal: Boolean = true, defineK: Option[Int] = None,
                       checkSolution: Boolean = false, runOptimiser: Boolean = false):
  (Option[(Seq[PlanStep], Map[PlanStep, DecompositionMethod], Map[PlanStep, (PlanStep, PlanStep)])], Boolean, Boolean) = {


    //val K = VerifyEncoding.computeTDG(domain,initialPlan,15,Math.max, 0)

    //println("DP-K: " + K)

    //System exit 0


    /*domain.primitiveTasks foreach {pt =>
      println(pt.name)
      println("  pre:")
      pt.preconditionsAsPredicateBool foreach {case (n,bool) => println("  " + bool + " " + n)}
      println("  eff:")
      pt.effectsAsPredicateBool foreach {case (n,bool) => println("  " + bool + " " + n)}
    }

    System exit 0*/



    val timerSemaphore = new Semaphore(0)

    val runner = new Runnable {
      var result: Option[Option[(Seq[PlanStep], Map[PlanStep, DecompositionMethod], Map[PlanStep, (PlanStep, PlanStep)])]] = None

      override def run(): Unit = {
        timeCapsule switchTimerToCurrentThread Timings.TOTAL_TIME
        result = Some(SATRunner.this.run(planLength: Int, offsetToK, includeGoal, defineK, checkSolution, runOptimiser))
        timerSemaphore.acquire()
      }
    }
    // start thread
    val threadGroup = new ThreadGroup("sat group")
    val thread = new Thread(threadGroup, runner)
    thread.start()

    // wait
    val startTime = System.currentTimeMillis()
    var rounds = 0
    while (System.currentTimeMillis() - startTime <= (if (expansionPossible) timelimit else timeLimitForLastRun) * 1.5 && runner.result.isEmpty && thread.isAlive) {
      Thread.sleep(100)
      rounds += 1
      if (rounds % (60 * 10) == 1)
        println("Still waiting ... running for " + (System.currentTimeMillis() - startTime) + " will abort at " + (if (expansionPossible) timelimit else timeLimitForLastRun))
    }

    if (satProcess.isDefined && System.getProperty("os.name").toLowerCase().startsWith("linux")) {
      //satProcess.destroy()

      val pid = getPID()
      //println(pid)

      val uuid = UUID.randomUUID().toString
      val file = new File("__pid" + uuid)
      writeStringToFile("#!/bin/bash\npstree -p " + pid + " | grep time | grep -o '([0-9]*)' | grep -o '[0-9]*' > __pid" + uuid, "__kill" + uuid)

      ("bash " + "__kill" + uuid) !


      val childPID = Source.fromFile(file).mkString
      //println("CHILD PIDs " + childPID)
      ("rm __pid" + uuid) !

      ("rm __kill" + uuid) !

      if (childPID != "") {
        childPID.split("\n") foreach { c =>
          if (c.toInt != pid) {
            println("Kill SAT solver with PID " + c)
            //System exit 0
            ("kill -9 " + c) !
          }
        }
        // add time

        // if this was the last run (no expansion of PDT possible) and we got here, we have a timeout, so increase the used time beyond the TL
        val solverStillRunningPenalty = if (expansionPossible) System.currentTimeMillis() - solverLastStarted else timeLimitForLastRun + 100
        timeCapsule.addTo(Timings.TOTAL_TIME, solverStillRunningPenalty)
        timeCapsule.addTo(Timings.SAT_SOLVER, solverStillRunningPenalty)
      }
    }

    timeCapsule switchTimerToCurrentThread(Timings.TOTAL_TIME, Some(if (expansionPossible) timelimit else timeLimitForLastRun))
    timerSemaphore.release()

    JavaConversions.mapAsScalaMap(Thread.getAllStackTraces).keys filter { t => thread.getThreadGroup == t.getThreadGroup } foreach { t => t.stop() }
    timeCapsule.switchTimerToCurrentThreadOrIgnore(Timings.VERIFY_TOTAL)
    timeCapsule stopOrIgnore Timings.VERIFY_TOTAL


    if (runner.result.isEmpty) {
      val errorState = System.currentTimeMillis() - startTime <= (if (expansionPossible) timelimit else timeLimitForLastRun)
      if (errorState) Thread.sleep(500)
      (None, errorState, expansionPossible)
    } else (runner.result.get, false, expansionPossible)
  }


  private def evaluateLTLFormulaOnStates(formula: LTLFormula, states: Seq[Seq[GroundLiteral]], actions: Seq[GroundTask]): Boolean = {
    val rr = formula match {
      case PredicateAtom(p) => states.head exists { _.predicate == p }
      case TaskAtom(t)      => if (actions.isEmpty) false else actions.head.task == t
      case LTLNot(f)        => !evaluateLTLFormulaOnStates(f, states, actions)
      case LTLAnd(fs)       => fs.forall(f => evaluateLTLFormulaOnStates(f, states, actions))
      case LTLOr(fs)        => fs.exists(f => evaluateLTLFormulaOnStates(f, states, actions))
      case LTLNext(f)       => if (states.isEmpty) false else evaluateLTLFormulaOnStates(f, states.drop(1), actions.drop(1))
      case LTLWeakNext(f)   => if (states.isEmpty) true else evaluateLTLFormulaOnStates(f, states.drop(1), actions.drop(1))
      case LTLEventually(f) =>
        if (states.isEmpty) false
        else if (evaluateLTLFormulaOnStates(f, states, actions)) true
        else evaluateLTLFormulaOnStates(LTLEventually(f), states.drop(1), actions.drop(1))
      case LTLAlways(f)     =>
        if (states.isEmpty) true
        else evaluateLTLFormulaOnStates(f, states, actions) && evaluateLTLFormulaOnStates(LTLAlways(f), states.drop(1), actions.drop(1))
      case LTLUntil(f, g)   =>
        if (states.isEmpty) false
        else if (evaluateLTLFormulaOnStates(g, states, actions)) true
        else evaluateLTLFormulaOnStates(f, states, actions) && evaluateLTLFormulaOnStates(LTLUntil(f, g), states.drop(1), actions.drop(1))
    }
    //println("Checking result " + rr + " of " + formula)
    rr
  }

  def checkIfTaskSequenceIsAValidPlan(sequenceToVerify: Seq[Task], checkGoal: Boolean = true): Unit = {
    val groundTasks: Seq[GroundTask] = sequenceToVerify map { task => GroundTask(task, Nil) }
    val initialState: Seq[GroundLiteral] = initialPlan.groundedInitialStateOnlyPositive
    val stateSequence = groundTasks.foldLeft(initialState :: Nil)(
      { case (states, action) =>
        /*println("STATE")
        println(states.last map {x => "\t" + x.predicate.name} mkString("\n"))
        println("PREC " + action.task.name)
        println(action.task.preconditionsAsPredicateBool map {x => "\t" + x._1.name} mkString "\n")
        println()*/
        val state = states.last

        action.substitutedPreconditions foreach { prec => exitIfNot(state contains prec, "action " + action.task.name + " prec " + prec.predicate.name) }
        val nextState = ((state diff action.substitutedDelEffects.map(_.copy(isPositive = true))) ++ action.substitutedAddEffects).distinct

        states :+ nextState
      })

    pureLTLFormulae.zipWithIndex foreach { case (f, i) =>
      val r = evaluateLTLFormulaOnStates(f, stateSequence, groundTasks)
      println("Testing LTL formula #" + i + ": " + r)
      exitIfNot(r, "Formula " + f.toString + " is not fulfilled")
    }

    val finalState = stateSequence.last
    if (checkGoal) initialPlan.groundedGoalTask.substitutedPreconditions foreach { goalLiteral => exitIfNot(finalState contains goalLiteral, "GOAL: " + goalLiteral.predicate.name) }
  }

  def run(planLength: Int, offSetToK: Int, includeGoal: Boolean = true, defineK: Option[Int] = None, checkSolution: Boolean = false, runOptimiser: Boolean = false):
  Option[(Seq[PlanStep], Map[PlanStep, DecompositionMethod], Map[PlanStep, (PlanStep, PlanStep)])] =
    try {
      informationCapsule.set(Information.PLAN_LENGTH, planLength)

      // clear index cache of clause class
      Clause.clearCache()


      val additionalConstraintsGenerators: Seq[AdditionalSATConstraint] =
        büchiAutomata.zipWithIndex.map({
                                         case (b: BüchiAutomaton, i)       => BüchiFormulaEncoding(b, "büchi_" + i)
                                         case (a: AlternatingAutomaton, i) => AlternatingAutomatonFormulaEncoding(a, "aauto_" + i)
                                       }) ++
          ltlFormulaAndEncoding ++
          (planDistanceMetric map {
            case MissingOperators(maximumDifference)              => ActionSetDifference(referencePlan.get, maximumDifference)
            case MissingTaskInstances(maximumDifference)          => ActionMatchingDifference(referencePlan.get, maximumDifference)
            case MinimumCommonSubplan(minimumLength, ignoreOrder) => LongestCommonSubplan(referencePlan.get, minimumLength, ignoreOrder)
          })


      //val restrictionMethod: RestrictionMethod = SlotGloballyRestriction
      val restrictionMethod: RestrictionMethod = SlotOverTimeRestriction

      val additionalDisablingGraphEdges = additionalConstraintsGenerators collect { case e: AdditionalEdgesInDisablingGraph => e }

      // start verification
      val encoder = //TreeEncoding(domain, initialPlan, sequenceToVerify.length, offSetToK)
        if (domain.isClassical) {
          encodingToUse match {
            case KautzSelmanEncoding => KautzSelman(timeCapsule, domain, initialPlan, intProblem, planLength)
            case ExistsStepEncoding  => ExistsStep(timeCapsule, domain, initialPlan, intProblem, planLength, -1, additionalDisablingGraphEdges)
          }
        }
        else if (domain.isTotallyOrdered && initialPlan.orderingConstraints.isTotalOrder())
          TotallyOrderedEncoding(timeCapsule, domain, initialPlan, intProblem, reductionMethod, planLength, offSetToK, defineK, restrictionMethod, usePDTMutexes)
        //else GeneralEncoding(domain, initialPlan, Range(0,planLength) map {_ => null.asInstanceOf[Task]}, offSetToK, defineK).asInstanceOf[VerifyEncoding]
        else {
          encodingToUse match {
            case TotSATEncoding                =>
              exitIfNot(domain.isTotallyOrdered, "The domain is not totally ordered. The totSAT encoding can only be applied to a totally ordered domain.", noStack = true)
              exitIfNot(initialPlan.orderingConstraints.isTotalOrder(),
                        "The initial plan is not totally ordered. The totSAT encoding can only be applied to a totally ordered planning problem.", noStack = true)
              TotallyOrderedEncoding(timeCapsule, domain, initialPlan, intProblem, reductionMethod, planLength, offSetToK, defineK, restrictionMethod, usePDTMutexes)
            case TreeBeforeEncoding            =>
              TreeVariableOrderEncodingKautzSelman(timeCapsule, domain, initialPlan, intProblem, planLength, offSetToK, usePDTMutexes, defineK)
            case TreeBeforeExistsStepEncoding  =>
              TreeVariableOrderEncodingExistsStep(timeCapsule, domain, initialPlan, intProblem, planLength, planLength, offSetToK, usePDTMutexes, defineK, additionalDisablingGraphEdges)
            case ClassicalForbiddenEncoding    =>
              SOGKautzSelmanForbiddenEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, offSetToK, defineK, false, usePDTMutexes)
            case ExistsStepForbiddenEncoding   =>
              SOGExistsStepForbiddenEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, planLength, offSetToK, defineK, false, usePDTMutexes, additionalDisablingGraphEdges)
            case ClassicalImplicationEncoding  =>
              SOGKautzSelmanForbiddenEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, offSetToK, defineK, true, usePDTMutexes)
            case ExistsStepImplicationEncoding =>
              SOGExistsStepForbiddenEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, planLength, offSetToK, defineK, true, usePDTMutexes, additionalDisablingGraphEdges)
            case ClassicalN4Encoding           =>
              SOGClassicalN4Encoding(timeCapsule, domain, initialPlan, intProblem, planLength, offSetToK, usePDTMutexes, defineK)
            case POCLDirectEncoding            =>
              SOGPOCLDirectEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, reductionMethod, offSetToK, defineK, restrictionMethod, usePDTMutexes)
            case POCLDeleteEncoding            =>
              SOGPOCLDeleteEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, reductionMethod, offSetToK, defineK, restrictionMethod, usePDTMutexes)
            case POCLForbidEncoding            =>
              SOGPOCLForbidEffectEncoding(timeCapsule, domain, initialPlan, intProblem, planLength, reductionMethod, offSetToK, defineK, restrictionMethod, usePDTMutexes)
            case POStateEncoding               =>
              SOGPOREncoding(timeCapsule, domain, initialPlan, intProblem, planLength, reductionMethod, offSetToK, usePDTMutexes, defineK)
          }
        }

      // (3)
      /*println("K " + encoder.K)
      informationCapsule.set(Information.ICAPS_K, VerifyEncoding.computeICAPSK(domain, initialPlan, planLength))
      informationCapsule.set(Information.TSTG_K, VerifyEncoding.computeTSTGK(domain, initialPlan, planLength))
      informationCapsule.set(Information.DP_K, VerifyEncoding.computeTDG(domain, initialPlan, planLength, Math.max, 0))
      informationCapsule.set(Information.LOG_K, VerifyEncoding.computeMethodSize(domain, initialPlan, planLength))*/
      informationCapsule.set(Information.OFFSET_K, offSetToK)
      informationCapsule.set(Information.ACTUAL_K, encoder.K)


      // abort if K is zero
      if (encoder.K == 0 && domain.abstractTasks.nonEmpty){
        println("K = 0 ... aborting run as this cannot have a solution")

        return None
      }

      //println(informationCapsule.longInfo)

      timeCapsule start Timings.VERIFY_TOTAL
      timeCapsule start Timings.GENERATE_FORMULA

      timeCapsule start GENERATE_STATE_FORMULA
      val stateFormula = encoder.stateTransitionFormula ++ encoder.initialState ++ (if (includeGoal) encoder.goalState else Nil) ++ encoder.noAbstractsFormula
      timeCapsule stop GENERATE_STATE_FORMULA

      val planningFormula = (encoder.decompositionFormula ++ stateFormula).toArray

      val additionalConstraintsFormula = additionalConstraintsGenerators flatMap { constraint =>
        encoder match {
          //case x: EncodingWithLinearPlan       => constraint(x)
          case lp: LinearPrimitivePlanEncoding => constraint(lp)
          case _                               => assert(false); Nil
        }
      }

      val usedFormulaGeneral = planningFormula ++ additionalConstraintsFormula

      println("NUMBER OF CLAUSES " + usedFormulaGeneral.length)
      val primitiveClauses: Int =
        encoder.numberOfPrimitiveTransitionSystemClauses + encoder.initialState.length + (if (includeGoal) encoder.goalState.length else 0) + encoder.noAbstractsFormula.length
      println("NUMBER OF STATE CLAUSES " + primitiveClauses)
      println("NUMBER OF DECOMPOSITION CLAUSES " + encoder.decompositionFormula.length)
      val ordermappingClauses = usedFormulaGeneral.length - primitiveClauses - encoder.decompositionFormula.length
      println("NUMBER OF ORDER CLAUSES " + ordermappingClauses)
      val percentPrimitiveClauses = (primitiveClauses.toDouble / usedFormulaGeneral.length) * 100 + 0.005
      val percentDecomposition = (encoder.decompositionFormula.length.toDouble / usedFormulaGeneral.length) * 100 + 0.005
      val percentOrderMapping = (ordermappingClauses.toDouble / usedFormulaGeneral.length) * 100 + 0.005
      println("PERCENTAGES " + (percentPrimitiveClauses - (percentPrimitiveClauses % 0.01)) + "% " +
                (percentDecomposition - (percentDecomposition % 0.01)) + "% " +
                (percentOrderMapping - (percentOrderMapping % 0.01)) + "% ")


      informationCapsule.set(Information.NUMBER_OF_STATE, primitiveClauses)
      informationCapsule.set(Information.NUMBER_OF_DECOMPOSITION, encoder.decompositionFormula.length)
      informationCapsule.set(Information.NUMBER_OF_ORDERING, ordermappingClauses)

      //println("NUMBER OF ADDITIONAL CONSTRAINT CLAUSES " + additionalConstraintsFormula.length)

      expansionPossible = encoder.expansionPossible
      //println("Done")
      //System.in.read()
      timeCapsule stop Timings.GENERATE_FORMULA

      ///////////////// END OF PRIMARY FORMULA GENERATION
      // if we are optimising in the short loop (currently only POCL encoding), then this here becomes a loop

      // do sime kind of binary search for the plan size ...
      var lo = 1
      var hi = -1
      var next = if (runOptimiser) 1 else planLength
      var foundSolution: Option[(Seq[PlanStep], Map[PlanStep, DecompositionMethod], Map[PlanStep, (PlanStep, PlanStep)])] = None

      while ((next > 0 || next == planLength) && (lo + 1 < hi || hi == -1)) {
        timeCapsule startOrLetRun Timings.VERIFY_TOTAL
        //println("\t\t\t\t\tRunning with plan length " + next + " lo " + lo + " " + hi)
        // generate appropriate formula
        val usedFormula = usedFormulaGeneral ++ encoder.planLengthDependentFormula(next)

        //val bMAp = Clause.atomIndices.map(_.swap)
        //writeStringToFile(usedFormula map { c => c.disjuncts map { case a => (if (a < 0) "not " else "") + bMAp(Math.abs(a) - 1) } mkString "\t" } mkString "\n", "formula.txt")

        timeCapsule start Timings.TRANSFORM_DIMACS
        //println("READY TO WRITE")
        val uniqFileIdentifier = UUID.randomUUID().toString
        //println("UUID " + uniqFileIdentifier)
        val writer = new BufferedWriter(new FileWriter(new File(fileDir + "__cnfString" + uniqFileIdentifier)))
        val atomMap: Map[String, Int] = encoder.miniSATString(usedFormula, writer)
        //println("FLUSH")
        writer.flush()
        writer.close()
        //println("CLOSE")
        timeCapsule stop Timings.TRANSFORM_DIMACS

        val tritivallUnsatisfiable = encoder match {
          case pathbased: PathBasedEncoding[_, _] =>
            //println(tot.primitivePaths map { case (a, b) => (a, b map { _.name }) } mkString "\n")
            informationCapsule.set(Information.NUMBER_OF_PATHS, pathbased.primitivePaths.length)
            println("NUMBER OF PATHS " + pathbased.primitivePaths.length)

            pathbased.primitivePaths.length == 0
          case _                                  => false
        }

        encoder match {
          case tot: TotallyOrderedEncoding     => informationCapsule.set(Information.MAX_PLAN_LENGTH, tot.primitivePaths.length)
          case tree: TreeVariableOrderEncoding => informationCapsule.set(Information.MAX_PLAN_LENGTH, tree.taskSequenceLength)
          case _                               =>
        }

        //println(timeCapsule.integralDataMap())

        def removeCommentAtBeginning(s: String): String = if (s.length == 0) s else {
          var i = 0
          while (s.charAt(i) == 'c') {
            while (s.length > i && s.charAt(i) != '\n')
              i += 1
            i += 1
          }

          s.substring(i)
        }

        // if we can't reach a primitive decomposition the whole PDT will be pruned, resulting in a trivially satisfiable SAT formula,
        // but the planning problem is clearly unsatisfiable
        if (tritivallUnsatisfiable) {
          println("Problem is trivially unsatisfiable ... exiting")
          timeCapsule stop Timings.VERIFY_TOTAL
          println("Removing files ... ")
          System.getProperty("os.name").toLowerCase match {
            case osname if osname startsWith "windows" =>
              ("cmd.exe /q /c del " + fileDir + "__cnfString" + uniqFileIdentifier) !!

            case osname if osname startsWith "mac os x" => ("rm " + fileDir + "__cnfString" + uniqFileIdentifier) !
            case _                                      => ("rm " + fileDir + "__cnfString" + uniqFileIdentifier) !

          }
          // abort the search
          lo = 1
          hi = 0
        } else {
          //System exit 0

          //timeCapsule start VerifyRunner.WRITE_FORMULA
          //writeStringToFile(cnfString, new File("__cnfString"))
          //timeCapsule stop VerifyRunner.WRITE_FORMULA

          //writeStringToFile(usedFormula map {_.disjuncts mkString "\t"} mkString "\n", new File("__formulaString"))

          try {
            val stdout = new StringBuilder
            val stderr = new StringBuilder
            val logger = ProcessLogger({ s => stdout append (s + "\n") }, { s => stderr append (s + "\n") })

            val outerScriptString = System.getProperty("os.name").toLowerCase match {
              case osname if osname startsWith "windows"  => ""
              case osname if osname startsWith "mac os x" => "#!/bin/bash\n"
              case _                                      => "#!/bin/bash\n/usr/bin/time -f '%U %S' "
            }

            val scriptFileName = fileDir + "__run" + uniqFileIdentifier + ".bat"

            println("Starting " + satSolver.longInfo)

            val solverCallString = satSolver match {
              case MINISAT                         =>
                solverPath.get + " -rnd-seed=" + randomSeed + " " + fileDir + "__cnfString" + uniqFileIdentifier + " " + fileDir + "__res" + uniqFileIdentifier + ".txt"
              case CRYPTOMINISAT | CRYPTOMINISAT55 =>
                solverPath.get + /*" -t " + solverThreads + " -r " + randomSeed + */ " --verb=0 " + fileDir + "__cnfString" + uniqFileIdentifier
              case RISS6                           =>
                solverPath.get + " -config=Riss427 -rnd-seed=" + randomSeed + " -verb=0 " + fileDir + "__cnfString" + uniqFileIdentifier
              case CADICAL                         =>
                solverPath.get + " --verbose=0 " + fileDir + "__cnfString" + uniqFileIdentifier
              case _: DefaultDIMACSSolver          =>
                solverPath.get + " -no-drup -rnd-seed=" + randomSeed + " -verb=0 " + fileDir + "__cnfString" + uniqFileIdentifier
            }
            writeStringToFile(outerScriptString + solverCallString, scriptFileName)

            solverLastStarted = System.currentTimeMillis()
            println("Setting starttime of solver to " + solverLastStarted)
            val runScriptString = System.getProperty("os.name").toLowerCase match {
              case osname if osname startsWith "windows"  => "cmd.exe /q /c  " + scriptFileName
              case osname if osname startsWith "mac os x" => "bash " + scriptFileName
              case _                                      => "bash " + scriptFileName
            }

            satProcess = Some(runScriptString.run(logger))

            // wait for termination
            satProcess.get.exitValue()
            satSolver match {
              case CRYPTOMINISAT | RISS6 | CADICAL | _: DefaultDIMACSSolver => writeStringToFile(stdout.toString(), new File(fileDir + "__res" + uniqFileIdentifier + ".txt"))
              case _                                                        =>
            }

            // remove runscript
            System.getProperty("os.name").toLowerCase match {
              case osname if osname startsWith "windows"  => ("cmd.exe /q /c del " + scriptFileName) !!
              case osname if osname startsWith "mac os x" => ("rm " + scriptFileName) !
              case _                                      => ("rm " + scriptFileName) !
            }
            // get time measurement
            val totalTime = System.getProperty("os.name").toLowerCase match {
              case osname if osname startsWith "windows"  => 0
              case osname if osname startsWith "mac os x" => 0
              case _                                      =>
                val errString = removeCommentAtBeginning(stderr.toString())
                println(errString)
                //println(errString.split('\n').map(x => "Line: " + x).mkString("\n"))

                val lines = errString.split('\n')
                val lineToTake = lines.find(x => '0' <= x.head && x.head <= '9').get

                (lineToTake.split(' ') map { _.toDouble * 1000 } sum).toInt
            }

            println("Time command gave the following runtime for the solver: " + totalTime)

            timeCapsule.addTo(SAT_SOLVER, totalTime)
            timeCapsule.set(SAT_SOLVER_K + "%04d".format(encoder.K), totalTime)
            //timeCapsule.addTo(TOTAL_TIME, totalTime)
            //timeCapsule.addTo(VERIFY_TOTAL, totalTime)

          } catch {
            case rt: RuntimeException => println("Minisat exitcode problem ..." + rt.toString)
              rt.printStackTrace()
              System exit 0
          }
          timeCapsule stop Timings.VERIFY_TOTAL

          print("Logging statistical information about the run ... ")
          val formulaVariables: Seq[String] = atomMap.keys.toSeq
          val averageClauseLength = (usedFormula map { _.disjuncts.length } sum).toDouble / usedFormula.length
          val assertClauses = usedFormula count { c => c.disjuncts.length == 1 && c.disjuncts.head > 0 }
          //val oneSided = usedFormula count { c => val x = c.disjuncts.head._2; c.disjuncts forall { _._2 == x } }
          val horn = usedFormula count { c => c.disjuncts.count(_ > 0) <= 1 }
          informationCapsule.set(Information.NUMBER_OF_VARIABLES, formulaVariables.size)
          informationCapsule.set(Information.NUMBER_OF_CLAUSES, usedFormula.length)
          informationCapsule.set(Information.AVERAGE_SIZE_OF_CLAUSES, "" + averageClauseLength)
          informationCapsule.set(Information.NUMBER_OF_ASSERT, assertClauses)
          //informationCapsule.set(Information.NUMBER_OF_ONE_SIDED, oneSided)
          informationCapsule.set(Information.NUMBER_OF_HORN, horn)

          informationCapsule.set(Information.STATE_FORMULA, stateFormula.length)
          //informationCapsule.set(Information.ORDER_CLAUSES, encoder.decompositionFormula count { _.disjuncts forall { case (a, _) => a.startsWith("before") || a.startsWith("childof") } })
          informationCapsule.set(Information.METHOD_CHILDREN_CLAUSES, encoder.numberOfChildrenClauses)
          println("done")

          // postprocessing
          print("Reading solver output ... ")
          val t1 = System.currentTimeMillis()
          val solverSource = Source.fromFile(fileDir + "__res" + uniqFileIdentifier + ".txt")
          val t2 = System.currentTimeMillis()
          val solverOutput = solverSource.mkString
          val t3 = System.currentTimeMillis()
          println("done")
          //println("done  " + (t2 - t1) + " " + (t3 - t2))
          print("Preparing solver output ... ")
          val t4 = System.currentTimeMillis()
          val (solveState, literals): (String, Set[Int]) = satSolver match {
            case MINISAT                                                  =>
              val splitted = solverOutput.split("\n")
              if (splitted.length == 1) (splitted(0), Set[Int]()) else (splitted(0), (splitted(1).split(" ") filter { _ != "" } map { _.toInt } filter { _ != 0 }).toSet)
            case CRYPTOMINISAT | RISS6 | CADICAL | _: DefaultDIMACSSolver =>

              val nonCommentOutput = removeCommentAtBeginning(solverOutput)
              if (nonCommentOutput.length < 100) println("STARTOUTPUT\n" + nonCommentOutput.replace(' ', '_') + "\nENDOUTPUT")

              val stateSplit = nonCommentOutput.split("\n", 2)
              val cleanState = stateSplit.head.replaceAll("s ", "")

              if (stateSplit.length == 1) (cleanState, Set[Int]())
              else {
                val singleItems: Seq[String] = stateSplit(1).split("\n").filterNot(_.startsWith("c")).flatMap(_.split(" "))
                val lits = singleItems.collect({ case s if s != "" && s != "\nv" && s != "v" && s != "0" && s != "0\n" => s.toInt }).toSet

                (cleanState, lits)
              }
          }
          val t5 = System.currentTimeMillis()
          println("done")
          //println("done " + (t5 - t4))

          // delete files
          System.getProperty("os.name").toLowerCase match {
            case osname if osname startsWith "windows" =>
              ("cmd.exe /q /c del " + fileDir + "__cnfString" + uniqFileIdentifier) !!

              ("cmd.exe /q /c del " + fileDir + "__res" + uniqFileIdentifier) !!

            case osname if osname startsWith "mac os x" => ("rm " + fileDir + "__cnfString" + uniqFileIdentifier + " " + fileDir + "__res" + uniqFileIdentifier + ".txt") !
            case _                                      => ("rm " + fileDir + "__cnfString" + uniqFileIdentifier + " " + fileDir + "__res" + uniqFileIdentifier + ".txt") !

          }


          // report on the result
          println("SAT-Solver says: " + solveState)
          val solved = solveState == "SAT" || solveState == "SATISFIABLE"

          // postprocessing
          if (solved) {
            println("")
            val allTrueAtoms: Set[String] = (atomMap filter { case (atom, index) => literals contains (index + 1) }).keys.toSet

            //writeStringToFile(allTrueAtoms mkString "\n", new File("true.txt"))
            //System exit 0

            //println((allTrueAtoms filter {_.startsWith("act_")}).toSeq.sorted mkString "\n")
            //println((allTrueAtoms filter {_.startsWith("auto_state")}).toSeq sortBy {case x => x.split('_').last.toInt}  mkString "\n")

            /*val db = allTrueAtoms filter { _ contains "direct_before" }

          val v: Set[String] = db flatMap { a => a.split("_").drop(2) }
          val e :Set[(String,String)]= db map { a => val x = a.split("_").drop(2); (x(0), x(1)) }

          val gg = SimpleDirectedGraph(v.toSeq, e.toSeq)
          Dot2PdfCompiler.writeDotToFile(gg,"graph.pdf")*/

            //System exit 0

            println("extracting solution")
            val (graphNodes, graphEdges, solutionSequence, methodsForAbstractTasks, parentsInDecompositionTree) =
              extractSolutionAndDecompositionGraph(encoder, atomMap, literals, formulaVariables, allTrueAtoms)


            if (checkSolution) runSolutionIntegrityCheck(encoder, graphNodes, graphEdges)

            // return the found solution
            foundSolution = Some(solutionSequence, methodsForAbstractTasks, parentsInDecompositionTree)

            // if we have solved and this is the "only" run, return
            if (!runOptimiser) {
              next = -2
            } else {
              // we are optimising
              if (hi == -1) {
                // this is the first solution we found: yipee, we have an upper bound, just start binary search
                hi = next
                next = (lo + hi) / 2
              } else {
                // binary search step, hi is ok
                hi = next
                next = (lo + hi) / 2
              }
            }

          } else {
            // have not found a solution, and the foundSolution already reflects that
            if (!runOptimiser) {
              next = -2
            } else {
              if (hi == -1) {
                lo = next // better lower bound for binary search
                next = next * 2 // just try larger plans
              } else {
                // binary search step, hi is not ok
                lo = next
                next = (lo + hi) / 2
              }
            }
          }
        }
      }

      foundSolution
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        None
    }


  private def extractSolutionAndDecompositionGraph(encoder: VerifyEncoding, atomMap: Map[String, Int], literals: Set[Int], formulaVariables: Seq[String], allTrueAtoms: Set[String]):
  (Seq[String], Seq[(String, String)], Seq[PlanStep], Map[PlanStep, DecompositionMethod], Map[PlanStep, (PlanStep, PlanStep)]) =
    encoder match {
      case g: GeneralEncoding =>
        // iterate through layers
        val nodes = Range(-1, encoder.numberOfLayers) flatMap { layer =>
          Range(0, g.numberOfActionsPerLayer) map { pos =>
            domain.tasks map { task =>
              val actionString = g.action(layer, pos, task)
              val isPres = if (atomMap contains actionString) literals contains (1 + atomMap(actionString)) else false
              (actionString, isPres)
            } find { _._2 }
          } filter { _.isDefined } map { _.get._1 }
        }

        val edges: Seq[(String, String)] = Range(-1, encoder.numberOfLayers) flatMap { layer =>
          Range(0, g.numberOfActionsPerLayer) flatMap { pos =>
            Range(0, g
              .numberOfActionsPerLayer) flatMap {
              father =>
                Range(0, encoder.DELTA) flatMap { childIndex =>
                  val childString = g.childWithIndex(layer, pos, father, childIndex)
                  if ((atomMap contains childString) && (literals contains (1 + atomMap(childString)))) {
                    // find parent and myself
                    val fatherStringOption = nodes find { _.startsWith("action^" + (layer - 1) + "_" + father) }
                    exitIfNot(fatherStringOption.isDefined, "action^" + (layer - 1) + "_" + father + " is not present but is a fathers")
                    val childStringOption = nodes find { _.startsWith("action^" + layer + "_" + pos) }
                    exitIfNot(childStringOption.isDefined, "action^" + layer + "_" + pos + " is not present but is a child")
                    (fatherStringOption.get, childStringOption.get) :: Nil
                  } else Nil
                }
            }
          }
        }

        (nodes, edges, ???, ???, ???)
      case ks: KautzSelman    =>
        val primitiveActions = allTrueAtoms filter { _.startsWith("action^") }
        println("Primitive Actions: \n" + (primitiveActions mkString "\n"))
        val actionsPerPosition = primitiveActions groupBy { _.split("_")(1).split(",")(0).toInt }
        val actionSequence = actionsPerPosition.keySet.toSeq.sorted map { pos => exitIfNot(actionsPerPosition(pos).size == 1); actionsPerPosition(pos).head }
        val primitiveSolution: Seq[PlanStep] = actionSequence map { case solAction =>
          val pos = solAction.split("_").last.split(",").head.toInt
          val actionIDX = solAction.split(",").last.toInt
          val task = domain.tasks(actionIDX)
          PlanStep(pos, task, Nil)
        }


        print("\n\nCHECKING primitive solution of length " + primitiveSolution.length + " ...")
        println("\n" + (primitiveSolution map { t => t.schema.isPrimitive + " " + t.id + " " + t.schema.name } mkString "\n"))

        checkIfTaskSequenceIsAValidPlan(primitiveSolution map { _.schema }, checkGoal = true)
        println(" done.")

        (Nil, Nil, primitiveSolution, Map(), Map())

      case es: ExistsStep               =>
        val primitiveActions = allTrueAtoms filter { _.startsWith("action^") }
        val statePredicates = allTrueAtoms filter { _.startsWith("predicate^") }
        //println("Primitive Actions: \n" + (primitiveActions mkString "\n"))
        val actionsPerPosition = primitiveActions groupBy { _.split("_")(1).split(",")(0).toInt }
        val predicatesPerPosition = statePredicates groupBy { _.split("_")(1).split(",")(0).toInt }



        //println(actionsPerPosition map { case (p, acts) => "Position " + p + "\n" + (acts map { "\t" + _ } mkString ("\n")) } mkString "\n")
        //println(predicatesPerPosition map { case (p, preds) => "Position " + p + "\n" + (preds map { "\t" + _ } mkString ("\n")) } mkString "\n")

        def stateAtTime(i: Int): String = predicatesPerPosition.getOrElse(i, Nil) map { pred => "\t" + domain.predicates(pred.split(",").last.toInt).name } mkString "\n"

        // try to get a linearisation of each position
        var c = -1
        val primitiveSolution: Seq[PlanStep] = actionsPerPosition.toSeq.sortBy(_._1) flatMap { case (p, acts) =>
          val executedActions: Set[Task] = acts map { solAction =>
            val pos = solAction.split("_").last.split(",").head.toInt
            val actionIDX = solAction.split(",").last.toInt
            domain.tasks(actionIDX)
          }

          val actionOrdering: Seq[Task] = executedActions.toSeq.sortWith(
            {
              case (t1, t2) => es.intProblem.disablingGraphTotalOrder.indexOf(t1) < es.intProblem.disablingGraphTotalOrder.indexOf(t2)
            })

          val x: Seq[PlanStep] = actionOrdering map { case a => c += 1; PlanStep(c, a, Nil) }

          println("Time " + p)
          //println(stateAtTime(p))
          println(actionOrdering map { "\t" + _.name } mkString ("\n"))

          x
        }

        //println("Time " + (actionsPerPosition.keys.max + 1))
        //println(stateAtTime(actionsPerPosition.keys.max + 1))

        //println(allTrueAtoms.toSeq filter {_.startsWith("matt")} sortBy {_.split("@")(1).toInt} mkString "\n")
        //println(allTrueAtoms.toSeq filter {_.startsWith("onparallel_2_")} sortBy {x => 2000*x.split("@")(1).toInt + x.split("@").head.split("_").last.toInt} mkString "\n")

        //println(domain.tasks.find(_.name == "Y[]").get.longInfo)


        print("\n\nCHECKING primitive solution of length " + primitiveSolution.length + " ...")
        //println("\n" + (primitiveSolution map { t => t.schema.isPrimitive + " " + t.id + " " + t.schema.name } mkString "\n"))

        checkIfTaskSequenceIsAValidPlan(primitiveSolution map { _.schema }, checkGoal = true)
        println(" done.")

        //System exit 0

        (Nil, Nil, primitiveSolution, Map(), Map())
      case pbe: PathBasedEncoding[_, _] =>
        val nodes = formulaVariables filter { _.startsWith("pathaction!") } filter allTrueAtoms.contains

        //println((allTrueAtoms filter {_.startsWith("method_path_to_pos_")}).toSeq.sorted mkString "\n")
        //println((allTrueAtoms filter {_.startsWith("forbidden_")}).toSeq.sorted mkString "\n")
        //println((allTrueAtoms filter {_.startsWith("pathToPos_")}).toSeq.sorted mkString "\n")
        //System exit 0

        val edges = nodes flatMap { parent =>
          nodes flatMap { child =>
            val parentLayer = parent.split("!").last.split("_").head.toInt
            val childLayer = child.split("!").last.split("_").head.toInt

            if (parentLayer + 1 != childLayer) Nil
            else {
              val parentPathString = parent.split("_").last.split(",").head
              val childPathString = child.split("_").last.split(",").head

              val parentPath = parentPathString.split(";").filter(_.nonEmpty) map { _.toInt }
              val childPath = childPathString.split(";").filter(_.nonEmpty) map { _.toInt }

              exitIfNot(parentPath.length + 1 == childPath.length)

              if (parentPath sameElements childPath.take(parentPath.length)) (parent, child) :: Nil else Nil
            }
          }
        }
        val graph: DirectedGraph[String] = SimpleDirectedGraph(nodes, edges)
        //Dot2PdfCompiler.writeDotToFile(graph,"dt-tree.pdf")
        // give all task a unique ID
        val nodeIDMap: Map[String, Int] = nodes.zipWithIndex.toMap
        val idNodeMap: Map[Int, String] = nodeIDMap.map(_.swap)
        val pathIDMap: Map[String, Int] = nodeIDMap map { case (n, id) => n.split("_").last.split(",").head -> id }

        //println(domain.tasks.zipWithIndex map {case (t,i) => i + ": " + t.name} mkString "\n")
        //println(domain.decompositionMethods.zipWithIndex map {case (t,i) => i + ": " + t.name} mkString "\n")

        def actionStringToTask(actionString: String): PlanStep = {
          val actionIDX = actionString.split(",").last.toInt
          val task = domain.tasks(actionIDX)
          PlanStep(nodeIDMap(actionString), task, Nil)
        }

        def nodeSortingFunction(t1: String, t2: String) = {
          val path1 = t1.split("_").last.split(",").head.split(";") map { _.toInt }
          val path2 = t2.split("_").last.split(",").head.split(";") map { _.toInt }
          PathBasedEncoding.pathSortingFunction(path1, path2)
        }

        //Dot2PdfCompiler.writeDotToFile(graph, "graph.pdf")

        val shortPS = nodes filterNot { _.contains("-") } map actionStringToTask map { ps => ps.id + " " + ps.schema.name + "\t" + domain.tasks.indexOf(ps.schema) }

        val allMethods = formulaVariables filter { _.startsWith("method^") } filter allTrueAtoms.contains

        //println(allMethods.sorted mkString "\n")
        //println(allTrueAtoms.filter(_.startsWith("pathaction")).toSeq.sorted mkString "\n")

        // attach methods to respective tasks
        val planStepsMethodMap: Map[PlanStep, DecompositionMethod] = if (!extractSolutionWithHierarchy) Map() else
          allMethods map { m =>
            val extract = m.split("_").last.split(",").head
            val ps = actionStringToTask(idNodeMap(pathIDMap(extract)))
            val method = domain.decompositionMethods(m.split(",").last.toInt)
            exitIfNot(method.abstractTask == ps.schema, method.abstractTask.name + " != " + ps.schema.name + "\n" + m + "\n" + pathIDMap(extract) + "\n" + idNodeMap(pathIDMap(extract)))
            println("Consider " + m + " " + m.split(",").last.toInt + " "  + method.name)
            println(extract + " " + pathIDMap(extract) + " " + idNodeMap(pathIDMap(extract)))
            ps -> method
          } toMap

        //println(planStepsMethodMap map { case (a, b) => a.schema.name + " -> " + b.name } mkString "\n")

        val parentInDecompositionMap: Map[PlanStep, (PlanStep, PlanStep)] = if (!extractSolutionWithHierarchy) Map() else
          edges.map(_.swap) collect { case (child, father) if !child.contains("-") && (actionStringToTask(child).schema.isAbstract || graph.edges(child).isEmpty) =>
            // find all children
            def getFirstFather(f: String): PlanStep = if (planStepsMethodMap.contains(actionStringToTask(f))) {
              //println("USE F " + f)
              actionStringToTask(f)
            } else getFirstFather(graph.reversedEdgesSet(f).head)

            val fatherPS = getFirstFather(father)
            val childPS = actionStringToTask(child)

            val siblings: Seq[PlanStep] = graph.edges(father).sortWith(nodeSortingFunction).filterNot(_.contains("-")).map(actionStringToTask)

            exitIfNot(siblings.count(_.schema == childPS.schema) == 1)

            //println("\n\nTASK " + child + " " + father + " " + planStepsMethodMap(fatherPS).name + " " + fatherPS.schema.name + " " + fatherPS.id)
            //println(childPS.schema.name)
            //println(planStepsMethodMap(fatherPS).subPlan.planStepsWithoutInitGoal.map(_.schema.name) mkString " ")
            //println("SIBS " + siblings.map(_.schema.name).mkString(" "))

            val planStepInMethod = planStepsMethodMap(fatherPS).subPlan.planStepsWithoutInitGoal.find(_.schema == childPS.schema).get

            exitIfNot(planStepInMethod.schema == childPS.schema)

            childPS -> (fatherPS, planStepInMethod)
          } toMap

        //println((domain.tasks.zipWithIndex map { case (t, i) => i + " : " + t.name } sorted).mkString("\n"))

        // can't annotate type : Seq[Task]
        val primitiveSolutionWithPotentialEmptyMethodApplications: Seq[PlanStep] = encoder match {
          case tot: TotallyOrderedEncoding =>


            //Dot2PdfCompiler.writeDotToFile(graph, "graph.pdf")
            /*graph.vertices foreach { t => val actionIDX = t.split(",").last.toInt;
              println("task " + t + " " + actionIDX + " " + domain.tasks(actionIDX).name)
              domain.tasks(actionIDX) }*/
            graph.sinks sortWith nodeSortingFunction map actionStringToTask

          case tree: TreeVariableOrderEncodingExistsStep =>
            val primitiveActions = allTrueAtoms filter { _.startsWith("action^") }
            val statePredicates = allTrueAtoms filter { _.startsWith("predicate^") }
            val pathToPos = allTrueAtoms filter { _.startsWith("pathToPos_") }
            val pathToPosWithTask = allTrueAtoms filter { _.startsWith("withTaskPathToPos_") }
            val actionsPerPosition = primitiveActions groupBy { _.split("_")(1).split(",")(0).toInt }
            val predicatesPerPosition = statePredicates groupBy { _.split("_")(1).split(",")(0).toInt }

            def stateAtTime(i: Int): String = predicatesPerPosition.getOrElse(i, Nil) map { pred => "\t" + domain.predicates(pred.split(",").last.toInt).name } mkString "\n"

            // try to get a linearisation of each position
            var c = -1
            val actionSequence: Seq[String] = actionsPerPosition.toSeq.sortBy(_._1) flatMap { case (p, acts) =>
              val executedActions: Set[(Task, String)] = acts map { solAction =>
                val pos = solAction.split("_").last.split(",").head.toInt
                val actionIDX = solAction.split(",").last.toInt
                (domain.tasks(actionIDX), solAction)
              }

              val actionOrdering: Seq[(Task, String)] = executedActions.toSeq.sortWith(
                {
                  case ((t1, _), (t2, _)) => tree.exsitsStepEncoding.intProblem.disablingGraphTotalOrder.indexOf(t1) < tree.exsitsStepEncoding.intProblem.disablingGraphTotalOrder.indexOf(t2)
                })

              println("Time " + p)
              //println(stateAtTime(p))
              println(actionOrdering map { x => "\t" + x._1.name + " " + x._2 } mkString ("\n"))

              actionOrdering map { _._2 }
            }

            val taskSequence = actionSequence map { case solAction =>
              val pos = solAction.split("_").last.split(",").head
              val taskID = solAction.split("_").last.split(",").last
              val pathToPosOption: Option[String] =
                if (tree.tasksWithOnePosition exists { tt => tree.taskIndex(tt) == taskID.toInt }) {
                  val allP2P = pathToPos filter { _.endsWith("-" + pos) }
                  // find the one whose path action is correct
                  allP2P find { p2p =>
                    val path = p2p.split("_").last.split("-").head
                    allTrueAtoms.contains("pathaction!" + (path.count(_ == ';') + 1) + "_" + path + "," + taskID)
                  }
                }
                else pathToPosWithTask find { _.endsWith("-" + pos + ":" + taskID) }
              val path = pathToPosOption match {
                case Some(s) => s.split("_").last.split("-").head
                case None    => exitIfNot(false, "action " + solAction + " has no connected path"); null
              }
              // find matching atom
              nodes find { _ contains ("_" + path + ",") } get
            } map actionStringToTask

            taskSequence
          case tree: TreeVariableOrderEncoding           =>
            val primitiveActions = allTrueAtoms filter { _.startsWith("action^") }
            val pathToPos = allTrueAtoms filter { _.startsWith("pathToPos_") }
            val pathToPosByPos: Map[String, String] = pathToPos groupBy { _.split("-").last } map { case (a, b) => exitIfNot(b.size == 1); a -> b.head }
            //println("Primitive Actions: \n" + (primitiveActions mkString "\n"))
            val actionsPerPosition = primitiveActions groupBy { _.split("_")(1).split(",")(0).toInt }
            val actionSequence = actionsPerPosition.keySet.toSeq.sorted map { pos => exitIfNot(actionsPerPosition(pos).size == 1); actionsPerPosition(pos).head }
            val taskSequence = actionSequence map { case solAction =>
              val pos = solAction.split("_").last.split(",").head
              val ptP = pathToPosByPos(pos)
              val path = ptP.split("_").last.split("-").head

              // find matching atom
              nodes find { _ contains ("_" + path + ",") } get
            } map actionStringToTask


            //println("Primitive Sequence with paths")
            //println(actionSequence map actionStringToInfoString mkString "\n")

            //val innerActions = allTrueAtoms filter { _.startsWith("pathaction!") } filterNot { t => t.contains("-1") || t.contains("-2") }
            //println("Inner actions with paths")
            //println(innerActions map actionStringToInfoString mkString "\n")


            //println(pathToPos mkString "\n")
            val active = allTrueAtoms filter { _.startsWith("active") }
            //println(active mkString "\n")
            exitIfNot(pathToPos.size == taskSequence.length)
            exitIfNot(active.size == taskSequence.length)

            //exitIfNot(graph.sinks.length == taskSequence.length, "SINKS " + graph.sinks.length + " vs " + taskSequence.length)
            val nextPredicates = allTrueAtoms filter { _.startsWith("next") }
            //println(nextPredicates mkString "\n")
            //exitIfNot(nextPredicates.size == taskSequence.length + 1, "NEXT " + nextPredicates.size + " vs " + (taskSequence.length + 1))

            val nextRel: Seq[(Array[Int], Array[Int])] =
              nextPredicates map { n => n.split("_").drop(1) } map { case l => (l.head, l(1)) } map { case (a, b) => (a.split(";") map { _.toInt }, b.split(";") map {
                _.toInt
              })
              } toSeq

            //println(nextRel map { case (a, b) => (a mkString ",") + ", " + (b mkString ",") } mkString "\n")
            //nextRel foreach { case (a, b) => exitIfNot(!(a sameElements b), "IDENTICAL " + (a mkString ",") + ", " + (b mkString ",")) }

            val lastPath = nextRel.indices.foldLeft((Array(-1), nextRel))(
              { case ((current, pairs), _) =>
                val (nextNext, remaining) = pairs partition { _._1 sameElements current }
                exitIfNot(nextNext.length == 1)

                (nextNext.head._2, remaining)
              })

            //exitIfNot(lastPath._1 sameElements Integer.MAX_VALUE :: Nil)

            taskSequence

          case sogTree: SOGEncoding =>

            //def actionStringToInfoString(t: String): String = {
            //  val actionIDX = t.split(",").last.toInt
            //  domain.tasks(actionIDX).name + " " + domain.tasks(actionIDX).isPrimitive + " " + t
            //}

            //val graph = SimpleDirectedGraph(nodes, edges)
            //println(graph.sinks filterNot { t => t.contains("-1") || t.contains("-2") } map actionStringToInfoString mkString "\n")

            sogTree match {

              case tree: SOGPartialNoPath     =>
                // extract partial order from formula
                val orderClauses = allTrueAtoms filter { _ startsWith "before" } map { _.split("_").tail } map { x => (x(0), x(1)) } toSeq

                val pathsToSinks: Map[String, (PlanStep, String)] = graph.sinks map { x =>
                  val actionID = x.split(",").last.toInt
                  val (action, id) = if (actionID == domain.tasks.length) (ReducedTask("init", true, Nil, Nil, Nil, And(Nil), And(Nil), ConstantActionCost(0)), -1)
                  else if (actionID == domain.tasks.length + 1) (ReducedTask("goal", true, Nil, Nil, Nil, And(Nil), And(Nil), ConstantActionCost(0)), -2)
                  else (domain.tasks(actionID), nodeIDMap(x))
                  val pathID = x.split("_")(1).split(",").head

                  pathID -> (PlanStep(id, action, Nil), pathID)
                } filter { _._2._1.schema.isPrimitive } toMap

                val v: Seq[(PlanStep, String)] = pathsToSinks.values.toSeq.distinct
                val e: Seq[((PlanStep, String), (PlanStep, String))] = orderClauses collect { case (before, after) if pathsToSinks.contains(before) && pathsToSinks.contains(after) =>
                  (pathsToSinks(before), pathsToSinks(after))
                }

                val partiallyOrderedSolution = SimpleDirectedGraph(v, e).transitiveReduction

                val graphString = partiallyOrderedSolution.dotString(options = DirectedGraphDotOptions(), nodeRenderer = {case (task, _) => task.schema.name})
                //Dot2PdfCompiler.writeDotToFile(graphString, "solutionOrder.pdf")

                // take a topological ordering (any should to it ...) and remove init and goal
                val withGoal = partiallyOrderedSolution.topologicalOrdering.get.tail
                println(withGoal.map(_._1.schema.name) mkString "\n")
                withGoal.take(withGoal.length - 1) map { _._1 }
              case tree: SOGClassicalEncoding =>
                val primitiveActions = allTrueAtoms filter { _.startsWith("action^") }
                val pathToPos = allTrueAtoms filter { _.startsWith("pathToPos_") }
                val actionsPerPosition = primitiveActions groupBy { _.split("_")(1).split(",")(0).toInt }

                val taskSequence = tree match {
                  case t: SOGKautzSelmanForbiddenEncoding =>
                    val pathToPosByPos = pathToPos groupBy { _.split("-").last } map { case (a, b) => exitIfNot(b.size == 1); a -> b.head }
                    //println("Primitive Actions: \n" + (primitiveActions mkString "\n"))
                    val actionSequence = actionsPerPosition.keySet.toSeq.sorted map { pos => exitIfNot(actionsPerPosition(pos).size == 1); actionsPerPosition(pos).head }
                    actionSequence map { case solAction =>
                      val pos = solAction.split("_").last.split(",").head
                      val ptP = pathToPosByPos(pos)
                      val path = ptP.split("_").last.split("-").head

                      // find matching atom
                      nodes find { _ contains ("_" + path + ",") } get
                    } map actionStringToTask

                  case t: SOGClassicalN4Encoding =>
                    val pathToPosByPos = pathToPos groupBy { _.split("-").last } map { case (a, b) => exitIfNot(b.size == 1); a -> b.head }
                    //println("Primitive Actions: \n" + (primitiveActions mkString "\n"))
                    val actionSequence = actionsPerPosition.keySet.toSeq.sorted map { pos => exitIfNot(actionsPerPosition(pos).size == 1); actionsPerPosition(pos).head }
                    actionSequence map { case solAction =>
                      val pos = solAction.split("_").last.split(",").head
                      val ptP = pathToPosByPos(pos)
                      val path = ptP.split("_").last.split("-").head

                      // find matching atom
                      nodes find { _ contains ("_" + path + ",") } get
                    } map actionStringToTask

                  case t: SOGExistsStepForbiddenEncoding =>
                    val pathToPosWithTask = allTrueAtoms filter { a => a.startsWith("withTaskPathToPos_") || a.startsWith("withTaskPathGroupToPos_")}
                    //println(pathToPosWithTask mkString "\n")
                    val statePredicates = allTrueAtoms filter { _.startsWith("predicate^") }
                    val predicatesPerPosition = statePredicates groupBy { _.split("_")(1).split(",")(0).toInt }

                    // try to get a linearisation of each position
                    var c = -1
                    val actionSequence: Seq[String] = actionsPerPosition.toSeq.sortBy(_._1) flatMap { case (p, acts) =>
                      val executedActions: Set[(Task, String)] = acts map { solAction =>
                        val pos = solAction.split("_").last.split(",").head.toInt
                        val actionIDX = solAction.split(",").last.toInt
                        (domain.tasks(actionIDX), solAction)
                      }

                      def stateAtTime(i: Int): String = predicatesPerPosition.getOrElse(i, Nil) map { pred => "\t" + domain.predicates(pred.split(",").last.toInt).name } mkString "\n"

                      val actionOrdering: Seq[(Task, String)] = executedActions.toSeq.sortWith(
                        {
                          case ((t1, _), (t2, _)) => t.exsitsStepEncoding.intProblem.disablingGraphTotalOrder.indexOf(t1) < t.exsitsStepEncoding.intProblem.disablingGraphTotalOrder
                            .indexOf(t2)
                        })

                      println("Time " + p)
                      //println(stateAtTime(p))
                      println(actionOrdering map { x => "\t" + x._1.name + " " + x._2 } mkString ("\n"))

                      actionOrdering map { _._2 }
                    }

                    //println(allTrueAtoms filter { _.startsWith("pathaction") } mkString "\n")
                    //println(pathToPos  mkString "\n")

                    val taskSeq = actionSequence map { case solAction =>
                      val pos = solAction.split("_").last.split(",").head
                      val taskID = solAction.split("_").last.split(",").last
                      val pathToPosOption: Option[String] =
                        //if (t.tasksWithOnePosition exists { tt => t.taskIndex(tt) == taskID.toInt })
                        {
                          val allP2P = pathToPos filter { _.endsWith("-" + pos) }
                          // find the one whose path action is correct
                          allP2P find { p2p =>
                            val path = p2p.split("_").last.split("-").head
                            allTrueAtoms.contains("pathaction!" + (path.count(_ == ';') + 1) + "_" + path + "," + taskID)
                          }
                        }
                        //else pathToPosWithTask find { _.endsWith("-" + pos + ":" + taskID) }
                      val path = pathToPosOption match {
                        case Some(s) => s.split("_").last.split("-").head
                        case None    => exitIfNot(false, "action " + solAction + " has no connected path"); null
                      }
                      // find matching atom
                      nodes find { _ contains ("_" + path + ",") } get
                    } map actionStringToTask


                    taskSeq
                }


                //println("Primitive Sequence with paths")
                //println(actionSequence map actionStringToInfoString mkString "\n")

                //val innerActions = allTrueAtoms filter { _.startsWith("pathaction!") } filterNot { t => t.contains("-1") || t.contains("-2") }
                //println("Inner actions with paths")
                //println(innerActions map actionStringToInfoString mkString "\n")


                //println(pathToPos mkString "\n")
                //println(primitiveActions mkString "\n")
                val active = allTrueAtoms filter { _.startsWith("active") }
                //println(active mkString "\n")
                //println(pathToPos.size + "==" + taskSequence.length)
                exitIfNot(pathToPos.size == taskSequence.length)
                exitIfNot(active.size == taskSequence.length)

                //exitIfNot(graph.sinks.length == taskSequence.length, "SINKS " + graph.sinks.length + " vs " + taskSequence.length)
                val nextPredicates = allTrueAtoms filter { _.startsWith("next") }
                //println(nextPredicates mkString "\n")
                //exitIfNot(nextPredicates.size == taskSequence.length + 1, "NEXT " + nextPredicates.size + " vs " + (taskSequence.length + 1))

                val nextRel: Seq[(Array[Int], Array[Int])] =
                  nextPredicates map { n => n.split("_").drop(1) } map { case l => (l.head, l(1)) } map { case (a, b) => (a.split(";") map { _.toInt }, b.split(";") map {
                    _.toInt
                  })
                  } toSeq

                //println(nextRel map { case (a, b) => (a mkString ",") + ", " + (b mkString ",") } mkString "\n")
                //nextRel foreach { case (a, b) => exitIfNot(!(a sameElements b), "IDENTICAL " + (a mkString ",") + ", " + (b mkString ",")) }

                val lastPath = nextRel.indices.foldLeft((Array(-1), nextRel))(
                  { case ((current, pairs), _) =>
                    val (nextNext, remaining) = pairs partition { _._1 sameElements current }
                    exitIfNot(nextNext.length == 1)

                    (nextNext.head._2, remaining)
                  })

                //exitIfNot(lastPath._1 sameElements Integer.MAX_VALUE :: Nil)

                taskSequence
            }
        }

        // there may be empty methods in the domain, which would produce abstract tasks as sinks. Hence we have to filter them out
        val primitiveSolution =
          primitiveSolutionWithPotentialEmptyMethodApplications filterNot { t =>
            t.schema.isAbstract && domain.methodsForAbstractTasks(t.schema).exists(_.subPlan.planStepsWithoutInitGoal.isEmpty)
          }

        //println(allTrueAtoms filter {_ contains "holds"} mkString "\n")

        print("\n\nCHECKING primitive solution of length " + primitiveSolution.length + " ...")
        println("\n" + (primitiveSolution map { t => t.schema.isPrimitive + " " + t.id + " " + t.schema.name } mkString "\n"))

        checkIfTaskSequenceIsAValidPlan(primitiveSolution map { _.schema }, checkGoal = true)
        println(" done.")

        (nodes, edges, primitiveSolution, planStepsMethodMap, parentInDecompositionMap)
    }


  private def runSolutionIntegrityCheck(encoder: VerifyEncoding, graphNodes: Seq[String], graphEdges: Seq[(String, String)]): Unit = {
    def changeSATNameToActionName(satName: String): SimpleGraphNode = {
      val actionID = satName.split(",").last.toInt
      if (actionID == domain.tasks.length) SimpleGraphNode(satName, "init")
      else if (actionID == domain.tasks.length + 1) SimpleGraphNode(satName, "init")
      else if (actionID >= 0) SimpleGraphNode(satName, (if (domain.tasks(actionID).isPrimitive) "!" else "") + domain.tasks(actionID).name) else SimpleGraphNode(satName, satName)
      //if (actionID >= 0) SimpleGraphNode(satName, "") else SimpleGraphNode(satName, satName)
    }

    val decompGraphNames = SimpleDirectedGraph(graphNodes map changeSATNameToActionName, graphEdges map { case (a, b) => (changeSATNameToActionName(a), changeSATNameToActionName(b)) })
    val decompGraph = SimpleDirectedGraph(graphNodes, graphEdges)
    //writeStringToFile(decompGraphNames.dotString, "decompName.dot")
    //Dot2PdfCompiler.writeDotToFile(decompGraph, "decomp.pdf")
    //Dot2PdfCompiler.writeDotToFile(decompGraphNames, "decompName.pdf")

    // no isolated nodes
    decompGraphNames.vertices foreach { v =>
      val (indeg, outdeg) = decompGraphNames.degrees(v)
      if (!v.id.contains("-1") && !v.id.contains("-2"))
        exitIfNot(indeg + outdeg != 0) // , "unconnected action " + v
    }

    if (encoder.isInstanceOf[PathBasedEncoding[_, _]]) {
      // check integrity of the methods
      decompGraphNames.vertices filter { v => decompGraphNames.degrees(v)._2 != 0 } foreach { v =>
        // either it is primitive
        val nei = decompGraphNames.edges(v)
        val myAction = domain.tasks(v.id.split(",").last.toInt)
        if (myAction.isPrimitive) {
          exitIfNot(nei.size == 1)
          exitIfNot(nei.head.name == v.name)
        } else {
          val subTasks: Seq[Task] = nei map { n => n.id.split(",").last.toInt } collect { case i if domain.tasks.length > i => domain.tasks(i) } filter {t =>
            t.isAbstract || ! t.name.contains(SHOPMethodCompiler.SHOP_METHOD_PRECONDITION_PREFIX)}
          val tasksSchemaCount = subTasks groupBy { p => p }
          //println("Checking " + v)
          //println(tasksSchemaCount map {case (a,b) => a.name + " " + b.size} mkString "\n")
          val possibleMethods = domain.methodsForAbstractTasks(myAction) map { m =>
            // only require the non-method preconditions to be present
            m.subPlan.planStepsWithoutInitGoal filter { ps => ps.schema.isAbstract || //!ps.schema.effect.isEmpty || !m.subPlan.orderingConstraints.fullGraph.sources.contains(ps) ||
              !ps.schema.name.contains(SHOPMethodCompiler.SHOP_METHOD_PRECONDITION_PREFIX)}
          } filter { planSteps =>
            //println("Plan steps " + planSteps.map(_.schema.name).mkString("\n\t"))
            val sameSize = planSteps.length == subTasks.length
            val planSchemaCount = planSteps groupBy { _.schema }
            val sameTasks = tasksSchemaCount.size == planSchemaCount.size && (tasksSchemaCount.keys forall planSchemaCount.contains)

            if (sameSize && sameTasks)
              tasksSchemaCount.keys forall { t => planSchemaCount.getOrElse(t,Nil).size == tasksSchemaCount.getOrElse(t,Nil).size } else false
          }
          exitIfNot(possibleMethods.nonEmpty, "Node " + v + " has no valid decomposition")
        }
      }

      // check order of methods
      decompGraphNames.vertices filter { v => decompGraphNames.degrees(v)._2 != 0 } foreach { v =>
        // either it is primitive
        val nei = decompGraphNames.edges(v)
        val myAction = domain.tasks(v.id.split(",").last.toInt)
        if (myAction.isPrimitive) {
          exitIfNot(nei.size == 1)
          exitIfNot(nei.head.name == v.name)
        } else if (encoder.isInstanceOf[TotallyOrderedEncoding]) {
          val subTasks: Seq[Task] = nei map { n => (n.id, domain.tasks(n.id.split(",").last.toInt)) } sortWith { case ((t1, _), (t2, _)) =>
            val path1 = t1.split("_").last.split(",").head.split(";") map { _.toInt }
            val path2 = t2.split("_").last.split(",").head.split(";") map { _.toInt }
            PathBasedEncoding.pathSortingFunction(path1, path2)
          } map { _._2 }

          val orderedMethods =
            domain.methodsForAbstractTasks(myAction) map { _.subPlan } map { plan =>
              exitIfNot(plan.orderingConstraints.graph.allTotalOrderings.get.size == 1)
              plan.orderingConstraints.graph.allTotalOrderings.get.head map { _.schema }
            } filter { _ == subTasks }

          exitIfNot(orderedMethods.nonEmpty, "Node " + v + " has no correctly ordered decomposition")
        }
      }
    }
  }

  def exitIfNot(f: Boolean, mess: String = "", noStack: Boolean = false): Unit = {
    if (!f) {
      if (mess != "") println(mess) else println("ATTENTION! AN ERROR OCCURRED")
      if (!noStack) println(Thread.currentThread().getStackTrace() map { _.toString } mkString "\n")
      System exit 0
    }
    assert(f)
  }

}

sealed trait POEncoding

object TotSATEncoding extends POEncoding

object TreeBeforeEncoding extends POEncoding

object TreeBeforeExistsStepEncoding extends POEncoding

object ClassicalForbiddenEncoding extends POEncoding

object ExistsStepForbiddenEncoding extends POEncoding

object ClassicalImplicationEncoding extends POEncoding

object ExistsStepImplicationEncoding extends POEncoding

object ClassicalN4Encoding extends POEncoding

object POCLDirectEncoding extends POEncoding

object POCLDeleteEncoding extends POEncoding

object POCLForbidEncoding extends POEncoding

object POStateEncoding extends POEncoding

object KautzSelmanEncoding extends POEncoding

object ExistsStepEncoding extends POEncoding
