package edu.gemini.util.security.ext.auth.ui

import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.auth.keychain.{Action => KeyAction}
import scala.swing._
import event.{ValueChanged, ButtonClicked}
import java.awt
import awt.Color
import javax.swing
import swing.{JPasswordField, BorderFactory, JPopupMenu, ImageIcon}
import java.util.logging.Logger
import scalaz._
import Scalaz._

object ChangePasswordDialog {

  def open(ac: KeyChain, parent: UIElement = null) = new ChangePasswordDialog(ac).open(parent)
  private lazy val Log = Logger.getLogger(classOf[ChangePasswordDialog].getName)

}

class ChangePasswordDialog(ac:KeyChain) extends Dialog with CloseOnEsc { dialog =>

  // Config
  title = "Change Password"
  contents = Contents
  pack()

  // Open relative to parent
  def open(parent: UIElement) {
    modal = true
    resizable = false
    Option(parent).foreach(setLocationRelativeTo)
    open()
  }

  // Our main content object
  object Contents extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8);

    // Add our content, defined below
    add(Header, BorderPanel.Position.North)
    add(Body, BorderPanel.Position.Center)
    add(Footer, BorderPanel.Position.South)

  }

  // Our header
  object Header extends Instructions {
    def icon = new ImageIcon(dialog.getClass.getResource("icons/lock_closed.png"))
    def instructions = "Please select a new keychain password."
  }

  // Our body
  object Body extends GridBagPanel with Rows {

    if (ac.hasLock.unsafeRunAndThrow) {
      addRow(new Label("Old Password:"), oldPass)
    }
    addSpacer()
    addSpacer()
    addRow(new Label("New Password:"), newPass1)
    addRow(new Label("Confirm:"), newPass2)

    class PassField(securedOnly: Boolean) extends TextField {
      override lazy val peer = new JPasswordField
      def valid = {
        // If there is no lock, we're valid even if there's no text
        val noLock = !ac.hasLock.unsafeRunAndThrow
        (noLock && securedOnly) || text.length > 0
      }
      val defaultFG = foreground
      reactions += {
        case c:ValueChanged => foreground = if (valid) defaultFG else Color.RED
      }
    }

    lazy val oldPass = new PassField(true)
    lazy val newPass1, newPass2 = new PassField(false)

    lazy val fields = Seq(oldPass, newPass1, newPass2)

  }

  // Our footer
  object Footer extends BorderPanel {
    add(new FlowPanel {

      contents += new Button("Ok") {

        dialog.peer.getRootPane.setDefaultButton(peer)

        enabled = false

        def enable(): Unit =
          enabled = Body.fields.forall(_.valid)

        Body.fields.foreach(_.reactions += {
          case _:ValueChanged => enable()
        })

        def good(a:Any) {
          Dialog.showMessage(dialog.Contents, "Your password has been changed.", "Password Changed", Dialog.Message.Info, null)
          dialog.close()
        }

        def bad(e:Throwable) = {
          val title = "Incorrect Password"
          val stdMsg ="The old password you supplied is incorrect."
          Dialog.showMessage(dialog.Contents, stdMsg, title, Dialog.Message.Error, null)
        }

        reactions += {
          case ButtonClicked(_) =>
            val (old, p1, p2) = (Body.oldPass.text, Body.newPass1.text, Body.newPass2.text)
            if (p1 != p2) {
              Dialog.showMessage(dialog.Contents, "New passwords don't match.", "Error", Dialog.Message.Error, null)
            } else {
              val a: KeyAction[Unit] =
                for {
                  b <- ac.hasLock
                  _ <- b.fold(ac.changeLock(old, p1), ac.setLock(p1))
                } yield ()
              a.unsafeRun.fold(bad, good)
            }
        }

      }

      contents += new Button("Cancel") {
        reactions += {
          case ButtonClicked(_) => dialog.close()
        }
      }

    }, BorderPanel.Position.East)

  }


}
