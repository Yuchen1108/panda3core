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

package de.uniulm.ki.panda3.symbolic.plan.element

import de.uniulm.ki.panda3.symbolic.PrettyPrintable
import de.uniulm.ki.panda3.symbolic.csp._
import de.uniulm.ki.panda3.symbolic.domain.updates._
import de.uniulm.ki.panda3.symbolic.domain.{DecompositionMethod, DomainUpdatable, ReducedTask, Task}
import de.uniulm.ki.panda3.symbolic.logic._
import de.uniulm.ki.panda3.symbolic._
import de.uniulm.ki.util.{HashMemo, Internable}

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.collection.mutable

/**
  *
  *
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
case class PlanStep(id: Int, schema: Task, arguments: Seq[Variable]) extends DomainUpdatable with PrettyPrintable {

  arguments foreach { v =>
    assert(v != null, "Plan step " + id + " task " + schema.name + " instantiated with null argument.\n" +
      "Arguments are " + arguments.map({ case null => "null" ; case x => x.name }).mkString(", " + ""))
  }
  assert(arguments.length == schema.parameters.length)


  // TODO: this might cause problems in the wrapper (two decompositon methods might be judged as equal if they really are not), but is necessary to achieve at least a decent performance
  // for the symbolic planner
  override def equals(o: Any): Boolean = o match {
    case step: PlanStep => id == step.id && schema.name == step.schema.name
    case _              => false
  }

  override val hashCode: Int = id + 31 * schema.hashCode()

  lazy val argumentSet = arguments.toSet
  lazy val argumentArray = arguments.toArray

  if (arguments.size != schema.parameters.size) {
    System.out.println("The number of parameters given in a plan step definition does not match the number that was given in the definition of the task schema.")
  }
  assert(arguments.size == schema.parameters.size)
  // TODO: test whether it is a subsort relation instead
  //assert((arguments zip schema.parameters) forall {case (a,b) => a.sort == b.sort})

  private lazy val parameterSubstitution = PartialSubstitution(schema.parameters, arguments)

  /** returns a version of the preconditions */
  lazy val substitutedPreconditions: Seq[Literal] = schema match {
    case reduced: ReducedTask => reduced.precondition.conjuncts map substitute
    case _                    => noSupport(FORUMLASNOTSUPPORTED)
  }
  /** returns a version of the effects */
  lazy val substitutedEffects      : Seq[Literal] = schema match {
    case reduced: ReducedTask => reduced.effect.conjuncts map substitute
    case _                    => noSupport(FORUMLASNOTSUPPORTED)
  }

  lazy val schemaParameterSubstitution = PartialSubstitution(schema.parameters, arguments)
  lazy val parameterSchemaSubstitution = PartialSubstitution(arguments, schema.parameters)

  ///** check whether two literals are identical given a CSP */
  //def =?=(that: PlanStep)(csp: CSP): Boolean = this.schema == that.schema &&
  //  ((this.arguments zip that.arguments) forall { case (v1, v2) => csp.getRepresentative(v1) == csp.getRepresentative(v2) })

  def indexOfPrecondition(l: Literal, csp: CSP): Int = indexOf(l, substitutedPreconditions, csp)

  private def indexOf(l: Literal, ls: Seq[Literal], csp: CSP): Int = (ls.zipWithIndex foldLeft -1) { case (i, (nl, j)) => if ((l =?= nl) (csp)) j else i }

  def indexOfEffect(l: Literal, csp: CSP): Int = indexOf(l, substitutedEffects, csp)

  private def substitute(literal: Literal): Literal = schema.substitute(literal, parameterSubstitution)

  override def update(domainUpdate: DomainUpdate): PlanStep = domainUpdate match {
    case ExchangePlanSteps(exchangeMap) => if (exchangeMap contains this) exchangeMap(this) else this
    case ExchangeTask(exchangeMap)      => if (exchangeMap contains schema) {
      val additionalParameters = exchangeMap(schema).parameters.drop(arguments.length) map { v => v.copy(name = v.name + "_ps" + id) }
      PlanStep.intern((id, exchangeMap(schema), arguments ++ additionalParameters))
    } else this
    case ExchangeVariable(_, _)         => PlanStep.intern((id, schema, arguments map { _.update(domainUpdate) }))
    case ExchangeVariables(_)           => PlanStep.intern((id, schema, arguments map { _.update(domainUpdate) }))
    // propagate irrelevant update to reduce task
    case _ => PlanStep.intern((id, schema.update(domainUpdate), arguments map { _.update(domainUpdate) }))
  }

  /** returns a short information about the object */
  override def shortInfo: String = id + ":" + schema.shortInfo

  /** returns a string that can be utilized to define the object */
  override def mediumInfo: String = shortInfo + (arguments map {
    _.shortInfo
  }).mkString("(", ", ", ")")

  /** returns a more detailed information about the object */
  override def longInfo: String = mediumInfo + "\npreconditions:\n" +
    (substitutedPreconditions map {
      "\t" + _.shortInfo
    }).mkString("\n") + "\neffects:\n" + (substitutedEffects map {
    "\t" + _.shortInfo
  }).mkString("\n")
}

