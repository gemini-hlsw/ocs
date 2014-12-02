package edu.gemini.spModel.io.impl.migration.to2015B

import java.io.File

import edu.gemini.pot.sp.SPComponentType

import edu.gemini.spModel.pio.xml.PioXmlUtil
import edu.gemini.spModel.pio._

/** Convert to new target model. */
object To2015B {
  import PioSyntax._
  import BrightnessParser._

  // These will be applied in the given order
  val conversions: List[Document => Unit] = List(
    brightnessToMagnitude,
    brightnessToNote
  )

  // Entry point here
  def updateProgram(d: Document): Unit =
    conversions.foreach(_.apply(d))

  // Parse old `brightness` property into magnitudes when possible, then set to empty.
  def brightnessToMagnitude(d: Document): Unit = {
    val names = Set("base", "spTarget")
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      b   <- ps.value("brightness").toList if b.nonEmpty && ps.getParamSet("magnitudeList") == null
      ms  <- parseBrightness(b).toList
    } {
//      println()
//      println("update " + obs.getName + " - " + ps.value("name").getOrElse("(untitled)"))
//      println("  from " + b)
//      println("    to " + ms.list.mkString(", "))



      ps.getParam("brightness").setValue("")
    }
  }


  var seen: Set[String] = Set()

  // Turn `brightness` value into a note, then set to empty.
  def brightnessToNote(d: Document): Unit = {
    val names = Set("base", "spTarget")
    for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets if names(ps.getName)
      b   <- ps.value("brightness").toList if b.nonEmpty && ps.getParamSet("magnitudeList") == null
    } {
      val b0 = b.replaceAll("""\d+(\.\d+)?""", "0").trim
      if (!seen(b0)) {
        seen = seen + b0
        println(seen.size + "  failed: " + b)
      }

//      println()
//      println("add note for " + obs.getName + " - " + ps.value("name").getOrElse("(untitled)"))
//      println("  failed: " + b)
    }
  }

  /// TESTING

  def main(args: Array[String]): Unit = {
    val fs = new File("/Users/rnorris/Scala/ocs-arch/20140922-0730").listFiles.toStream.filter(_.getName.endsWith(".xml"))
    fs.take(600).map(PioXmlUtil.read).map(_.asInstanceOf[Document]).foreach(updateProgram)
  }

}
