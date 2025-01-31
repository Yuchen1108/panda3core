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

package de.uniulm.ki.panda3.symbolic.logic

import de.uniulm.ki.panda3.symbolic.DefaultLongInfo
import de.uniulm.ki.panda3.symbolic.csp.VariableConstraint
import de.uniulm.ki.panda3.symbolic.domain.ActionCost

//import de.uniulm.ki.panda3.symbolic.domain.updates.{ReduceFormula, DomainUpdate}

import de.uniulm.ki.panda3.symbolic.domain.updates.{ExchangeVariable, ReduceFormula, DomainUpdate}
import de.uniulm.ki.util.{Internable, HashMemo}

/**
  *
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
trait LogicalConnector extends Formula {

}

trait Quantor extends Formula {

}

// TODO: include more and better reductions

// TODO: implement better info strings
case class Not(formula: Formula) extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Formula =
    formula.update(domainUpdate) match {
      case Literal(predicate, isPositive, parameters) => Literal(predicate, !isPositive, parameters)
      case Not(sub)                                   => sub // eliminate double negation
      case f                                          => Not(f)
    }

  lazy val containedVariables: Set[Variable] = formula.containedVariables

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = formula.containedPredicatesWithSign map { case (a, b, c) => (a, b, !c) }

  override def longInfo: String = "!" + formula.longInfo

  override val isEmpty: Boolean = formula.isEmpty

  def compileQuantors(): (Formula, Seq[Variable]) = {
    val (inner, vars) = formula.compileQuantors()

    (Not(inner), vars)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val (f, ac) = formula.splitFormulaAndCostFunction()
    assert(ac.isEmpty)
    (f, Nil)
  }
}

/*
case class And[SubFormulas <: Formula](conjuncts: Seq[SubFormulas]) extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): And[Formula] = {
    val subreduced = conjuncts map {
      _ update domainUpdate
    }
    val flattenedSubs = subreduced flatMap {
      case And(sub) => sub
      case f => f :: Nil
    }
    And[Formula](flattenedSubs)
  }


  }*/

case class And[SubFormulas <: Formula](conjuncts: Seq[SubFormulas]) extends LogicalConnector with DefaultLongInfo with HashMemo {
  assert(!(conjuncts contains null))

  override def update(domainUpdate: DomainUpdate): Formula = {
    val subreduced = conjuncts map {
      _ update domainUpdate
    }

    domainUpdate match {
      case ReduceFormula() =>
        val identitiesRemoved = subreduced filter {
          case Identity() => false
          case _          => true
        }

        val flattenedSubs = identitiesRemoved flatMap {
          case And(sub) => sub
          case f        => f :: Nil
        }
        And.intern(flattenedSubs)
      case _               => And.intern(subreduced)
    }
  }

  lazy val containedVariables: Set[Variable] = conjuncts.flatMap(_.containedVariables).toSet

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = conjuncts flatMap { case f: Formula => f.containedPredicatesWithSign } toSet

  override def longInfo: String = (conjuncts map { _.longInfo }).mkString("\n")

  override val isEmpty: Boolean = conjuncts forall { _.isEmpty }

  lazy val containsOnlyLiterals = conjuncts forall { case l: Literal => true; case _ => false }

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    val (newconj, newvars) = conjuncts map { _.compileQuantors() } unzip

    (And.intern(newconj), newvars flatten)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: Seq[(Formula, Seq[ActionCost])] = conjuncts map { _.splitFormulaAndCostFunction() }
    (And(x.map(_._1) filterNot  {case Identity() => true; case _ => false}), x.flatMap(_._2))
  }

}

object And extends Internable[Seq[Formula], And[Formula]] {
  override protected val applyTuple: Seq[Formula] => And[Formula] = { f => And.apply(f) }
}

case class Identity[SubFormulas <: Formula]() extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Identity[Formula] = new Identity[Formula]()

  lazy val containedVariables: Set[Variable] = Set[Variable]()

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = Set[(Predicate, Seq[Variable], Boolean)]()

  override def longInfo: String = "Identity"

  override val isEmpty: Boolean = true

  override def compileQuantors(): (Formula, Seq[Variable]) = (this, Nil)

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = (this,Nil)
}

case class Or[SubFormulas <: Formula](disjuncts: Seq[SubFormulas]) extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Or[Formula] = Or[Formula](disjuncts map {
    _ update domainUpdate
  })

  lazy val containedVariables: Set[Variable] = disjuncts.flatMap(_.containedVariables).toSet

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = disjuncts flatMap { case f: Formula => f.containedPredicatesWithSign } toSet

  override def longInfo: String = (disjuncts map {
    _.longInfo
  }).mkString("\n")

  override val isEmpty: Boolean = disjuncts forall {
    _.isEmpty
  }

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    val (newdis, newvars) = disjuncts map { _.compileQuantors() } unzip

    (Or(newdis), newvars flatten)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: Seq[(Formula, Seq[ActionCost])] = disjuncts map { _.splitFormulaAndCostFunction() }
    assert(x.forall(_._2.isEmpty))
    (Or(x.map(_._1)), Nil)
  }
}

case class Implies(left: Formula, right: Formula) extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Implies = Implies(left.update(domainUpdate), right.update(domainUpdate))

  lazy val containedVariables: Set[Variable] = left.containedVariables ++ right.containedVariables

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = left.containedPredicatesWithSign ++ right.containedPredicatesWithSign

  override def longInfo: String = left.longInfo + " => " + right.longInfo

  override val isEmpty: Boolean = left.isEmpty && right.isEmpty

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    val (lForm, lVars) = left.compileQuantors()
    val (rForm, rVars) = right.compileQuantors()

    (Implies(lForm, rForm), lVars ++ rVars)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: (Formula, Seq[ActionCost]) = left.splitFormulaAndCostFunction()
    val y: (Formula, Seq[ActionCost]) = right.splitFormulaAndCostFunction()
    assert(x._2.isEmpty)
    assert(y._2.isEmpty)
    (Implies(x._1,y._1), Nil)
  }
}

