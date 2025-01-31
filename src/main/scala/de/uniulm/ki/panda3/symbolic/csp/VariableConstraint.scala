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

package de.uniulm.ki.panda3.symbolic.csp

import de.uniulm.ki.panda3.symbolic.domain.DomainUpdatable
import de.uniulm.ki.panda3.symbolic.domain.updates.DomainUpdate
import de.uniulm.ki.panda3.symbolic.logic.{Constant, Sort, Value, Variable}

/**
  * Variable Constraints are symbolic representations of relations between variables.
  * A [[CSP]] can handle constraint networks expressed with relations between variables expressed by VariableConstraints.
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
sealed trait VariableConstraint extends DomainUpdatable {

  /** Returns an equivalent set of constraints, which does not contains [[NotOfSort]] constraints. These will be compiled into [[NotEqual]] constraints. */
  def compileNotOfSort: Set[VariableConstraint] = {
    this match {
      case Equal(_, _) | NotEqual(_, _) | OfSort(_, _) => Set(this)
      case NotOfSort(v, s)                             => s.elements.map(element => NotEqual(v, element)).toSet[VariableConstraint]
    }
  }

  val getVariables: Seq[Variable]

  def containsVariable(v: Variable) : Boolean

  def substitute(sub: PartialSubstitution[Variable]): VariableConstraint

  override def update(domainUpdate: DomainUpdate): VariableConstraint

  def isTautologic: Boolean
}


// the 4 kinds of constraints the CSPs currently support
/**
  * Represents the constraint v_1 = v_2 or v = c, i.e. either forced equality between two variables or a variable and a constant.
  */
case class Equal(left: Variable, right: Value) extends VariableConstraint {

  /** equals respects the equivalence of v_1 = v_2 and v_2 = v_1 */
  override def equals(that: Any): Boolean =
    that match {
      case Equal(thatLeft, thatRight) => (thatLeft == this.left && thatRight == this.right) || (this.left == thatRight && this.right == thatLeft)
      case _                          => false
    }

  override val getVariables = right match {
    case variable: Variable => variable :: left :: Nil
    case _                  => left :: Nil
  }

  def containsVariable(v: Variable) : Boolean = v == left || v == right

  override def substitute(sub: PartialSubstitution[Variable]): VariableConstraint = {
    val newLeft = sub(left)
    right match {
      case v: Variable => Equal(newLeft, sub(v))
      case c: Constant => Equal(newLeft, c)
    }
  }

  override def update(domainUpdate: DomainUpdate): Equal = Equal(left.update(domainUpdate), right.update(domainUpdate))

   val isTautologic: Boolean = left == right
}


/**
  * Represents the constraint v_1 != v_2 or v != c, i.e. either forced un-equality between two variables or a variable and a constant.
  */
case class NotEqual(left: Variable, right: Value) extends VariableConstraint {

  /** equals respects the equivalence of v_1 = v_2 and v_2 = v_1 */
  override def equals(that: Any): Boolean =
    that match {
      case NotEqual(thatLeft, thatRight) => (thatLeft == this.left && thatRight == this.right) || (this.left == thatRight && this.right == thatLeft)
      case _                             => false
    }

  override val getVariables = right match {
    case variable: Variable => variable :: left :: Nil
    case _                  => left :: Nil
  }

  def containsVariable(v: Variable) : Boolean = v == left || v == right

  override def substitute(sub: PartialSubstitution[Variable]): VariableConstraint = {
    val newLeft = sub(left)
    right match {
      case v: Variable => NotEqual(newLeft, sub(v))
      case c: Constant => NotEqual(newLeft, c)
    }
  }

  override def update(domainUpdate: DomainUpdate): NotEqual = NotEqual(left.update(domainUpdate), right.update(domainUpdate))

  lazy val isTautologic: Boolean = right match {
    case c: Constant => !(left.sort.elements contains c)
    case _           => false
  }
}


/**
  * Represents the constraint v_1 element-of S, for some sort S
  */
case class OfSort(left: Variable, right: Sort) extends VariableConstraint {
  override val getVariables = left :: Nil

  def containsVariable(v: Variable) : Boolean = v == left

  override def substitute(sub: PartialSubstitution[Variable]): VariableConstraint = OfSort(sub(left), right)

  override def update(domainUpdate: DomainUpdate): OfSort = OfSort(left.update(domainUpdate), right.update(domainUpdate))

  // TODO interacts with the XML parser
  lazy val isTautologic: Boolean = false // left.sort.elements forall right.elements.contains
}

/**
  * Represents the constraint v_1 not-element-of S, for some sort S
  */
case class NotOfSort(left: Variable, right: Sort) extends VariableConstraint {
  override val getVariables = left :: Nil

  def containsVariable(v: Variable) : Boolean = v == left

  override def substitute(sub: PartialSubstitution[Variable]): VariableConstraint = NotOfSort(sub(left), right)

  override def update(domainUpdate: DomainUpdate): NotOfSort = NotOfSort(left.update(domainUpdate), right.update(domainUpdate))

  // TODO interacts with the XML parser
  lazy val isTautologic: Boolean = false // !(left.sort.elements exists right.elements.contains)
}
