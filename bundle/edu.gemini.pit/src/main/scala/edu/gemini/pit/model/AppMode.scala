package edu.gemini.pit.model

object AppMode {
  lazy val isTest = CurrentVersion.get().isTest
}
