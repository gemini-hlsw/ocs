package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.ui.util.ConversionResultDialog
import javax.xml.bind.UnmarshalException
import javax.swing.JOptionPane

import edu.gemini.pit.model.{Model, ModelConversion}
import java.io.File

import scalaz._
import edu.gemini.model.p1.immutable.Semester
import edu.gemini.shared.gui.Chooser

import swing.{Component, Dialog, UIElement}

class OpenAction(shell: RichShell[Model], handler: ((Model, Option[File]) => Unit)) extends ShellAction(shell, "Open", Some(KeyEvent.VK_O)) {

  override def apply() {
    val of = new Chooser[OpenAction]("defaultDir", shell.peer).chooseOpen("Proposal Documents", ".xml")
    of.map(Model.fromFile).foreach {
      case Success(model @ Model(_, ModelConversion(false, _, _))) =>
        handler(model, of)
        shell.model match {
          case None              => shell.close()
          case Some(Model.empty) => shell.close()
          case _                 => // nop
        }

      case Success(model @ Model(_, ModelConversion(true, _, _))) =>
        // Show dialog box with changes summary
        val cd = new ConversionResultDialog(model)
        cd.bind(Some(model), _ => ())
        cd.open(UIElement.wrap(shell.peer))

        handler(model, of)
        shell.model match {
          case None              => shell.close()
          case Some(Model.empty) => shell.close()
          case _                 => // nop
        }

      case Failure(e) => println(e)
        e match {
        case Right(x) => alert(x.toString()) // TODO: make this better
        case Left(x)  => alert(x.getMessage) // TODO: make this better
      }
    }
  }

  private def alert(s: String) {
    JOptionPane.showMessageDialog(shell.peer, s, "Open Failed", JOptionPane.ERROR_MESSAGE)
  }

}
