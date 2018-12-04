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
package net.ripe.irrstats

import net.ripe.irrstats.parsing.holdings.{CountryHoldings, EntityHoldings, Holdings, RIRHoldings}
import net.ripe.irrstats.parsing.ris.RisDumpUtil
import net.ripe.irrstats.parsing.roas.RoaUtil
import net.ripe.irrstats.parsing.route.RouteParser
import net.ripe.irrstats.route.validation.RtrPrefix

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = Config.config(args)

  val announcementsF = Future {
    Time.timed {
      RisDumpUtil.parseDumpFile(config.risDumpFile)
    }
  }

  val roasF = Future {
    Time.timed {
      config.routeAuthorisationType() match {
        case RoaCsvDump => RoaUtil.parse(config.routeAuthorisationFile)
        case RouteObjectDbDump => if (config.looseRouteObjectValidation) {
          RouteParser.parse(config.routeAuthorisationFile).par.map(r => RtrPrefix(r.asn, r.prefix, Some(24))).seq
        } else {
          RouteParser.parse(config.routeAuthorisationFile).par.map(r => RtrPrefix(r.asn, r.prefix)).seq
        }
      }
    }
  }


  val (holdingsLines, ht) = Time.timed(Holdings.read(config.statsFile))

  // lazy vals are only initialised when used for the first time,
  // so there is no performance penalty defining all of the following
  val rirHoldingsF = Future(Time.timed(RIRHoldings.parse(holdingsLines)))
  val countryHoldingsF = Future(Time.timed(CountryHoldings.parse(holdingsLines)))
  val entityHoldingsF = Future(Time.timed(EntityHoldings.parse(holdingsLines)))

  val report = for {
    (countryHolding, ct)              <- countryHoldingsF
    (rirHoldings, rt)                 <- rirHoldingsF
    (entityHoldings, et)              <- entityHoldingsF
    (announcements, announcementTime) <- announcementsF
    (authorisations, roaParseTime)    <- roasF
  } yield {
    val (_, reportTime) = Time.timed {
      config.analysisMode match {
        case AsnMode => ReportAsn.report(announcements, authorisations, config.quiet)
        case WorldMapMode => ReportWorldMap.report(announcements, authorisations, countryHolding)
        case NROStatsMode => ReportNROStats.report(announcements, authorisations, countryHolding, rirHoldings)
        case CountryDetailsMode => ReportCountry.reportCountryDetails(announcements, authorisations, countryHolding, config.countryDetails.get)
        case CountryAdoptionMode => ReportCountry.reportCountryAdoption(announcements, authorisations, countryHolding, config.quiet, config.date)
        case CountryMode => ReportCountry.reportCountries(announcements, authorisations, countryHolding, config.quiet, config.date)
        case RirMode => ReportRir.report(announcements, authorisations, rirHoldings, config.quiet, config.date, config.rir)
        case RirAdoptionMode => ReportRir.reportAdoption(announcements, authorisations, rirHoldings, config.quiet, config.date, config.rir)
        case PerEntityMode => ReportRir.report(announcements, authorisations, entityHoldings, config.quiet, config.date, config.rir)
      }
    }
    (announcementTime, roaParseTime, reportTime, ct, rt, et)
  }

  val (announcementTime, roaParseTime, reportTime, ct, rt, et) = Await.result(report, Duration.Inf)
  System.err.println(s"Announcement parse ${announcementTime}ms, roa parse time ${roaParseTime}ms, report generation time ${reportTime}ms")

  System.exit(0) // We're done here

}
