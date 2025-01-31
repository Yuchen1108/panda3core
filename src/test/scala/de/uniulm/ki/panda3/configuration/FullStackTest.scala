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

package de.uniulm.ki.panda3.configuration

import java.io.FileInputStream

import de.uniulm.ki.panda3.progression.heuristics.htn.RelaxedComposition.gphRcFFMulticount
import de.uniulm.ki.panda3.progression.htn.search.searchRoutine.PriorityQueueSearch
import de.uniulm.ki.panda3.symbolic.search.SearchState
import org.scalatest.FlatSpec

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
class FullStackTest extends FlatSpec {
  val postprocessing = PostprocessingConfiguration(Set(ProcessingTimings, SearchStatistics, SearchStatus, SearchResult))

  val grounded = PreprocessingConfiguration(compileNegativePreconditions = true, compileUnitMethods = false,
                                            compileOrderInMethods = None,
                                            compileInitialPlan = false, removeUnnecessaryPredicates = false, removeNoOps = false,
                                            convertToSASP = false, allowSASPFromStrips = false, splitIndependentParameters = false,
                                            ensureMethodsHaveLastTask = false,
                                            ensureMethodsHaveAtMostTwoTasks = false,
                                            compileUselessAbstractTasks = false,
                                            liftedReachability = true, groundedReachability = Some(PlanningGraphWithMutexes),
                                            groundedTaskDecompositionGraph = Some(TwoWayTDG),
                                            iterateReachabilityAnalysis = false, groundDomain = true, stopDirectlyAfterGrounding = false)

  val lifted = PreprocessingConfiguration(compileNegativePreconditions = true, compileUnitMethods = false,
                                          compileOrderInMethods = None,
                                          compileInitialPlan = false, removeUnnecessaryPredicates = false, removeNoOps = false,
                                          convertToSASP = false, allowSASPFromStrips = false, splitIndependentParameters = false,
                                          ensureMethodsHaveLastTask = false,
                                          ensureMethodsHaveAtMostTwoTasks = false,
                                          compileUselessAbstractTasks = false,
                                          liftedReachability = true, groundedReachability = Some(PlanningGraphWithMutexes),
                                          groundedTaskDecompositionGraph = Some(TwoWayTDG),
                                          iterateReachabilityAnalysis = false, groundDomain = false, stopDirectlyAfterGrounding = false)


