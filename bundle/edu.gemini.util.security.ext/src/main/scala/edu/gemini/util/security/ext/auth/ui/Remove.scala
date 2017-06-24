package edu.gemini.util.security.ext.auth.ui

import scalaz._
import Scalaz._
import swing.{Component, Dialog}
import java.util.logging.{Logger, Level}
import edu.gemini.util.security.auth.keychain.KeyChain

trait Remove // just so we have something to hang the logger on

object Remove {
  private lazy val Log = Logger.getLogger(classOf[Remove].getName)

  def remove(ac:KeyChain, parent:Component):Boolean = {

    if (Dialog.showConfirmation(parent,
      "This action will remove your keychain's password.\n\n" +
        "Continue?",
      "Confirm Password Removal", Dialog.Options.YesNo, Dialog.Message.Question, null) == Dialog.Result.Ok) {

      PasswordDialog.removePassword(ac, parent)

      true

    } else false

  }

}