case class When(left: Formula, right: Formula) extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): When = When(left.update(domainUpdate), right.update(domainUpdate))

  lazy val containedVariables: Set[Variable] = left.containedVariables ++ right.containedVariables

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = left.containedPredicatesWithSign ++ right.containedPredicatesWithSign

  override def longInfo: String = left.longInfo + " ~> " + right.longInfo

  override val isEmpty: Boolean = left.isEmpty && right.isEmpty

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    val (lForm, lVars) = left.compileQuantors()
    val (rForm, rVars) = right.compileQuantors()

    (Implies(lForm, rForm), lVars ++ rVars)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: (Formula, Seq[ActionCost]) = left.splitFormulaAndCostFunction()
    val y: (Formula, Seq[ActionCost]) = right.splitFormulaAndCostFunction()
    assert(x._2.isEmpty)
    assert(y._2.isEmpty)
    (When(x._1,y._1), Nil)
  }

}

case class Equivalence(left: Formula, right: Formula) extends LogicalConnector with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Equivalence = Equivalence(left.update(domainUpdate), right.update(domainUpdate))

  lazy val containedVariables: Set[Variable] = left.containedVariables ++ right.containedVariables

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = left.containedPredicatesWithSign ++ right.containedPredicatesWithSign

  override def longInfo: String = left.longInfo + " == " + right.longInfo

  override val isEmpty: Boolean = left.isEmpty && right.isEmpty

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    val (lForm, lVars) = left.compileQuantors()
    val (rForm, rVars) = right.compileQuantors()

    (Equivalence(lForm, rForm), lVars ++ rVars)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: (Formula, Seq[ActionCost]) = left.splitFormulaAndCostFunction()
    val y: (Formula, Seq[ActionCost]) = right.splitFormulaAndCostFunction()
    assert(x._2.isEmpty)
    assert(y._2.isEmpty)
    (Equivalence(x._1,y._1), Nil)
  }
}

case class Exists(v: Variable, formula: Formula) extends Quantor with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Exists = Exists(v.update(domainUpdate), formula.update(domainUpdate))

  lazy val containedVariables: Set[Variable] = formula.containedVariables - v

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = formula.containedPredicatesWithSign

  override def longInfo: String = "exists " + v.shortInfo + " in (" + formula.longInfo + ")"

  override val isEmpty: Boolean = formula.isEmpty

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    val (innerFormula, innerVars) = formula.compileQuantors()

    // create for instance for the quantifier
    val newVar = v.copy(name = v.name + "_compiled_" + Variable.nextFreeVariableID())

    (innerFormula update ExchangeVariable(v, newVar), innerVars :+ newVar)
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: (Formula, Seq[ActionCost]) = formula.splitFormulaAndCostFunction()
    assert(x._2.isEmpty)
    (Exists(v,formula), Nil)
  }
}

object Exists {
  def apply(vs: Seq[Variable], f: Formula): Formula = {
    if (vs.isEmpty) {f } else {Exists(vs.head, Exists(vs.tail, f)) }
  }
}

case class Forall(v: Variable, formula: Formula) extends Quantor with DefaultLongInfo with HashMemo {
  override def update(domainUpdate: DomainUpdate): Forall = Forall(v.update(domainUpdate), formula.update(domainUpdate))

  lazy val containedVariables: Set[Variable] = formula.containedVariables - v

  lazy val containedPredicatesWithSign: Set[(Predicate, Seq[Variable], Boolean)] = formula.containedPredicatesWithSign

  override def longInfo: String = "forall " + v.shortInfo + " in (" + formula.longInfo + ")"

  override val isEmpty: Boolean = formula.isEmpty

  override def compileQuantors(): (Formula, Seq[Variable]) = {
    // create for instance for the quantifier
    val newForlumaeAndVars = v.sort.elements map { c =>
      val (innerFormula, innerVars) = formula.compileQuantors()
      val newSort = Sort(v.sort.name, c :: Nil, Nil)
      val newVar = v.copy(name = v.name + "_compiled_" + Variable.nextFreeVariableID() + c.name, sort = newSort)

      (innerFormula update ExchangeVariable(v, newVar), innerVars :+ newVar)
    }

    (And(newForlumaeAndVars map { _._1 }), newForlumaeAndVars flatMap { _._2 })
  }

  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = {
    val x: (Formula, Seq[ActionCost]) = formula.splitFormulaAndCostFunction()
    assert(x._2.isEmpty)
    (Forall(v,formula), Nil)
  }
}

object Forall {
  def apply(vs: Seq[Variable], f: Formula): Formula = {
    if (vs.isEmpty) {f } else {Forall(vs.head, Forall(vs.tail, f)) }
  }
}


case class ActionCostFormula(cost: ActionCost) extends Formula {
  def splitFormulaAndCostFunction(): (Formula, Seq[ActionCost]) = (Identity(), cost :: Nil)

  //// THESE FUNCTIONS SHOULD NEVER BE CALLED!
  override val isEmpty = true

  override def update(domainUpdate: DomainUpdate) = ???

  override val containedVariables = Set()

  override def containedPredicatesWithSign = ???

  override def compileQuantors() = ???

  /** returns a string by which this object may be referenced */
  override def shortInfo = ???

  /** returns a string that can be utilized to define the object */
  override def mediumInfo = ???

  /** returns a detailed information about the object */
  override def longInfo = ???
}

