package net.ripe.irrstats.analysis

import java.math.BigInteger

import net.ripe.ipresource.{IpRange, IpResourceSet}
import scala.collection.JavaConverters._

object StatsUtil {

  def getNumberOfAddresses(prefixes: Seq[IpRange]): BigInteger = {
    val resourceSet = new IpResourceSet()
    prefixes.foreach(pfx => resourceSet.addAll(new IpResourceSet(pfx)))
    getNumberOfAddresses(resourceSet)
  }

  def getNumberOfAddresses(resourceSet: IpResourceSet): BigInteger = {
    resourceSet.iterator().asScala.foldLeft(BigInteger.ZERO)((r, c) => {
      r.add(c.getEnd.getValue.subtract(c.getStart.getValue).add(BigInteger.ONE))
    })
  }
}
