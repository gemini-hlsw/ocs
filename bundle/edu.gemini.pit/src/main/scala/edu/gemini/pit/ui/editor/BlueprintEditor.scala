package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.dtree._
import edu.gemini.pit.ui.util.StdModalWizard

import scala.swing._
import scala.swing.Swing._
import javax.swing
import java.awt
import swing.JLabel

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.dtree.exchange.{Keck, Subaru}
import edu.gemini.shared.Platform

import scalaz._
import Scalaz._

object BlueprintEditor {

  def rootNode(site:Site, sem:Semester) = site match {
    case Site.Keck => Keck()
    case Site.Subaru => Subaru()
    case _ => new Root(sem)
  }

  import scala.language.existentials

  def emptyState(site:Site, sem:Semester) = rootNode(site, sem).toUIPage.toInitialState

  def open(p:Proposal, bp:Option[BlueprintBase], editable:Boolean, parent:UIElement):Option[BlueprintBase] = {
    val initialState = bp.flatMap(b => emptyState(b.site, p.semester).recover(b)).getOrElse(emptyState(p.proposalClass match {
      case e: ExchangeProposalClass       => e.partner match {
        case ExchangePartner.KECK   => Site.Keck
        case ExchangePartner.SUBARU => Site.Subaru
        case ExchangePartner.CFHT   => Site.CFHT // Shouldn't happen
      }
      case s: SubaruIntensiveProgramClass => Site.Subaru
      case _                              => Site.GN // or GS; same thing
    }, p.semester))
    new BlueprintEditor(initialState, editable).open(parent)
  }

}

/**
 * Modal wizard that constructs most of a new Blueprint; in progress.
 */
class BlueprintEditor private (initialState:UIState[_, _], editable:Boolean) extends StdModalWizard[UIState[_, _], BlueprintBase]("Resource Configuration", initialState) {dialog =>

  // Our editor. We delegate apply()
  def editor = Data
  def apply(newState:UIState[_, _]) {
    Data.apply(newState)
  }

  // Bryan wants it to be resizable...
  resizable = true

  // Back action
  def back = state.undo.foreach(state = _)

  // Next action
  def next = {
    for {
      n <- nextState
      state0 <- n.left
    } state = state0
  }

  // Finish action
  def finish = nextState match {
    case Some(Right((_, bp:BlueprintBase))) => bp
    case _                                  => sys.error("Unexpected final state " + nextState) // TODO: this, more safely
  }

  // What would the next state be, based on the current selection?
  def nextState:Option[Either[UIState[_, _], Any]] ={
    def typed(s:UIState[_, _]) = s.asInstanceOf[UIState[Object, Object]]
    def styped(s:SelectUIState[_, _]) = s.asInstanceOf[SelectUIState[Object, Object]]
    typed(state) match {
      case s: SelectUIState[_, _] =>
        for {
          selection <- styped(s).select(Data.Options.selection.indices.toList)
        } yield typed(state)(selection)
      case _ => for { // TODO Make this non text field specific
        e <- Some(Data.TextEntry.text).filter(_.nonEmpty)
      } yield typed(state)(e)
    }
  }

  // Data area is a gridbag
  object Data extends BorderPanel with (UIState[_, _] => Unit) {
    val dimensions: Dimension = (200, 225)

    import BorderPanel.Position._

    // Propagate new state to our controls
    def apply(newState:UIState[_, _]) {

      // Field values
      Title.text = state.node.title
      Description.text = state.node.description
      state match {
        case s: SelectUIState[_, _] => addListView(s)
        case s: TextUIState[_] => addTextView(s)
      }

      // Button state
      enable(state.canUndo, state.canRedo || (state.default.isDefined && !state.canFinish), state.canFinish && editable)

    }

    // Our header area, main content (list), and instructions (all defined below)
    add(new BorderPanel {
      add(Title, North)
      add(Description, Center)
    }, North)

    add(Instructions, South)

