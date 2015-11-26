package edu.gemini.dataman.osgi

import edu.gemini.dataman.DatamanLogger
import edu.gemini.spModel.core.catchingNonFatal

import java.util.logging.Level

import scalaz._
import Scalaz._

sealed trait DatamanCommands {
  def dataman(args: Array[String]): String
}

object DatamanCommands {
  def apply(): DatamanCommands =
    new DatamanCommands {
      val help =
        """
          |Data Manager Help
          |-----------------
          |
          |Grammar:
          |  <level> := SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST
          |
          |Commands:
          |  dataman detail           Shows the current detail logging level
          |  dataman detail <level>   Sets the detail logging level
          |  dataman json             Shows the current json logging level
          |  dataman json <level>     Sets the json logging level
          |
        """.stripMargin

      def showLoggable(l: Level): String =
        s"logging ${DatamanLogger.isLoggable(l) ? "on" | "off"}"

      def showLevel(l: Level): String =
        s"${l.toString} (${showLoggable(l)})"

      def setLevel(name: String, level: String)(f: Level => Unit): String =
        catchingNonFatal {
          Level.parse(level) <| f
        }.fold(
            _ => s"Could not parse $name level '$level'.\n\n$help",
            l => s"Logging $name level set to $level (${showLoggable(l)})"
          )

      override def dataman(args: Array[String]): String =
        args.toList match {
          case List("detail")        => showLevel(edu.gemini.dataman.DetailLevel)
          case List("detail", level) => setLevel("detail", level) { edu.gemini.dataman.DetailLevel_= }
          case List("json")          => showLevel(edu.gemini.dataman.JsonLevel)
          case List("json", level)   => setLevel("json", level) { edu.gemini.dataman.JsonLevel_= }
          case _                     => help
        }
    }
}
