package edu.gemini.spModel.io.impl.migration.to2017B

import edu.gemini.spModel.io.PioSyntax._
import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.pio.xml.PioXmlFactory

import edu.gemini.spModel.pio.{Pio, Document, Version}

import scalaz._, Scalaz._

/** 2017B Migration.
 */
object To2017B extends Migration {

  val version = Version.`match`("2017B-1")

  val conversions: List[Document => Unit] =
    List(updateTimeAccounting)

  val fact = new PioXmlFactory

  // Changes the old implicit program award in hours to an explicit program
  // award in milliseconds and adds explicit partner award of 0.
  private def updateTimeAccounting(d: Document): Unit =
    for {
      cont <- d.containers.find(_.getKind == SpIOTags.PROGRAM)
      dobj <- cont.dataObject
      tact <- dobj.paramSet("timeAcct")
      aloc <- tact.paramSets("timeAcctAlloc")
      hrs  <- aloc.double("hours")
    } {
      aloc.removeChild("hours")
      Pio.addLongParam(fact, aloc, "program", (hrs * 3600000).round)
      Pio.addLongParam(fact, aloc, "partner", 0)
    }
}