object PlanStep extends Internable[(Int, Task, Seq[Variable]), PlanStep] {
  override protected val applyTuple = (PlanStep.apply _).tupled
}


/**
  * A ground task is basically a planstep without an id.
  */
case class GroundTask(task: Task, arguments: Seq[Constant]) extends HashMemo with Ordered[GroundTask] with PrettyPrintable with DomainUpdatable {

  /*@elidable(ASSERTION)
  def assertion(): Boolean = {
    assert(task.parameters.size == arguments.size, "Incorrect argument number " + task.name + " " + task.parameters.size + " != " + arguments.size)
    task.parameters.zipWithIndex foreach { case (p, i) =>
      if (!p.sort.elements.contains(arguments(i))) {
        println(p.sort.elements)
        println(arguments(i))
      }
      assert(p.sort.elements.contains(arguments(i)), "Task " + task.name + "Argument " + i + " asserted to " + arguments(i) + " but not allowed in sort " + p.sort.name +
        p.sort.elements.map(_.name).mkString("(", ",", ")"))
    }

    // the arguments must be allowed
    assert(task.areParametersAllowed(arguments))
    true
  }
  assert(assertion())*/

  lazy val argumentArray = arguments.toArray

  lazy val parameterSubstitution: TotalSubstitution[Variable, Constant] = TotalSubstitution(task.parameters, arguments)

  lazy val substitutedPreconditions: Seq[GroundLiteral] = task match {
    case reduced: ReducedTask => reduced.precondition.conjuncts map { _ ground parameterSubstitution }
    case _                    => noSupport(FORUMLASNOTSUPPORTED)
  }

  lazy val substitutedPreconditionsSet: Set[GroundLiteral] = substitutedPreconditions.toSet
  lazy val substitutedEffectSet       : Set[GroundLiteral] = substitutedEffects.toSet

  /** returns a version of the effects */
  lazy val substitutedEffects: Seq[GroundLiteral] = task match {
    case reduced: ReducedTask => reduced.effect.conjuncts map { _ ground parameterSubstitution }
    case _                    => noSupport(FORUMLASNOTSUPPORTED)
  }

  lazy val substitutedDelEffects: Seq[GroundLiteral] = substitutedEffects filterNot { _.isPositive }

  lazy val substitutedAddEffects: Seq[GroundLiteral] = substitutedEffects filter { _.isPositive }

  override def compare(that: GroundTask): Int = {
    this.task compare that.task match {
      case 0 => ((this.arguments zip that.arguments) map { case (x, y) => x compare y }) find ((i: Int) => i != 0) getOrElse (0)
      case _ => this.task compare that.task
    }
  }

  override def update(domainUpdate: DomainUpdate): GroundTask = GroundTask(task.update(domainUpdate), arguments map {_.update(domainUpdate)})

  /** returns a string by which this object may be referenced */
  override def shortInfo: String = task.name + (arguments map { _.name }).mkString("(", ",", ")")

  /** returns a string that can be utilized to define the object */
  override def mediumInfo: String = shortInfo

  /** returns a detailed information about the object */
  override def longInfo: String = mediumInfo
}


case class PartiallyInstantiatedTask(task: Task, arguments: Map[Variable, Constant]) extends HashMemo {

  lazy val allInstantiations: Seq[GroundTask] = Sort.allPossibleInstantiationsWithVariables(task.parameters filterNot { arguments.contains } map { v => (v, v.sort.elements) }) map {
    case newlyBoundVariables =>
      val allVariablesMap = arguments ++ newlyBoundVariables
      GroundTask(task, task.parameters map allVariablesMap)
  }
}
