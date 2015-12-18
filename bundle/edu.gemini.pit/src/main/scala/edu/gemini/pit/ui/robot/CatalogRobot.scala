package edu.gemini.pit.ui.robot

import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.util.{BooleanToolPreference, PreferenceManager}
import edu.gemini.pit.catalog._
import BooleanToolPreference.{SIMBAD, NED, HORIZONS}
import java.awt.Component
import javax.swing.JOptionPane
import java.util.UUID
import edu.gemini.spModel.core.Coordinates

import scala.swing.Swing
import edu.gemini.pit.model.Model

class CatalogRobot(parent: Component) extends Robot {

  // Our state
  type State = Map[Target, Option[Failure]]
  protected[this] lazy val initialState: State = Map.empty

  // Some lenses
  private val semesterLens = Model.proposal andThen Proposal.semester
  private val targetLens = Model.proposal andThen Proposal.targets

  // Formerly we looked up all empty targets automatically; now we look them up only when told to do so, and only when
  // they're empty and not already queued up.
  def lookup(t: Target) {
    checkThread()
    if (t.isEmpty && !state.contains(t)) {
      logger.info(s"Performing catalog lookup for '${t.name}'")
      catalog.find(t.name)(callback(t))
    }
  }

  override protected def refresh(m: Option[Model]) {
    for (m <- m) {

      // Take the opportunity to remove state for targets that are no longer part of the model.
      // This won't amount to a big leak but we might as well clean it up.
      val ts = targetLens.get(m)
      state = state -- state.keys.filterNot(ts.contains)

    }
  }

  def catalog = {
    implicit val btp2bool = (btp: BooleanToolPreference) => PreferenceManager.get(btp).booleanValue()
    var cat = Catalog.empty("No catalog selected.")
    model.foreach {
      m =>
        if (SIMBAD) cat = cat && Simbad
        if (NED) cat = cat && Ned
        if (HORIZONS) cat = cat && Horizons(semesterLens.get(m))
    }
    cat
  }

  // This callback can come from anywhere, so route it onto the UI thread. This ensures that
  // updates are serial and always operate on the current model.
  def callback(t: Target)(r: Result) {
    Swing.onEDT {
      for (m <- model) {
        r match {

          // One result. Replace t with t0 wherever it occurs
          case Success(t0 :: Nil, Nil) =>
            val ts = targetLens.get(m)
            val t1 = withUuid(rename(t0, t.name), t.uuid)
            val i = ts.indexOf(t)
            if (i >= 0) {
              // it probably is...
              val ts0 = ts.updated(i, t1)
              model = Some(targetLens.set(m, ts0))
            }

          // Several choices. Let the user select one.
          case Success(ts, choices) => disambiguate(semesterLens.get(m), t.name, ts, choices) match {
            case None =>
              // User selected cancel. Remove t wherever it occurs. Note that the model might have
              // changed so be sure to fetch it again rather than using the outer 'm'
              model.foreach {
                m =>
                  val ts = targetLens.get(m)
                  model = Some(targetLens.set(m, ts.filterNot(_ == t)))
              }

            case Some(Left(t0))     =>
              // User selected one item, so pass it to our handler above
              callback(t)(Success(List(t0), Nil))

            case Some(Right(retry)) =>
              // User selected a retry option, so evaluate it
              retry(callback(t))
          }

          case fail: Failure =>
            // Record the failure, and we're done
            state = state + (t -> Some(fail))

        }
      }
    }
  }

  // Because copy isn't polymorphic
  private def rename(t: Target, newName: String) = t match {
    case t: TooTarget         => t.copy(name = newName)
    case t: SiderealTarget    => t.copy(name = newName)
    case t: NonSiderealTarget => t.copy(name = newName)
  }

  // Because copy isn't polymorphic
  private def withUuid(t: Target, uuid: UUID) = t match {
    case t: TooTarget         => t.copy(uuid = uuid)
    case t: SiderealTarget    => t.copy(uuid = uuid)
    case t: NonSiderealTarget => t.copy(uuid = uuid)
  }

  def disambiguate(sem: Semester, name: String, ts: Seq[Target], cs: Seq[Choice]): Option[Either[Target, Choice]] = {

    type E = Either[Target, Choice]

    implicit object Ord extends Ordering[E] {
      def compare(a0: E, b0: E): Int = (a0, b0) match {
        case (Left(a), Left(b))   => a.name.compareTo(b.name)
        case (Right(a), Left(b))  => a.name.compareTo(b.name)
        case (Left(a), Right(b))  => a.name.compareTo(b.name)
        case (Right(a), Right(b)) => a.name.compareTo(b.name)
      }
    }

    case class PickerTarget(item: E) {
      override def toString = item match {

        case Left(target: SiderealTarget) =>
          target.coords(sem.midPoint) match {
            case Some(Coordinates(ra, dec)) => s"${target.name} (${ra.toAngle.formatHMS}, ${dec.formatDMS}) ${target.magnitudes.map(_.band.name).mkString(" ")}"
            case None                       => s"${target.name} (--, --) ${target.magnitudes.map(_.band.name).mkString(" ")}"
          }

        case Left(target: NonSiderealTarget) =>
          target.coords(sem.midPoint) match {
            case Some(Coordinates(ra, dec)) => s"${target.name} (${ra.toAngle.formatHMS}, ${dec.formatDMS})"
            case None                       => s"${target.name} (--, --)"
          }

        case Left(target: TooTarget) => "???" // can't happen

        case Right(c) => c.name

      }
    }

    val all: Seq[E] = ts.map(Left(_): E) ++ cs.map(Right(_): E)

    val pts = all.sorted.map(PickerTarget)

    Option(JOptionPane.showInputDialog(
      parent,
      s"""Your search for "$name" yielded several possible targets.\nPlease select the one you intended:""",
      "Multiple Targets Found",
      JOptionPane.QUESTION_MESSAGE,
      null,
      pts.toArray,
      pts.head)).collect {
        case pt: PickerTarget => pt.item
      }
  }

}