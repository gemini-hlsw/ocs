package edu.gemini.util.security.ext.auth.ui

import scalaz._
import Scalaz._
import swing.{Component, Dialog}
import java.util.logging.{Logger, Level}
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._

trait Reset // just so we have something to hang the logger on

object Reset {
  private lazy val Log = Logger.getLogger(classOf[Reset].getName)

  def reset(ac:KeyChain, parent:Component):Boolean = {

    if (Dialog.showConfirmation(parent,
      "This action will reset your keychain to an empty, unlocked state.\n" +
      "Existing keys will be lost. This operation cannot be undone.\n\n" +
      "Continue?",
      "Confirm Reset", Dialog.Options.YesNo, Dialog.Message.Question, null) == Dialog.Result.Ok) {

      ac.reset(None).unsafeRun.fold(
        e => Log.log(Level.SEVERE, "Could not reset keychain.", e),
        _ => ())

      true

    } else false

  }

}
