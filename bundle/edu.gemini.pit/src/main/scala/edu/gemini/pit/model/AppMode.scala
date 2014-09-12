package edu.gemini.pit.model

object AppMode {

  lazy val isTAC = isDefined("edu.gemini.pit.tac")
  lazy val isTest = isDefined("edu.gemini.pit.test")

  private def isDefined(prop:String) = Option(System.getProperty(prop)).isDefined

}
