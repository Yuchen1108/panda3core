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

package de.uniulm.ki.panda3.symbolic.parser.oldpddl

import java.io.{File, FileInputStream, InputStream}

import de.uniulm.ki.panda3.symbolic.domain.Domain
import de.uniulm.ki.panda3.symbolic.parser.Parser
import de.uniulm.ki.panda3.symbolic.parser.hddl.HDDLParser
import de.uniulm.ki.panda3.symbolic.plan.Plan
import de.uniulm.ki.panda3.translation.TypeConverter

/**
  * @author Gregor Behnke (gregor.behnke@uni-ulm.de)
  */
object OldPDDLParser extends Parser {

  override def parseDomainAndProblem(domainFile: InputStream, problemFile: InputStream): (Domain, Plan) = {
    // first we translate Rons format to Daniel's and then we use the HDDL parser
    val domainString = scala.io.Source.fromInputStream(domainFile).mkString
    val problemString = scala.io.Source.fromInputStream(problemFile).mkString

    // the translator actually writes the files, so create targets
    val translatedDomain = File.createTempFile("domain", ".pddl")
    val translatedProblem = File.createTempFile("problem", ".pddl")

    //OldPDDLConverter.processDomain(translatedDomain.getAbsolutePath, domainString)
    //OldPDDLConverter.processProblem(translatedProblem.getAbsolutePath, problemString)

    TypeConverter.`type`(domainString, problemString, translatedDomain.getAbsolutePath, translatedProblem.getAbsolutePath)

    val dom = scala.io.Source.fromFile(translatedDomain.getAbsolutePath).mkString
    val prob = scala.io.Source.fromFile(translatedProblem.getAbsolutePath).mkString

    //println(dom)
    //println("=============")
    //println(prob)

    HDDLParser.parseDomainAndProblem(new FileInputStream(translatedDomain),new FileInputStream(translatedProblem))
  }
}
