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

package de.uniulm.ki

import java.io.{FileWriter, BufferedWriter, PrintWriter, File}

import scala.collection.mutable

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
package object util {
  def writeStringToFile(s: String, file: String): Unit = writeStringToFile(s, new File(file))

  def writeStringToFile(s: String, file: File): Unit = Some(new BufferedWriter(new PrintWriter(file))).foreach { p => p.write(s); p.close() }


  def allMappings[A, B](listA: Seq[A], listB: Seq[B]): Seq[Seq[(A, B)]] = if (listA.isEmpty || listB.isEmpty) Nil :: Nil
  else {
    val aElem = listA.head
    val remListA = listA.tail
    listB flatMap { bElem =>
      val remListB = listB filter { _ != bElem }
      allMappings(remListA, remListB) map { case l => l :+(aElem, bElem) }
    }
  }

  def crossProduct[A](list: Seq[Seq[A]]): Seq[Seq[A]] = if (list.isEmpty) Nil :: Nil
  else {
    val subList = crossProduct(list.tail)
    list.head flatMap { e => subList map { l => l :+ e } }
  }

  def crossProduct[A](array1: Array[A], array2: Array[A]): Array[(A, A)] = array1 flatMap { p1 => array2 map { p2 => (p1, p2) } }

  def arrayContains[A](array: Array[A], element: A): Boolean = {
    var i = 0
    var found = false
    while (i < array.length && !found) {
      if (array(i) == element) found = true
      i += 1
    }
    found
  }

  def allSubsets[A](seq: Seq[A]): Seq[Seq[A]] = seq.toSet.subsets() map { _.toSeq } toSeq

  def memoise[Input, Output](function: Input => Output): Input => Output = {

    val memoisationMap = new mutable.HashMap[Input, Output]()

    def apply(input: Input): Output = {
      if (memoisationMap contains input) {memoisationMap(input) }
      else {
        val newValue = function(input)
        memoisationMap(input) = newValue
        newValue
      }
    }

    apply
  }
}
