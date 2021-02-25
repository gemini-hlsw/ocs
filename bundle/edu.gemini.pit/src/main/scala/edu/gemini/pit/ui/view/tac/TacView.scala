package edu.gemini.pit.ui.view.tac

import edu.gemini.shared.gui.textComponent.{NumberField, SelectOnFocus}

import scalaz._
import Scalaz._
import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.binding._
import edu.gemini.pit.ui.view.partner.PartnersFlags
import java.util.Locale
import swing._
import event.ValueChanged
import javax.swing.{BorderFactory, JLabel}
import edu.gemini.pit.ui.util._
import edu.gemini.pit.ui.editor.SubmissionRequestEditor
import edu.gemini.pit.model.{AppPreferences, Model}

object TacView {
  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)

  type Partner = Any // eek; this is fallout from the schema generator

  // Given a ProposalClass, return a map from partner to response. Due to the varied shapes of the ProposalClass
  // types this is kind of ugly.
  def responses(pc:ProposalClass):Map[Partner, SubmissionResponse] =
    (pc match {
      case g:GeminiNormalProposalClass => g.subs match {
        case Left(ns)                         => ns.filter(_.response.isDefined).map {s => s.partner -> s.response}
        case Right(e) if e.response.isDefined => List(e.partner -> e.response)
        case _                                => Nil
      }
      case e:ExchangeProposalClass     => e.subs.filter(_.response.isDefined).map {s => s.partner -> s.response}
      case l:LargeProgramClass         => l.sub.response.map(s => List(LargeProgramPartner -> Some(s))).getOrElse(Nil)

      // We only support SpecialProposalClass if it's a GT proposal, otherwise this method yields
      // no partners and the tab's controls will be disabled. Elsewhere we don't bother with the
      // `gt.sub.specialType` check.
      case gt:SpecialProposalClass if gt.sub.specialType === SpecialProposalType.GUARANTEED_TIME =>
        gt.sub.response.map(r => List(GuaranteedTimePartner -> Some(r))).getOrElse(Nil)

      case _                           => Nil
    }).toMap[Partner, Option[SubmissionResponse]].mapValues(_.get)

  // Given a ProposalClass, return a list of all partners for which responses exist.
  def partners(pc:ProposalClass):List[Partner] = responses(pc).toList.map(_._1)

  // Given a Partner, return a Lens setter for that partner's response. TODO: testcases
  def setResponse(p:Partner)(pc:ProposalClass, sr:SubmissionResponse):ProposalClass = {

    def swapNgo(ns:List[NgoSubmission]) = ns map {
      case s if s.partner == p => s.copy(response = Some(sr))
      case s                   => s
    }

    def swapExchange(es:ExchangeSubmission) =
      if (es.partner == p) es.copy(response = Some(sr)) else es

    pc match {
      case q:QueueProposalClass     => q.subs match {
        case Left(ns) => q.copy(subs = Left(swapNgo(ns)))
        case Right(e) => q.copy(subs = Right(swapExchange(e)))
      }
      case c:ClassicalProposalClass => c.subs match {
        case Left(ns) => c.copy(subs = Left(swapNgo(ns)))
        case Right(e) => c.copy(subs = Right(swapExchange(e)))
      }
      case e:ExchangeProposalClass  => e.copy(subs = swapNgo(e.subs))
      case l:LargeProgramClass      => l.copy(sub = l.sub.copy(response = Some(sr)))
      case s:SpecialProposalClass   => s.copy(sub = s.sub.copy(response = Some(sr)))
      case _                        => pc
    }

  }

  def isTac = AppPreferences.current.mode == AppPreferences.PITMode.TAC

}

// This view ends up being complex because it's the only in-place master-detail editor in the PIT. Everywhere else
// we're editing a single value (like ProposalClass) or use popups to edit things when there may be more that one
// value. So this is an unhappy situation. Perhaps something can get abstracted out.
class TacView(loc: Locale) extends BorderPanel with BoundView[ProposalClass] { view =>
  import TacView._

  // Bound
  val lens = Model.proposal >=> Proposal.proposalClass
  override def children = List(form)

  add(form, BorderPanel.Position.Center)

  // Our data entry form
  object form extends GridBagPanel with Rows with Bound.Self[ProposalClass] {

    // Bound
    override def children = List(proposalClassLabel, partnerLabel, tacEmailLabel, partner)

    // Our content, defined below
    addRow(new Label("Proposal Class:"), proposalClassLabel)
    addRow(partnerLabel, partner)
    addRow(new Label("Decision:"), decision)
    addRow(tacEmailLabel, email)
    addRow(new Label("Partner Ranking:"), ranking)
    addRow(new Label("Recommended Time:"), time)
    addRow(new Label("Poor Weather:"), poorWeather)
    addRow(new Label("Comments:"), new ScrollPane(comment), GridBagPanel.Fill.Both, 1)

