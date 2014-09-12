package jsky.app.ot.tpe

import edu.gemini.ags.api.{DefaultMagnitudeTable, AgsRegistrar, AgsStrategy}
import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.guide.GuideProbe
import jsky.app.ot.gemini.altair.Altair_WFS_Feature
import jsky.app.ot.gemini.inst.OIWFS_Feature
import jsky.app.ot.gemini.tpe.TpePWFSFeature
import jsky.app.ot.util.Resources

import java.awt.{Color, Font}
import javax.swing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.GridBagPanel.Anchor.West
import scala.swing.GridBagPanel.Fill.{Both, Horizontal}
import scala.swing.event.{ButtonClicked, WindowClosing}
import scala.util.{Failure, Success}
import java.util.logging.{Level, Logger}


/**
 * Placeholder.  We needed something to replace the old AgsClient which uses
 * the old AGS API.  A better UI is planned.
 */

object AgsClient {
  private val Note = "The Automatic Guide Star selection feature attempts to " +
                     "find the best guide star (s) for your observation."

  private val NothingFoundMessage =
    "Sorry, could not find any guide stars for your observation."

  private val ExceptionMessage =
    "There was a problem communicating with the remote catalog servers."

  private val SearchingIcon = Resources.getIcon("spinner.gif")

  private def javaIcon(k: String) = UIManager.getLookAndFeelDefaults.get(k).asInstanceOf[Icon]
  private val FailureIcon = javaIcon("OptionPane.errorIcon")

  private val Log = Logger.getLogger(this.getClass.getName)

  def launch(n: ISPNode, relativeTo: JComponent): Unit = launch(TpeContext.apply(n), relativeTo)

  def launch(tpeCtx: TpeContext, relativeTo: JComponent): Unit =
    tpeCtx.obsContext.foreach { obsCtx =>
      AgsRegistrar.selectedStrategy(obsCtx).foreach { strategy =>
        val fut    = strategy.select(obsCtx, DefaultMagnitudeTable(obsCtx))
        val dialog = new AgsClient(tpeCtx)

        fut.onComplete {
          case Success(sel) => Swing.onEDT { dialog.handleSuccess(sel) }
          case Failure(ex)  => Swing.onEDT { dialog.handleFailure(ex) }
        }

        dialog.pack()
        dialog.location = SwingUtilities.windowForComponent(relativeTo).getLocationOnScreen
        dialog.visible  = true
      }
    }
}

import AgsClient._

class AgsClient(ctx: TpeContext) extends Dialog {
  modal         = true
  resizable     = false
  preferredSize = new Dimension(500,200)
  minimumSize   = preferredSize

  var cancelled = false

  reactions += {
    case WindowClosing(e) => quit()
  }

  object mainPanel extends GridBagPanel {
    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

    val statusIcon = new Label() {
      icon = SearchingIcon
    }
    layout(statusIcon) = new Constraints {
      gridx   = 0
      gridy   = 0
      weightx = 0.0
      insets  = new Insets(0, 0, 0, 10)
    }

    val statusMessage = new Label("Searching for guide stars ...") {
      horizontalAlignment = Alignment.Left
      font       = font.deriveFont(Font.BOLD).deriveFont(font.getSize2D + 8.0f)
      foreground = Color.BLACK
    }
    layout(statusMessage) = new Constraints {
      gridx   = 1
      gridy   = 0
      fill    = Horizontal
      weightx = 1.0
      anchor  = West
    }

    val explanatoryText = new TextArea() {
      wordWrap      = true
      lineWrap      = true
      editable      = false
      preferredSize = new Dimension(0, 50)
      foreground    = Color.DARK_GRAY
      text          = Note
    }
    layout(explanatoryText) = new Constraints {
      gridx     = 0
      gridy     = 1
      gridwidth = 2
      fill      = Horizontal
      weightx   = 1.0
      insets    = new Insets(15, 10, 5, 10)
    }

    layout(new BorderPanel()) = new Constraints {
      gridx     = 0
      gridy     = 2
      gridwidth = 2
      fill      = Both
      weightx   = 1.0
      weighty   = 1.0
    }

    object buttonPanel extends GridBagPanel {
      border = BorderFactory.createCompoundBorder(
                 BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                 BorderFactory.createEmptyBorder(10, 0, 0, 0))

      layout(new BorderPanel()) = new Constraints {
        gridx   = 0
        fill    = Horizontal
        weightx = 1.0
      }

      val acceptButton = new Button("OK") {
        enabled = false
      }
      layout(acceptButton) = new Constraints {
        gridx = 1
      }
      val cancelButton = new Button("Cancel")
      layout(cancelButton) = new Constraints {
        gridx  = 2
        insets = new Insets(0, 5, 0, 0)
      }
    }

    layout(buttonPanel) = new Constraints {
      gridx     = 0
      gridy     = 4
      gridwidth = 2
      fill      = Horizontal
      weightx   = 1.0
    }
  }

  contents = mainPanel

  import mainPanel.buttonPanel.cancelButton
  listenTo(cancelButton)
  reactions += {
    case ButtonClicked(`cancelButton`) => quit()
  }

  private def quit(): Unit = {
    cancelled = true
    visible   = false
    dispose()
  }

  private def handleSuccess(selOpt: Option[AgsStrategy.Selection]): Unit =
    if (!cancelled) {
      selOpt match {
        case Some(sel) =>
          applySelection(sel)
          showTpeFeatures(sel)
          quit()
        case _ =>
          setupManualSearch(NothingFoundMessage)
      }
    }

  private def setupManualSearch(msg: String): Unit = {
    mainPanel.explanatoryText.text = msg + "  Try a manual search?"
    mainPanel.statusIcon.icon      = FailureIcon
    mainPanel.statusMessage.text   = "No guide star was found."

    import mainPanel.buttonPanel.acceptButton
    acceptButton.text = "Manual Search"
    acceptButton.enabled = true
    listenTo(acceptButton)
    reactions += {
      case ButtonClicked(`acceptButton`) =>
        TpeGuideStarDialog.showDialog(TpeManager.open().getImageWidget)
        quit()
    }
  }

  private def handleFailure(ex: Throwable): Unit =
    if (!cancelled) {
      Log.log(Level.WARNING, "AGS lookup failure", ex)
      setupManualSearch(ExceptionMessage)
    }

  private def showTpeFeatures(sel: AgsStrategy.Selection): Unit =
    Option(TpeManager.get()).filter(_.isVisible).foreach { tpe =>
      sel.assignments.foreach { ass =>
        val clazz = ass.guideProbe.getType match {
          case GuideProbe.Type.AOWFS => classOf[Altair_WFS_Feature]
          case GuideProbe.Type.OIWFS => classOf[OIWFS_Feature]
          case GuideProbe.Type.PWFS  => classOf[TpePWFSFeature]
        }
        Option(tpe.getFeature(clazz)).foreach { tpe.selectFeature }
      }
    }

  private def applySelection(sel: AgsStrategy.Selection): Unit = {
    // Make a new TargetEnvironment with the guide probe assignments.
    val newEnv = sel.applyTo(ctx.targets.envOrDefault)

    // Update the TargetEnvironment.
    ctx.targets.dataObject.foreach { targetComp =>
      targetComp.setTargetEnvironment(newEnv)
      ctx.targets.commit()

      // Update the position angle, if necessary.
      ctx.instrument.dataObject.foreach { inst =>
        val deg = sel.posAngle.toDegrees.getMagnitude
        val old = inst.getPosAngleDegrees
        if (deg != old) {
          inst.setPosAngleDegrees(deg)
          ctx.instrument.commit()
        }
      }
    }
  }
}