    // Title is just a label
    object Title extends Label {
      horizontalAlignment = Alignment.Left
      font = font.deriveFont(awt.Font.BOLD)
    }

    // Description is a text area with some tweaking
    object Description extends TextArea {
      opaque = false
      editable = false
      columns = 30
      rows = 3
      peer.setLineWrap(true)
      peer.setWrapStyleWord(true)
      border = swing.BorderFactory.createEmptyBorder(4, 4, 4, 4)
      foreground = awt.Color.DARK_GRAY
    }

    // Our text entry is a text field
    object TextEntry extends TextField("", 0) {
      // Setup listeners when the user presses a key
      keys.reactions += {
        case e => nextState match {
          case None           => enable(state.canUndo, false, false)
          case Some(Left(_))  => enable(state.canUndo, true, false)
          case Some(Right(_)) => enable(state.canUndo, false, true)
        }
      }
    }

    def addTextView(s: TextUIState[_]) = {
      TextEntry.text = ~s.defaultString // Set the text from the current state
      add(new BorderPanel {
        preferredSize = dimensions;
        add(TextEntry, North)
      }, Center)
      pack()
    }


    // Our options are a list view
    object Options extends ListView(Seq.empty[Any]) {

      // Disallow focus (it's ugly)
      focusable = false

      // When my selection changes, this affects the footer buttons
      selection.reactions += {
        case e => nextState match {
          case None           => enable(state.canUndo, false, false)
          case Some(Left(_))  => enable(state.canUndo, true, false)
          case Some(Right(_)) => enable(state.canUndo, false, true)
        }
      }

      // Interpret a double-click here as a single-click on a button
      peer.addMouseListener(new awt.event.MouseAdapter {
        val buttons = List(Footer.Finish, Footer.Next)
        override def mouseClicked(me:awt.event.MouseEvent) {
          if (me.getClickCount == 2)
            buttons.find(_.enabled).foreach(_.doClick(1))
        }
      })

      // Update the cell renderer to do something other than toString
      renderer = new ListView.Renderer[Any] {

        val delegate = renderer

        def componentFor(list:ListView[_], isSelected:Boolean, focused:Boolean, a:Any, index:Int) = {
          val c = delegate.componentFor(list, isSelected, focused, a, index)
          c.peer.asInstanceOf[JLabel].setText(stringValue(a))
          c
        }

        def stringValue(a:Any):String = try {
          a.asInstanceOf[AnyRef].getClass.getMethod("value").invoke(a) match {
            case e:Enum[_] => stringValue(e)
            case a         => a.toString
          }
        } catch {
          case _: Throwable => a.toString
        }

      }
    }

    def addListView(s: SelectUIState[_, _]) = {
      val node:SelectNode[_, _, _, _] = s.node match {
        case n: SingleSelectNode[_, _, _] => {
          Instructions.visible = false
          Options.selection.intervalMode = ListView.IntervalMode.Single
          n
        }
        case n: MultiSelectNode[_, _, _] => {
          Instructions.visible = true
          Options.selection.intervalMode = ListView.IntervalMode.MultiInterval
          n
        }
        case _ => sys.error("Should not happen")
      }

      Options.listData = node.choices
      // Editability
      Options.enabled = editable

      // Pre-selection, for undo/redo via state.selection otherwise default via state.default
      for {
        i <- if (!s.selection.isEmpty)
          s.selection
        else
          s.default.map(node.choices.indexOf(_)).filterNot(_ == -1).toList
      } Data.Options.selection.indices += i

      add(new ScrollPane(Options) {
        preferredSize = dimensions
      }, Center)
      pack()
    }

    // Instructions are another label whose content changes based on platform
    object Instructions extends Label {
      horizontalAlignment = Alignment.Left
      foreground = awt.Color.DARK_GRAY
      visible = false
      text = Platform.IS_MAC match {
        case true  => "Command+Click to select more than one option."
        case false => "Ctrl+Click to select more than one option."
      }
    }

  }

}