    object proposalClassLabel extends Label with Bound.Self[ProposalClass] {
      text = "Large Program"
      horizontalAlignment = Alignment.Left

      override def refresh(m:Option[ProposalClass]) = text = ~m.map(_.classLabel)
    }

    object partnerLabel extends Label with Bound.Self[ProposalClass] {
      text = "Partner:"

      override def refresh(m:Option[ProposalClass]) = visible = m match {
        case Some(_: LargeProgramClass | _: SpecialProposalClass) => false
        case _                          => true
      }
    }

    object tacEmailLabel extends Label with Bound.Self[ProposalClass] {

      override def refresh(m:Option[ProposalClass]) = text = m match {
        case Some(_: LargeProgramClass | _: SpecialProposalClass) => "Support Staff Email:"
        case _                          => "NGO Support Staff Email:"
      }
    }

    // Our combo box for selecting partners. Typically this will contain only one entry, but strictly speaking it's
    // possible to end up with more than one so we will support it here. It does complicate things a bit. This control
    // is the root for another set of bound controls, which are bound directly and not via continued lensing of the
    // outer model. This leads to a bit of tricky stuff (see below).
    object partner extends ComboBox[Partner](Nil) with Bound.Self[ProposalClass] {

      // Render with flags and proper names
      renderer = new PartnerRenderer(renderer)

      private var oldItems:Option[List[Partner]] = None

      override def refresh(m:Option[ProposalClass]) = {
        val newItems = ~m.map(partners)
        enabled = isTac && m.isDefined
        if (!(~oldItems.map(_ == newItems))) {
          oldItems = Some(newItems)
          this.peer.setModel(ComboBox.newConstantModel(newItems)) // ick
          Partners.forLocale(loc).foreach(selection.item = _) // pick a default if we can, just to be nice
          enabled = Option(selection.item).isDefined
        }
        bindUnderlings() // important; see below
        val isVisible = m match {
          case Some(_: LargeProgramClass | _: SpecialProposalClass) => false
          case _                          => true
        }
        visible = isVisible
      }

      selection.reactions += {
        case _ => bindUnderlings()
      }

      // Our list of children for the new binding root.
      val underlings:List[Bound[SubmissionResponse, _]] = List(decision, email, ranking, time, poorWeather, comment)

      // returns the large partner for large programs or the selected item on the combobox
      def partnerInEffect = Option(selection.item).getOrElse(LargeProgramPartner)

      // Bind each child component to the submission response corresponding with the *selected* partner. This makes the
      // underling controls behave as if there's only one response, so their implementation is easy.
      def bindUnderlings() {
        for {
          pc <- model
          rs =  responses(pc) // expensive
          p  <- Option(selection.item)
          u  <- underlings
        } {
          // See below for explanations for these functions
          u.bind(rs.get(p), osr =>
            osr.foreach {sr =>
              model = Some(setResponse(p)(pc, sr))
            }
          )
        }
      }

    }

    sealed trait Status
    case object Undecided extends Status
    case object Accepted extends Status
    case object Rejected extends Status

    // Our decision combo.
    object decision extends ComboBox[Status](Seq(Undecided, Accepted, Rejected)) with Bound[SubmissionResponse, Option[SubmissionDecision]] {
      val lens = SubmissionResponse.decision
      enabled = false
      private var updating = false // ugh
      override def refresh(m:Option[Option[SubmissionDecision]]) {
        try {
          updating = true
          selection.item = m.flatten.map(_.decision.fold(identity, identity)).map {
            case _:SubmissionReject => Rejected
            case _:SubmissionAccept => Accepted
          }.getOrElse(Undecided)
          enabled = isTac && m.isDefined
        } finally {
          updating = false
        }
      }
      selection.reactions += {
        case _ if !updating => model = Some(selection.item match {
          case Rejected  => Some(SubmissionDecision(Left(SubmissionReject)))
          case Accepted  => Some(SubmissionDecision(Right(SubmissionAccept.empty)))
          case Undecided => None
        })
      }
    }

    // We have two text fields with similar behavior
    trait OptionText[A] extends SelectOnFocus with Bound[SubmissionResponse, Option[A]] {comp:TextComponent =>
      def focus:Lens[A, String]
      comp.enabled = false
      private var updating = false // ugh
      override def refresh(m:Option[Option[A]]) {
        if (!updating) try {
          updating = true
          val (t, e) = ~m.flatten.map(sa => (focus.get(sa), true))
          text = t
          comp.enabled = isTac && e
        } finally {
          updating = false
        }
      }
      reactions += {
        case ValueChanged(_) => model.foreach {s =>
          if (~s.map(_.toString) != text && !updating) try {
            updating = true
            model = Some(s.map(sa => focus.set(sa, text)))
          } finally {
            updating = false
          }
        }
      }
    }

