package edu.gemini.util.security.ext.auth.ui

import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._
import scala.swing._
import event.{ValueChanged, ButtonClicked}
import java.awt
import awt.Color
import javax.swing
import swing.{JPasswordField, BorderFactory, JPopupMenu, ImageIcon}
import java.util.logging.{Level, Logger}

object PasswordDialog {
  private lazy val Log = Logger.getLogger(classOf[PasswordDialog].getName)

  sealed trait Mode
  case object Unlock extends Mode
  case object RemovePassword extends Mode

  def unlock(ac: KeyChain, parent: UIElement = null) = new PasswordDialog(ac, Unlock).open(parent)
  def removePassword(ac: KeyChain, parent: UIElement = null) = new PasswordDialog(ac, RemovePassword).open(parent)

}

import PasswordDialog._

class PasswordDialog(ac:KeyChain, mode:Mode) extends Dialog with CloseOnEsc { dialog =>
  var parent: UIElement = null


  // Config
  title = "Check Password"
  contents = Contents
  pack()

  // Open relative to parent
  def open(p: UIElement) {
    modal = true
    resizable = false
    parent = p
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
    def instructions = "Please enter your keychain password\n" +
      "or select an advanced option.";
  }

  // Our body
  object Body extends GridBagPanel with Rows {

    addRow(new Label("Password:"), pass)

    lazy val pass = new TextField {
      override lazy val peer = new JPasswordField
      def valid = text.length > 0
      val defaultFG = foreground
      reactions += {
        case c:ValueChanged => foreground = if (valid) defaultFG else Color.RED
        case e => println(e)
      }
    }

  }

  // Our footer
  object Footer extends BorderPanel {
    add(new FlowPanel {

      contents += new Button("Ok") {

        dialog.peer.getRootPane.setDefaultButton(peer)

        val title = "Incorrect Password"
        val stdMsg =
          "The specified password is incorrect.\n\n" +
          "If you have forgotten your password, you can start over with an empty\n" +
          "keychain by selecting Advanced > Reset Keychain."

        enabled = false

        def enable() = enabled = Body.pass.valid

        Body.pass.reactions += {
          case _:ValueChanged => enable()
        }

        def good(a:Any) {
          if (mode == RemovePassword)
            Dialog.showMessage(dialog.Contents, "Password has been removed.", "Password Removed", Dialog.Message.Info, null)
          dialog.close()
        }

        def bad(e:Throwable) = Dialog.showMessage(dialog.Contents, stdMsg, title, Dialog.Message.Error, null)

        reactions += {
          case ButtonClicked(_) =>
            mode match {
              case Unlock => ac.unlock(Body.pass.text).unsafeRun.fold(bad, good)
              case RemovePassword =>
                val z = ac.removeLock(Body.pass.text).unsafeRun
                z.fold(bad, good)
            }
        }

      }

      contents += new Button("Cancel") {
        reactions += {
          case ButtonClicked(_) => dialog.close()
        }
      }

    }, BorderPanel.Position.East)

    add(new Button("Advanced") {

      icon = new ImageIcon(dialog.getClass.getResource("icons/triangle.png"))
      opaque = false
      border = null
      margin = new Insets(0,0,0,0 )
      border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
      focusable = false
      peer.setContentAreaFilled(false)
      peer.setBorderPainted(false)


      reactions += {
        case ButtonClicked(_) =>

          val pop = new JPopupMenu()
          pop.add(new MenuItem(Action("Change Password...") {
              dialog.close()
              ChangePasswordDialog.open(ac, parent)
            }).peer)
          pop.add(new MenuItem(Action("Reset Keychain...") {
            if (Reset.reset(ac, dialog.Contents))
              close()
          }).peer)
          pop.show(this.peer, 0, 0)

      }
    }, BorderPanel.Position.West)

  }


}
