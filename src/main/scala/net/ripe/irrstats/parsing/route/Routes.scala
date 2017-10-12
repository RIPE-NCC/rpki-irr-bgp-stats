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
package net.ripe.irrstats.parsing.route

import java.io.File

import net.ripe.ipresource.{Asn, IpRange}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.io.Source


object RouteParser {

  def parse(routeFile: File) = {
    
    var routes = List.empty[Route]

    var prefix: IpRange = null
    var asn: Asn = null
    var lastModified: DateTime = null
    
    val keyValueRegex = """^([\w\-]+):\s*(\S*).*$""".r
    
    for (l <- Source.fromFile(routeFile, "iso-8859-1").getLines) {
      
      l match {
          case keyValueRegex(key, value) => {            
            key match {
              case "route" | "route6" => {
                if (asn != null && prefix != null) {
                  routes = routes :+ Route(prefix, asn, lastModified)
                  prefix = IpRange.parse(value)
                  asn = null
                  lastModified = null
                } else {
                  prefix = IpRange.parse(value)
                }
              }
              case "origin" => asn = Asn.parse(value)
              case "last-modified" => lastModified = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z").withZoneUTC().parseDateTime(value)
              case _ => // ignore other
            }
          }
          case _ => // nothing to do
        }
    }
    
    routes = routes :+ Route(prefix, asn, lastModified)
    
    routes
  }  
}


case class Route(prefix: IpRange, asn: Asn, lastModified: DateTime)