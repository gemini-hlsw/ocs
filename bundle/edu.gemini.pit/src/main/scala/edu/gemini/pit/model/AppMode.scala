package edu.gemini.pit.model

import java.util.logging.Logger

import scala.util.Try

object AppMode {
  private val Log = Logger.getLogger(AppMode.getClass.getName)

  val TestProperty = "edu.gemini.pit.test"
  lazy val isTest = Try {
    System.getProperty(TestProperty).toBoolean
  } getOrElse {
    Log.warning(s"System property $TestProperty should be defined and have a boolean value. Using false as default.")
    false
  }
}