  val algos =
    ("symbolic-BFS", grounded, PlanBasedSearch(None, BFSType, Nil, Nil, LCFR, efficientSearch = false)) ::
      ("symbolic-DFS", grounded, PlanBasedSearch(None, DFSType, Nil, Nil, LCFR, efficientSearch = false)) ::
      ("BFS", grounded, PlanBasedSearch(None, BFSType, Nil, Nil, LCFR)) ::
      ("DFS", grounded, PlanBasedSearch(None, DFSType, Nil, Nil, LCFR)) ::
      ("PR", grounded, PlanBasedSearch(None, AStarDepthType(2), LiftedTDGPreconditionRelaxation(NeverRecompute) :: Nil, Nil, LCFR)) ::
      ("PR-Recompute-Reach", grounded, PlanBasedSearch(None, AStarDepthType(2), LiftedTDGPreconditionRelaxation(ReachabilityRecompute) :: Nil, Nil, LCFR)) ::
      ("PR-Recompute-CL", grounded, PlanBasedSearch(None, AStarDepthType(2), LiftedTDGPreconditionRelaxation(CausalLinkRecompute) :: Nil, Nil, LCFR)) ::
      ("ActionCount", grounded, PlanBasedSearch(None, AStarActionsType(2), LiftedTDGMinimumAction(NeverRecompute) :: Nil, Nil, LCFR)) ::
      ("ActionCount-Recompute-Reach", grounded, PlanBasedSearch(None, AStarActionsType(2), LiftedTDGMinimumAction(ReachabilityRecompute) :: Nil, Nil, LCFR)) ::
      ("ActionCount-Recompute-CL", grounded, PlanBasedSearch(None, AStarActionsType(2), LiftedTDGMinimumAction(CausalLinkRecompute) :: Nil, Nil, LCFR)) ::
      ("TDGADD", grounded, PlanBasedSearch(None, AStarActionsType(2), LiftedTDGMinimumADD(NeverRecompute) :: Nil, Nil, LCFR)) ::
      ("TDGADD-Recompute-Reach", grounded, PlanBasedSearch(None, AStarActionsType(2), LiftedTDGMinimumADD(ReachabilityRecompute) :: Nil, Nil, LCFR)) ::
      ("TDGADD-Recompute-CL", grounded, PlanBasedSearch(None, AStarActionsType(2), LiftedTDGMinimumADD(CausalLinkRecompute) :: Nil, Nil, LCFR)) ::
      ("lifted-DFS", lifted, PlanBasedSearch(None, DFSType, Nil, Nil, LCFR)) ::
      ("lifted-PR", lifted, PlanBasedSearch(None, AStarDepthType(2), TDGPreconditionRelaxation() :: Nil, Nil, LCFR)) ::
      ("lifted-MMESCC", lifted, PlanBasedSearch(None, AStarDepthType(2), TDGMinimumModificationWithCycleDetection() :: Nil, Nil, LCFR)) ::
      ("lifted-ADD", lifted, PlanBasedSearch(None, AStarDepthType(2), TDGMinimumADD() :: Nil, Nil, LCFR)) ::
      ("lifted-ActionCount", lifted, PlanBasedSearch(None, AStarDepthType(2), TDGMinimumAction() :: Nil, Nil, LCFR)) ::
      ("PRO-RCG", grounded, ProgressionSearch( AStarActionsType(1),
                                              Some(RelaxedCompositionGraph(useTDReachability = true, producerSelectionStrategy = gphRcFFMulticount.producerSelection.firstCome,
                                                                           heuristicExtraction = gphRcFFMulticount.heuristicExtraction.ff)),
                                              abstractTaskSelectionStrategy = PriorityQueueSearch.abstractTaskSelection.decompDepth)) ::
      //("PRO-cRPGHTN", grounded, ProgressionSearch( AStarActionsType(1),CompositionRPGHTN))) ::
      //("PRO-greedyProgression", grounded, ProgressionSearch( AStarActionsType(1),GreedyProgression))) ::
      Nil


  val instances =
    ("Smartphone-VeryVerySmall",
      "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/SmartPhone-HierarchicalNoAxioms.xml",
      "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/OrganizeMeeting_VeryVerySmall.xml") ::
      ("Woodworking-01",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/woodworking-legal-fewer-htn-groundings.xml",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/01--p01-complete.xml") ::
      ("SAT-A",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/satellite2.xml",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/sat-A.xml") ::
      ("SAT-2-2-2",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/satellite2.xml",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/satellite2-P-abstract-2obs-2sat-2mod.xml") ::
      ("UMTranslog-P-1-AirplanesHub",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/UMTranslog.xml",
        "src/test/resources/de/uniulm/ki/panda3/symbolic/parser/xml/UMTranslog-P-1-AirplanesHub.xml") ::
      Nil


  algos foreach { case (algoText, preprocess, search) =>
    instances.zipWithIndex foreach { case ((instanceText, domain, problem), i) =>
      if (i == 0) {algoText must "deliver correct results on " + instanceText } else { it must "deliver correct results on " + instanceText } in {
        val searchConfig = PlanningConfiguration(printGeneralInformation = true, printAdditionalData = true, randomSeed = 42, timeLimit = Some(60 * 60),
                                                 ParsingConfiguration(eliminateEquality = false, stripHybrid = false),
                                                 preprocess,
                                                 search,
                                                 postprocessing)


        val results: ResultMap = searchConfig.runResultSearch(new FileInputStream(domain),
                                                              new FileInputStream(problem))

        assert(results(SearchStatus) == SearchState.SOLUTION)
      }
    }
  }
}
