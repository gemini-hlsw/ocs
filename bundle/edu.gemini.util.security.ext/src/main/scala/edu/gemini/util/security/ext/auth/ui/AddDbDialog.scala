package edu.gemini.util.security.ext.auth.ui

import edu.gemini.util.security.auth.keychain.KeyChain
import scala.swing._
import event.{ValueChanged, SelectionChanged, ButtonClicked}
import java.awt
import awt.Color
import javax.swing
import javax.swing.ImageIcon
import java.util.UUID
import java.io.{InvalidClassException, IOException}
import java.util.logging.{Level, Logger}
import util.matching.Regex
import edu.gemini.spModel.core.{VersionException, Peer}
import edu.gemini.util.security.auth.keychain.KeyChain

object AddDbDialog {

  private lazy val Log = Logger.getLogger(classOf[AddDbDialog].getName)

  def open(ac: KeyChain, parent: UIElement = null): Unit =
    new AddDbDialog(ac).open(parent)

}

class AddDbDialog(ac:KeyChain) extends Dialog with CloseOnEsc { dialog =>
  import AddDbDialog.Log

  // Config
  title = "Edit Database"
  contents = Contents
  pack()

  // Open relative to parent
  def open(parent: UIElement): Unit = {
    modal = true
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
    def icon = new ImageIcon(dialog.getClass.getResource("icons/add_database.png"))
    def instructions =
      "Please specify the database host and port.\n" +
      "The name will be filled in automatically."
  }

  // Our body
  object Body extends GridBagPanel with Rows {

    addRow(new Label("Host:"), host)
    addRow(new Label("Port:"), port)

    lazy val host = new TextField() {
      // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
      val ValidIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$".r
      val ValidHostnameRegex = "^(([a-zA-Z]|[a-zA-Z][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z]|[A-Za-z][A-Za-z0-9\\-]*[A-Za-z0-9])$".r
      def matches(r:Regex) = r.pattern.matcher(text).matches
      def valid = matches(ValidHostnameRegex) || matches(ValidIpAddressRegex)
      val defaultFG = foreground
      reactions += {
        case c:ValueChanged => foreground = if (valid) defaultFG else Color.RED
      }
    }

    lazy val port = new TextField("8443") {
      def valid = try { text.toInt > 0 } catch { case nfe:NumberFormatException => false }
      val defaultFG = foreground
      reactions += {
        case c:ValueChanged => foreground = if (valid) defaultFG else Color.RED
      }
    }

  }

  // Our footer
  object Footer extends BorderPanel {
    add(new FlowPanel {

      contents += new Button("Ok") {

        dialog.peer.getRootPane.setDefaultButton(peer)

        val title = "Connection Failed"

        val stdMsg =
          "Could not connect to the specified database.\n\n" +
          "This could mean the database is down or temporarily unreachable, \n" +
          "or you may have specified an incorrect hostname or port."

        val versionMsg =
          "Could not connect to the specified database.\n\n" +
            "It appears that your OT is out of date.\n" +
            "Please upgrade to the latest version."

        def msg(e: Throwable) =
          "Could not connect to the specified database.\n\n" +
          "There was an unexpected exception of type " + e.getClass.getSimpleName + ".\n" +
          "Please try again later, and report this condition if it persists."

        enabled = false

        def enable() = enabled = Body.host.valid && Body.port.valid

        Body.host.reactions += {
          case _:ValueChanged => enable()
        }
        Body.port.reactions += {
          case _:ValueChanged => enable()
        }

        def good(a:Any): Unit =
          dialog.close()

        def bad(e:Throwable, p:Peer): Unit = {
          Log.log(Level.WARNING, "Trouble contacting database.", e)
          val message = e match {
            case v: VersionException      => v.getLongMessage(s"${p.host}:${p.port}")
            case _: InvalidClassException => versionMsg
            case _: IOException           => stdMsg
            case _: Throwable             => msg(e)
          }
          Dialog.showMessage(dialog.Contents, message, title, Dialog.Message.Error, null)
        }

        reactions += {
          case ButtonClicked(_) =>
            val p = new Peer(Body.host.text, Body.port.text.toInt, null)

            val r = ac.addPeer(p).run.catchLeft.unsafePerformIO
            r.fold(e => bad(e, p), good) // TODO: doesn't look at result
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