    object email extends TextField with OptionText[SubmissionAccept] with EmailText {
      val lens = SubmissionResponse.acceptDecision
      override def enabled_=(b:Boolean) {
        super.enabled = b
        opaque = b
      }
      val focus:Lens[SubmissionAccept, String] = Lens.lensu((a, b) => a.copy(email = b), _.email)
    }

    object ranking extends NumberField(None, allowEmpty = false) with OptionText[SubmissionAccept] {
      val lens = SubmissionResponse.acceptDecision
      override def enabled_=(b:Boolean) {
        super.enabled = b
        opaque = b
      }
      val focus:Lens[SubmissionAccept, String] = Lens.lensu((a, b) =>
        a.copy(ranking = try {
          form.ranking.text.toDouble
        } catch {
          case _:NumberFormatException => 0.0
        }),
        _.ranking.toString)
    }

    // Recommended time control set
    object time extends FlowPanel(FlowPanel.Alignment.Left)() with Bound[SubmissionResponse, Option[SubmissionAccept]] {
      val lens = SubmissionResponse.acceptDecision

      // Bound
      override def children = List(edit, label)

      // Configure the panel
      vGap = 0
      hGap = 0

      // Add our children, defined below
      peer.add(edit.peer)
      peer.add(label.peer)

      // Recommended time edit button
      object edit extends Button with Bound.Self[Option[SubmissionAccept]] {button =>

        // Enabled when it's an accept
        override def refresh(m:Option[Option[SubmissionAccept]]) {
          enabled = isTac && m.flatten.isDefined
        }

        // Our action, which re-uses the submission request editor
        action = Action("") {
          for {
            Some(sa @ SubmissionAccept(_, _, time, minTime, _)) <- model
            req = SubmissionRequest(time, minTime, None, None)
            (r, _, _) <- SubmissionRequestEditor.open(req, None, Nil, None, button)
          } model = Some(Some(sa.copy(recommended = r.time, minRecommended = r.minTime)))
        }

        // Configure. Must be done AFTER setting the action (duh)
        icon = SharedIcons.ICON_CLOCK
        enabled = false

      }

      // Recommended time label
      object label extends Label with Bound.Self[Option[SubmissionAccept]] {

        // Configure
        horizontalAlignment = Alignment.Left
        border = BorderFactory.createEmptyBorder(0, 3, 0, 0)

        // Enabled when it's an accept
        override def refresh(m:Option[Option[SubmissionAccept]]) {
          text = ~m.flatten.map {a =>
            "%1.2f %s (%1.2f %s minimum)".format(
              a.recommended.value,
              a.recommended.units.value,
              a.minRecommended.value,
              a.minRecommended.units.value)
          }
        }

      }

    }

    // Poor weather
    sealed trait PoorWeatherChoice {
      def value:Boolean
    }

    case object PoorWeatherYes extends PoorWeatherChoice {
      val value = true
      override def toString = "Yes, poor weather candidate."
    }

    case object PoorWeatherNo extends PoorWeatherChoice {
      val value = false
      override def toString = "No, not a poor weather candidate."
    }

    object poorWeather extends ComboBox[PoorWeatherChoice](Seq(PoorWeatherNo, PoorWeatherYes)) with Bound[SubmissionResponse, Option[SubmissionAccept]] {
      val lens = SubmissionResponse.acceptDecision
      enabled = false

      // Enabled when it's an accept
      override def refresh(m:Option[Option[SubmissionAccept]]) {
        val (b, e) = ~m.flatten.map(sa => (sa.poorWeather, true))
        enabled = isTac && e
        selection.item = if (b) PoorWeatherYes else PoorWeatherNo
      }

      selection.reactions += {
        case _ => for {
          Some(m) <- model
        } model = Some(Some(m.copy(poorWeather = selection.item.value)))
      }

    }

    object comment extends TextArea with OptionText[String] {
      val lens:Lens[SubmissionResponse,Option[String]] = Lens.lensu((a, b) => a.copy(comment = b), _.comment.orElse(Some("")))
      val focus = Lens.lensId[String]
      override def enabled_=(b:Boolean) {
        super.enabled = b
        opaque = b
      }
      override def refresh(m:Option[Option[String]]) {
        super.refresh(m)
        enabled = isTac && m.isDefined
      }
    }

  }

  ///
  /// HELPERS
  ///

  class PartnerRenderer(delegate:ListView.Renderer[Partner]) extends ListView.Renderer[Partner] {
    def componentFor(list:ListView[_], isSelected:Boolean, focused:Boolean, a:Partner, index:Int) = {
      val c = delegate.componentFor(list, isSelected, focused, a, index)
      val (t, i) = Option(a) map {n =>
        (Partners.name.get(n).orNull, PartnersFlags.flag.get(n).orNull)
      } getOrElse(("Select", null))
      c.peer.asInstanceOf[JLabel].setText(t)
      c.peer.asInstanceOf[JLabel].setIcon(i)
      c
    }
  }



}
