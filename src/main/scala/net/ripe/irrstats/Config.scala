package net.ripe.irrstats

import java.io.File

import net.ripe.irrstats.Main._
import org.joda.time.DateTime

object Config {

  private val ApplicationName = "rpki-irr-bgp-stats"
  private val ApplicationVersion = "0.1"

  private val cliOptionParser = new scopt.OptionParser[Config](ApplicationName) {
    head(ApplicationName, ApplicationVersion)

    opt[File]('b', "bgp-announcements") required() valueName("<file>") action { (x, c) =>
      c.copy(risDumpFile = x) } text { "Location of a RIS Dump style file with BGP announcements " }
    opt[File]('s', "extended-delegated-stats") required() valueName("<file>") action { (x, c) =>
      c.copy(statsFile = x) } text { "Location of a copy of the NRO extended delegated stats" }
    opt[File]('r', "route-authorisations") required() valueName("<file>") action { (x, c) =>
      c.copy(routeAuthorisationFile = x) } text { "Location of a file with either ROA export from the RIPE NCC RPKI Validator (.csv) or route[6] objects (.txt)" }
    opt[Unit]('q', "quiet") optional() action { (x, c) => c.copy(quiet = true) } text { "Quiet output, just (real) numbers" }
    opt[String]('d', "date") optional() action { (x, c) => c.copy(date = x) } text { "Override date string, defaults to today"}
    opt[String]('r', "rir") optional() action { (x, c) => c.copy(rir = x) } text { "Only show results for specified rir, defaults to all"}
    opt[String]('x', "country-details")  optional() action { (x, c) => c.copy(countries = true, countryDetails = Some(x)) } text { "Do a detailed announcement report for country code" }

    opt[Unit]('c', "countries") optional() action { (x, c) => c.copy(countries = true) } text { "Do a report per country instead of per RIR" }
    opt[Unit]('w', "worldmap") optional() action { (x, c) => c.copy(worldmap = true, countries = true) } text { "Produce an HTML page with country stats projected on a number of world maps" }

    opt[Unit]('a', "asn") optional() action { (x, c) => c.copy(asn = true) } text { "Find and report top ASNs" }

    checkConfig { c =>
      if (!c.routeAuthorisationFile.getName.endsWith(".csv") && !c.routeAuthorisationFile.getName.endsWith(".txt") ) failure("option -r must refer roas.csv or route[6].db file") else success }

  }

  def config(args: Array[String]) = cliOptionParser.parse(args, Config()).getOrElse { sys.exit(1) }

}

case class Config(risDumpFile: File = new File("."),
                  statsFile: File = new File("."),
                  routeAuthorisationFile: File = new File("."),
                  quiet: Boolean = false,
                  date: String = DateTime.now().toString("YYYYMMdd"),
                  rir: String = "all",
                  countries: Boolean = false,
                  countryDetails: Option[String] = None,
                  worldmap: Boolean = false,
                  asn: Boolean = false) {

  def routeAuthorisationType(): RouteAuthorisationDumpType = {
    if (routeAuthorisationFile.getName.endsWith(".csv")) {
      RoaCsvDump
    } else {
      RouteObjectDbDump
    }
  }
}

sealed trait RouteAuthorisationDumpType
case object RoaCsvDump extends RouteAuthorisationDumpType
case object RouteObjectDbDump extends RouteAuthorisationDumpType

