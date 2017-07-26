package edu.gemini.pit.ui.view.submit

import edu.gemini.model.p1.immutable._
import edu.gemini.shared.gui.GlassLabel
import scala.swing._
import event.ButtonClicked
import Swing._
import scalaz._
import Scalaz._
import javax.swing.BorderFactory._
import edu.gemini.pit.ui.robot.ProblemRobot.Problem
import BorderPanel.Position._
import java.awt.{Font, Color}
import edu.gemini.pit.ui.view.partner.PartnersFlags
import edu.gemini.pit.ui.util.{ProposalSubmissionErrorDialog, SharedIcons, Rows}
import edu.gemini.model.p1.submit.{SubmitResult, SubmitDestination, SubmitClient}
import java.io.File
import edu.gemini.pit.ui.robot.ProblemRobot
import edu.gemini.pit.model.{AppPreferences, Model}
import javax.swing.SwingUtilities
import edu.gemini.pit.ui.binding._
import edu.gemini.pit.ui.util.gface.SimpleListViewer

class SubmitView(ph: ProblemRobot, newShellHandler: (Model,Option[File]) => Unit, saveHandler: () => Boolean, submitClient: SubmitClient) extends BorderPanel with BoundView[Model] {panel =>
  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)

  // Bound
  val lens = Lens.lensId[Model]
  override def children = List(controls, viewer)

  // Mutable state for errors
  private var _dsrs:Map[SubmitDestination, SubmitResult] = Map.empty
  def dsrs = _dsrs
  def dsrs_=(dsrs:Map[SubmitDestination, SubmitResult]) {
    _dsrs = dsrs
    viewer.refresh()
  }

  // Pull an error message out, if any
  def sr(sub:Submission): Option[SubmitResult] = {
    val sd = sub match {
      case n: NgoSubmission                    => SubmitDestination.Ngo(n.partner)
      case e: ExchangeSubmission               => SubmitDestination.Exchange(e.partner)
      case s: SpecialSubmission                => SubmitDestination.Special(s.specialType)
      case l: LargeProgramSubmission           => SubmitDestination.LargeProgram
      case i: SubaruIntensiveProgramSubmission => SubmitDestination.SubaruIntensiveProgram
      case f: FastTurnaroundSubmission         => SubmitDestination.FastTurnaroundProgram
    }
    dsrs.get(sd)
  }

  // Read-only lenses for SubmitStatus and List[Submit]
  val statusLens:Lens[ProposalClass, SubmitStatus] = Lens.lensu((a, b) => sys.error("Lens is read-only."), a => SubmitStatus.forProposal(a, ph.state))
  val subsLens:Lens[ProposalClass, List[Submission]] = Lens.lensu((a, b) => sys.error("Lens is read-only."), {
    case q: QueueProposalClass          => q.subs.left.getOrElse(q.subs.right.toOption.toList)
    case c: ClassicalProposalClass      => c.subs.left.getOrElse(c.subs.right.toOption.toList)
    case e: ExchangeProposalClass       => e.subs
    case s: SpecialProposalClass        => List(s.sub)
    case l: LargeProgramClass           => List(l.sub)
    case i: SubaruIntensiveProgramClass => List(i.sub)
    case f: FastTurnaroundProgramClass  => List(f.sub)
  })

  // When the list of problems change, just rebind everything
  ph.addListener {ps:List[Problem] => rebind()}

  add(new BorderPanel {
    add(controls, North)
    add(viewer, Center)
  }, Center)

  object controls extends GridBagPanel with Rows with Bound[Model, Proposal] {

    val lens = Model.proposal
    override def children = List(status, description, reopen, submit)

    border = createEmptyBorder(10, 10, 10, 10)

    addRow(new Label("Status: "), status)
    addRow(description)
    addSpacer()
    addCentered(submit)
    addSpacer()
    addCentered(reopen)

    object status extends Label with Bound[Proposal, SubmitStatus] {
      font = font.deriveFont(Font.BOLD)
      horizontalAlignment = Alignment.Left
      val lens = Proposal.proposalClass andThen statusLens
      override def refresh(m:Option[SubmitStatus]) {
        text = ~m.map(s => if (tac) "TAC Mode" else s.title)
        foreground = m.map {
          case _ if tac   => Color.RED
          case Incomplete => Color.RED
          case Partial    => Color.RED
          case Success    => Color.GREEN.darker
          case Ready      => Color.GREEN.darker
        }.getOrElse(Color.BLACK /* doesn't matter */)
      }
    }

    object description extends TextArea with Bound[Proposal, SubmitStatus] {
      font = font.deriveFont(Font.ITALIC)
      val lens = Proposal.proposalClass andThen statusLens
      override def refresh(m:Option[SubmitStatus]) {
        text = ~m.map(s => if (tac) "Submit functionality is not available in TAC mode." else s.description)
      }
      enabled = false
      lineWrap = true
      wordWrap = true
      background = panel.background
      border = createEmptyBorder(10, 10, 10, 10)
      peer.setDisabledTextColor(Color.DARK_GRAY)
    }

    def tac = AppPreferences.current.mode == AppPreferences.PITMode.TAC

    object submit extends Button("Submit this Proposal") with Bound[Proposal, SubmitStatus] {

      val lens = Proposal.proposalClass andThen statusLens

      minimumSize = (200, minimumSize.height)
      enabled = false

      override def refresh(m:Option[SubmitStatus]) {
        enabled = ~m.map {
          case _ if tac => false
          case Ready    => true
          case Partial  => true
          case _        => false
        }
      }

      reactions += {
        case ButtonClicked(_) =>
          for (m <- panel.model) {
            if (saveHandler()) {
              val glass = GlassLabel.show(panel.peer, "Submitting Proposal...")
              submitClient.submit(m.proposal) { psr =>
                SwingUtilities.invokeLater(new Runnable {
                  def run() {
                    glass.foreach(_.hide())

                    val anySuccess = psr.results.exists(_.result.isSuccess)
                    val errors = psr.results.map(_.result).filter(!_.isSuccess)

                    if (anySuccess) {
                      // Save the model
                      val m0 = Model.proposal.set(m, psr.proposal)
                      panel.model = Some(m0)
                      saveHandler()
                    }
                    if (errors.nonEmpty) {
                      // Show prompt with error messages
                      SwingUtilities.invokeLater(new Runnable {
                        override def run() {
                          new ProposalSubmissionErrorDialog(SubmitStatus.msg(errors)).open(UIElement.wrap(panel.peer.getRootPane))
                        }
                      })
                    }
                  }
                })
              }
            }
          }
      }
    }

    object reopen extends Button("Open an Editable Copy") with Bound[Proposal, SubmitStatus] {
      val lens = Proposal.proposalClass andThen statusLens
      val rolledLens = Model.rolled
      minimumSize = (200, minimumSize.height)
      enabled = false
      override def refresh(m:Option[SubmitStatus]) {
        val wasRolled = panel.model map rolledLens.get
        enabled = ~m.map {
          case _ if tac => false
          case Partial  => true
          case Success  => true
          // REL-693 We need to allow opening editable copies of rolled proposals
          case _        => ~wasRolled
        }
      }
      reactions += {
        case ButtonClicked(_) =>
          for (m <- panel.model) {
            val lens = Model.proposal andThen Proposal.proposalClass
            val pc0 = lens.get(m).reset
            newShellHandler(rolledLens.set(lens.set(m.resetObservationMeta, pc0), false), None)
          }
      }
    }

  }

  object viewer extends SimpleListViewer[Model, List[Submission], Submission] {

    override val lens = Model.proposal andThen Proposal.proposalClass andThen subsLens
    val versionLens = Model.schemaVersion

    object columns extends Enumeration {
      val Partner, Status, Reference, Contact = Value
    }

    import columns._

    def elementAt(ss:List[Submission], i:Int) = ss(i)
    def size(ss:List[Submission]) = ss.length

    def text(s:Submission) = {
      case Partner   => s match {
        case n: NgoSubmission                    => Partners.name.get(n.partner).orNull
        case e: ExchangeSubmission               => Partners.name.get(e.partner).orNull
        case _: SpecialSubmission                => "Gemini Observatory"
        case _: LargeProgramSubmission           => "Large Program"
        case _: SubaruIntensiveProgramSubmission => "Subaru Intensive Program"
        case _: FastTurnaroundSubmission         => "Fast Turnaround"
      }
      case Reference => s.response.map(_.receipt.id).orNull
      case Contact   => s.response.flatMap(_.receipt.contact).orNull
      case Status => sr(s) match {
        case Some(SubmitResult.Offline(_))               => "Offline"
        case Some(SubmitResult.ClientError(_, _))        => "Error"
        case Some(SubmitResult.ServiceError(_, _, _, _)) => "Error"
        case Some(SubmitResult.SubmitException(_, _))    => "Error"
        case Some(SubmitResult.Success(_, _, _, _))      => "Received"
        case None if s.response.isDefined                => "Received"
        case None                                        => "None"
      }
    }

    override def tooltip(s:Submission) = {
      case Status => sr(s) match {
        case Some(SubmitResult.Offline(d))                  =>  SubmitStatus.offlineBackend(d)
        case Some(SubmitResult.ClientError(m, _))           => s"Client error: $m"
        case Some(SubmitResult.ServiceError(d, pc, 405, m)) => s"Service error (405) ${SubmitStatus.parseServiceError405(d, pc, m)}"
        case Some(SubmitResult.ServiceError(d, _, i, m))    => s"Service error ($i) ${SubmitStatus.genericError(d)}"
        case Some(SubmitResult.SubmitException(d, _))       => s"Internal error: ${SubmitStatus.genericError(d)}"
        case Some(SubmitResult.Success(_, _, _, _))         =>  "Proposal was successfully received."
        case None if s.response.isDefined                   =>  "Proposal was successfully received."
        case None                                           =>  "Proposal has not been submitted."
      }
    }

    def columnWidth = {
      case Partner   => (120, 120)
      case Status    => (80, 80)
      case Reference => (120, Int.MaxValue)
      case Contact   => (150, Int.MaxValue)
    }

    def icon(s:Submission) = {
      case Partner => s match {
        case n: NgoSubmission                    => PartnersFlags.flag.get(n.partner).orNull
        case e: ExchangeSubmission               => PartnersFlags.flag.get(e.partner).orNull
        case _: SpecialSubmission                => PartnersFlags.flag.get(LargeProgramPartner).orNull
        case _: LargeProgramSubmission           => PartnersFlags.flag.get(LargeProgramPartner).orNull
        case _: SubaruIntensiveProgramSubmission => PartnersFlags.flag.get(LargeProgramPartner).orNull
        case _: FastTurnaroundSubmission         => PartnersFlags.flag.get(LargeProgramPartner).orNull
      }
      case Status  => sr(s) match {
        case Some(SubmitResult.Success(_, _, _, _)) => SharedIcons.BULLET_GREEN
        case Some(SubmitResult.Offline(_))          => SharedIcons.BULLET_YELLOW
        case Some(_)                                => SharedIcons.BULLET_RED
        case None if s.response.isDefined           => SharedIcons.BULLET_GREEN
        case None                                   => SharedIcons.BULLET_GREY
      }
    }

  }

}

