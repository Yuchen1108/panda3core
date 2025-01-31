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

package de.uniulm.ki.panda3.symbolic.search

import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.symbolic.plan.modification.Modification
import de.uniulm.ki.util.DotPrintable

import scala.concurrent.Promise
import scala.util.Success

/**
  * Represents a search state in a search space. This structure will (usually, i.e., we will probably never have loops do the systematicity) be a tree.
  *
  * @param nodeId     the plan contained in this search node, if it has no flaws this is a solution
  * @param nodePlan   the nodes parent node
  * @param nodeParent the computed heuristic of this node. This might be -1 if the search procedure does not use a heuristic
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
class SearchNode(nodeId: Int, nodePlan: Unit => Plan, nodeParent: SearchNode, nodeHeuristic: Double) extends DotPrintable[Unit] {

  val id: Int = nodeId
  lazy val plan: Plan = nodePlan()
  val parent   : SearchNode = nodeParent
  val heuristic: Double     = nodeHeuristic

  /** if this flag is true only the current plan, the heuristic and its parent are valid! Do not read any other information */
  var dirty: Boolean = true

  private var determinedSearchState = SearchState.INSEARCH

  def searchState: SearchState = determinedSearchState


  /** returns the current state of this search node */
  def recomputeSearchState(): Unit = if (determinedSearchState == SearchState.INSEARCH) {
    determinedSearchState = if (dirty) SearchState.INSEARCH
    else if (children.isEmpty) {
      if (plan.flaws.isEmpty) SearchState.SOLUTION
      else SearchState.DEADEND_UNRESOLVABLEFLAW // it has a flaw but no children ...
    } else {
      children foreach { _._1.recomputeSearchState() }
      // determine state recursively (this is a little bit inefficient)
      if (children exists { _._1.searchState == SearchState.SOLUTION })
        SearchState.SOLUTION
      else if (!(children exists { ch => ch._1.searchState == SearchState.EXPLORED || ch._1.searchState == SearchState.INSEARCH }))
        SearchState.UNSOLVABLE
      else
        SearchState.EXPLORED
    }
  }

  def setSearchState(newSearchState: SearchState): Unit = determinedSearchState = newSearchState


  def setSelectedFlaw(flaw: Int): Unit = { memorySelectedFlaw = Some(flaw) }

  def setSelectedFlaw(flaw: () => Int): Unit = { promiseSelectedFlaw = Some(flaw) }

  def setModifications(mods: Seq[Seq[Modification]]): Unit = { memoryModifications = Some(mods) }

  def setModifications(mods: () => Seq[Seq[Modification]]): Unit = { promiseModifications = Some(mods) }

  def setChildren(children: Seq[(SearchNode, Int)]): Unit = { memoryChildren = Some(children) }

  def setChildren(children: () => Seq[(SearchNode, Int)]): Unit = { promiseChildren = Some(children) }

  def setPayload(payload: Any): Unit = { memoryPayload = Some(payload) }

  def setPayload(payload: () => Any): Unit = { promisePayload = Some(payload) }


  private var promiseSelectedFlaw : Option[() => Int]                    = None
  private var promiseModifications: Option[() => Seq[Seq[Modification]]] = None
  private var promiseChildren     : Option[() => Seq[(SearchNode, Int)]] = None
  private var promisePayload      : Option[() => Any]                    = None

  private var memorySelectedFlaw : Option[Int]                    = None
  private var memoryModifications: Option[Seq[Seq[Modification]]] = None
  private var memoryChildren     : Option[Seq[(SearchNode, Int)]] = None
  private var memoryPayload      : Option[Any]                    = None


  /** the flaw selected for refinement */
  def selectedFlaw: Int = {
    assert(!dirty)
    if (memorySelectedFlaw.isEmpty) {
      memorySelectedFlaw = promiseSelectedFlaw match {
        case Some(x) => assert(!dirty); Some(x())
        case _       => None
      }
    }
    memorySelectedFlaw getOrElse -1
  }

  /** the possible modifications for all flaws. The i-th list of modifications will be the list of possible resolvantes for the i-the flaw in the plan's flaw list. If one of the lists is
    * empty this is a dead-end node in the search space. */
  def modifications: Seq[Seq[Modification]] = {
    if (memoryModifications.isEmpty) {
      memoryModifications = promiseModifications match {
        case Some(x) => assert(!dirty); Some(x())
        case _       => None
      }
    }
    memoryModifications getOrElse Nil
  }

  /** the successors based on the list of modifications. The pair (sn,i) indicates that the child sn was generated based on the modification modifications(selectedFlaw)(i) */
  def children: Seq[(SearchNode, Int)] = {
    if (memoryChildren.isEmpty) {
      memoryChildren = promiseChildren match {
        case Some(x) => assert(!dirty); Some(x())
        case _       => None
      }
    }
    memoryChildren getOrElse Nil
  }

  /** any possible further payload */
  def payload: Any = {
    if (memoryPayload.isEmpty) {
      memoryPayload = promisePayload match {
        case Some(x) => assert(!dirty); Some(x())
        case _       => None
      }
    }
    memoryPayload.orNull
  }

  override lazy val dotString: String = dotString(())

  /** The DOT representation of the object with options */
  override def dotString(options: Unit): String = {
    val builder = new StringBuilder
    builder append "digraph somePlan{\n"

    // enumerate by dfs
    def dfs(currentNode: SearchNode): Unit = currentNode.children foreach { case (child, position) =>
      builder append "\tn" + currentNode.id + " -> t" + child.id + ";\n"
      dfs(child)
    }

    dfs(this)

    builder append "}"
    // retrun the graph
    builder.toString
  }
}
