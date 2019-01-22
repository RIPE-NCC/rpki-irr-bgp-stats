/**
 * Copyright (c) 2015-2107 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of this software, nor the names of its contributors, nor
 *     the names of the contributors' employers may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.irrstats.parsing.certifiedresource

import java.io.File

import net.ripe.ipresource.{IpRange, IpResource, IpResourceSet}

import scala.collection.mutable
import scala.io.Source

// Assume format:
// Resource
// prefix1
// prefix2
// prefix3
// ...
object CertifiedResourceParser {

  def parseMap(certifiedResourceFile: File): Map[String, IpResourceSet] = {

    val result = mutable.Map[String, IpResourceSet]()

    System.err.println("Start parsing Certificates")
    Source
      .fromFile(certifiedResourceFile, "iso-8859-1")
      .getLines.drop(1)
      .foreach(line => {
        val skiResource: Array[String] = line.split("\",\"")
        val ski = skiResource(0).replaceAll("\"","")
        val ranges = skiResource(1).trim.replaceAll("\"","").split(",").map(_.trim)
        ranges.foreach { range =>
          try {
            val resource = IpResourceSet.parse(range)
            if (!result.contains(ski)) {
              result(ski) = new IpResourceSet()
            }
            result(ski).addAll(resource)
          } catch {
            case e => System.err.println(s"Failed parsing $line")
          }
        }

      })
    System.err.println("Done parsing Certificates")
    result.toMap
  }
}
