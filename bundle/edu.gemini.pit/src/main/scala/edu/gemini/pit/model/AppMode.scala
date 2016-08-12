package edu.gemini.pit.model

import java.util.logging.Logger

import scala.util.Try

object AppMode {
  private val Log = Logger.getLogger(AppMode.getClass.getName)

  lazy val isTAC  = isDefinedAndTrue("edu.gemini.pit.tac")
  lazy val isTest = isDefinedAndTrue("edu.gemini.pit.test")

  private def isDefinedAndTrue(prop: String): Boolean = Try {
    System.getProperty(prop).toBoolean
  } getOrElse {
    Log.warning(s"System property $prop should be defined and have a boolean value. Using false as default.")
    false
  }
}
