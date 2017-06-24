package edu.gemini.util.security.ext.auth.ui

import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.KeyException
import edu.gemini.util.security.auth.keychain.KeyFailure
import edu.gemini.util.security.auth.keychain.Action.ActionOps
import edu.gemini.util.security.auth.keychain.{Action => KAction}

import scala.swing._
import event.{SelectionChanged, ValueChanged}
import java.awt
import javax.swing
import javax.swing.{Icon, ImageIcon}

import edu.gemini.util.security.principal._
import java.util.logging.{Level, Logger}
import java.io.IOException

import edu.gemini.spModel.core.{Affiliate, Peer, SPProgramID, VersionException}

object AddKeyDialog {

  private lazy val Log =
    Logger.getLogger(classOf[AddKeyDialog].getName)

  def open(ac: KeyChain, parent: UIElement = null): Unit =
    new AddKeyDialog(ac).open(parent)

  // For saving UI state between invocations.
  case class SavedState(peer: Peer, keyType: AddKeyDialog#KeyType[_ <: GeminiPrincipal])

  private var savedState: Option[SavedState] =
    None

}

class AddKeyDialog(ac: KeyChain) extends Dialog with CloseOnEsc { dialog =>

  import AddKeyDialog._

  // Config
  title = "Request a Key"
  contents = Contents
  pack()

  // Open relative to parent
  def open(parent: UIElement): Unit = {
    modal = true
    Option(parent).foreach(setLocationRelativeTo)
    open()
  }

  object RequestAction extends Action("Request") {
    enabled = false

    val msgTitle =
      "Request Failed"

    val secMsg =
      "The specified key name or password is incorrect.\n" +
      "This failure has been logged."

    val stdMsg =
      "Could not connect to the specified database.\n\n" +
        "This could mean the database is down or temporarily unreachable, \n" +
        "or you may have specified an incorrect hostname or port."

    def msg(e: Throwable) =
      "Could not connect to the specified database.\n\n" +
        "There was an unexpected exception of type " + e.getClass.getSimpleName + ".\n" +
        "Please try again later, and report this condition if it persists."

    def bad(e: Throwable, p: Peer): Unit =
      e match {

        case KeyException(KeyFailure.InvalidPassword) =>
          Log.log(Level.WARNING, "Authentication Failure")
          Dialog.showMessage(dialog.Contents, secMsg, msgTitle, Dialog.Message.Error, null)

        case _: IOException =>
          Log.log(Level.WARNING, "IO problem requesting key.", e)
          Dialog.showMessage(dialog.Contents, stdMsg, msgTitle, Dialog.Message.Error, null)

        case e: VersionException =>
          Log.log(Level.WARNING, "Logical version incompatibility requesting key.")
          Dialog.showMessage(dialog.Contents, e.getLongMessage(s"${p.host}:${p.port}"), msgTitle, Dialog.Message.Error, null)

        case _: Exception =>
          Log.log(Level.WARNING, "Unknown problem requesting key.", e)
          Dialog.showMessage(dialog.Contents, msg(e), msgTitle, Dialog.Message.Error, null)
      }

    def enable(): Unit =
      enabled = Body.nameBox.nameField.valid && Body.passField.valid

    def apply(): Unit = {
      val peer = Body.Database.selection.item.peer
      val ctor = Body.KeyTypes.selection.item.ctor
      val name = Body.nameBox.nameField.text
      val pass = Body.passField.password
      val principal = ctor(name)

      val action: KAction[Unit] =
        for {
          k <- ac.addKey(peer, principal, pass)
          _ <- if (Body.makeActive.selected) ac.select(Some((peer,k))) else KAction(()) // whenM y u no work
        } yield ()

      action.unsafeRun.fold(e => bad(e, peer), _ => close())
    }

  }




  object ResetAction extends Action("Reset Password") {
    enabled = false

    def msg(e: Throwable) =
      "Could not connect to the specified database.\n\n" +
        "There was an unexpected exception of type " + e.getClass.getSimpleName + ".\n" +
        "Please try again later, and report this condition if it persists."

    val okMsg =
      "Password has been reset. Please check your email."

    def bad(e: Throwable): Unit = {
      Log.log(Level.WARNING, "Unknown problem resetting password.", e)
      Dialog.showMessage(dialog.Contents, msg(e), "Request Failed", Dialog.Message.Error, null)
    }

    def good(x: Unit): Unit = {
      Dialog.showMessage(dialog.Contents, okMsg, "Password Reset", Dialog.Message.Info, null)
    }

    def enable(): Unit =
      enabled = Body.nameBox.nameField.valid && Body.KeyTypes.selection.item.name == "User Key"

    def apply(): Unit = {
      val peer = Body.Database.selection.item.peer
      val ctor = Body.KeyTypes.selection.item.ctor
      val name = Body.nameBox.nameField.text
      val principal = ctor(name).asInstanceOf[UserPrincipal] // YOLO
      if (Dialog.showConfirmation(dialog.Contents,
        s"This action will contact $peer and reset the password for $name.\n" +
          "Continue?",
          "Confirm Reset", Dialog.Options.YesNo, Dialog.Message.Question, null) == Dialog.Result.Ok) {


        ac.resetPasswordAndNotify(peer, principal).unsafeRun.fold(bad, good)


      }


      }

  }


