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

package de.uniulm.ki.panda3.symbolic.parser

import java.io.{FileInputStream, File}

import de.uniulm.ki.panda3.symbolic.parser.hddl.HDDLParser
import org.scalatest.FlatSpec

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
class PDDLParserTest extends FlatSpec {

  val ipc7Domains = new File("src/test/resources/de/uniulm/ki/panda3/symbolic/parser/pddl/IPC7").listFiles() map { d => ("IPC7", d) }
  val ipc6Domains = new File("src/test/resources/de/uniulm/ki/panda3/symbolic/parser/pddl/IPC6").listFiles() map { d => ("IPC6", d) }


  ipc7Domains ++ ipc6Domains foreach { case (ipc, d) =>
    ipc + "domain " + d.getName must "be parsable" in {
      val domainDir = d.listFiles() find { _.getName == "domain" }
      assert(domainDir.isDefined)
      val problemDir = d.listFiles() find { _.getName == "problems" }
      assert(problemDir.isDefined)

      val domainFiles = domainDir.get.listFiles()
      val problemFiles = problemDir.get.listFiles()

      assert(domainFiles.length == 1 || domainFiles.length == problemFiles.length)

      val usableDomainFiles = if (domainFiles.length == 1) List.fill(problemFiles.length)(domainFiles.head) else domainFiles.toList

      problemFiles.sortBy(_.getName) zip usableDomainFiles.sortBy(_.getName) foreach { case (problemFile, domainFile) =>
        println(ipc + " " + d.getPath + " " + problemFile.getName + " & " + domainFile.getName)
        val (domain, plan) = HDDLParser.parseDomainAndProblem(new FileInputStream(domainFile), new FileInputStream(problemFile))

        assert(plan.planSteps.length == 2)
      }
    }
  }
}