  Body.nameBox.nameField.reactions += {
    case _: ValueChanged =>
      RequestAction.enable()
      ResetAction.enable()
  }

  Body.passField.reactions += {
    case _: ValueChanged => RequestAction.enable()
  }

  Body.KeyTypes.selection.reactions += {
    case _ => ResetAction.enable()
  }

  lazy val CancelAction =
    Action("Cancel") {
      close()
    }


  // Our main content object
  object Contents extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Add our content, defined below
    add(Header, BorderPanel.Position.North)
    add(Body, BorderPanel.Position.Center)
    add(Footer, BorderPanel.Position.South)

  }

  // Our header
  object Header extends Instructions {

    lazy val icon: Icon =
      new ImageIcon(dialog.getClass.getResource("icons/add_key.png"))

    lazy val instructions =
      "Request a key from the selected database.\n" +
        "Select a type and name, and provide the password."
  }

  // A class of key types
  case class KeyType[A <: GeminiPrincipal](name: String, ctor: String => A) {
    override def toString = name
  }

  // Our body
  object Body extends GridBagPanel with Rows {

    val nameLabel = new Label("Email:") {
      KeyTypes.selection.reactions += {
        case _: SelectionChanged => text = KeyTypes.selection.item.name match {
          case "User Key"    => "Email:"
          case "Program Key" |
               "Visitor Key" => "Program Id:"
          case "Partner Key" => "Partner:"
          case "Staff Key"   => "Facility:"
        }
      }
    }

    object nameBox extends BorderPanel {

      val nameField = new TextField {
        def valid = !text.isEmpty
      }

      val affiliateMenu = new ComboBox(Affiliate.values.toList.filter(_.isActive).map(_.displayValue)) {
        selection.reactions += {
          case _: SelectionChanged => nameField.text = selection.item
        }
      }

      add(nameField, BorderPanel.Position.Center)

      KeyTypes.selection.reactions += {
        case _: SelectionChanged =>
          nameField.text = ""
          KeyTypes.selection.item.name match {

            case "Staff Key" =>
              add(nameField, BorderPanel.Position.Center)
              nameField.text = "Gemini"
              nameField.enabled = false

            case "Partner Key" =>
              add(affiliateMenu, BorderPanel.Position.Center)
              nameField.text = affiliateMenu.selection.item

            case _ =>
              add(nameField, BorderPanel.Position.Center)
              nameField.text = ""
              nameField.enabled = true

          }
      }


    }

    val passField = new PasswordField {
      def valid = !password.isEmpty
    }
    passField.peer.putClientProperty("JPasswordField.cutCopyAllowed", true)

    val makeActive = new CheckBox {
      this.selected = true
      this.text = "Make this the current active key."
    }

    addRow(new Label("Science Database:"), Database)
    addRow(new Label("Key Type:"), KeyTypes)
    addRow(nameLabel, nameBox)
    addRow(new Label("Password:"), passField)

    addSeparator()
    addRow(makeActive)

    lazy val dbs: List[Peer] =
      ac.peers.map(_.toList).unsafeRun.fold(throw _, identity)

    case class DB(peer: Peer) {
      override def toString = peer.displayName
    }

    object Database extends ComboBox[DB](dbs.map(DB(_)))

    lazy val keyTypes = List(
      KeyType("User Key",    s => UserPrincipal(s)),
      KeyType("Program Key", s => ProgramPrincipal(SPProgramID.toProgramID(s))),
      KeyType("Partner Key", s => AffiliatePrincipal(Affiliate.fromString(s))),
      KeyType("Staff Key",   s => StaffPrincipal(s)),
      KeyType("Visitor Key", s => VisitorPrincipal(SPProgramID.toProgramID(s))))

    object KeyTypes extends ComboBox[KeyType[_ <: GeminiPrincipal]](keyTypes)

  }

  // Our footer
  object Footer extends BorderPanel {
    add(new FlowPanel {
      contents += new Button(ResetAction)
      contents += new Button(RequestAction) {
        dialog.peer.getRootPane.setDefaultButton(peer)
      }
      contents += new Button(CancelAction)
    }, BorderPanel.Position.East)
  }

  override def close(): Unit = {
    savedState = Some(SavedState(Body.Database.selection.item.peer, Body.KeyTypes.selection.item))
    super.close()
  }

  override def open(): Unit = {
    savedState.foreach { s =>
      // N.B. can't use == on the values themselves because they're path-dependant
      Body.dbs.find(_ == s.peer) foreach (p => Body.Database.selection.item = Body.DB(p))
      Body.keyTypes.find(_.name == s.keyType.name) foreach (Body.KeyTypes.selection.item = _)
    }
    super.open()
  }

}
