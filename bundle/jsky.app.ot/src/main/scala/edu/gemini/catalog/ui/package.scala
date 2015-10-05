package edu.gemini.catalog

import javax.swing.table._

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsRegistrar, AgsStrategy}
import edu.gemini.catalog.api.{UCAC4, CatalogName, CatalogQuery}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.obs.context.ObsContext
import jsky.app.ot.gemini.editor.targetComponent.GuidingFeedback.ProbeLimits

import scala.language.existentials
import scala.swing.{Alignment, Label, Component}
import scalaz._
import Scalaz._

/**
 * contains the Catalog Navigator UI model classes
 */
package object ui {

  /**
   * Locally describe an ags strategy including its limits and the query that would trigger
   */
  case class SupportedStrategy(strategy: AgsStrategy, limits: Option[ProbeLimits], query: List[CatalogQuery])

  /**
   * Describes the observation used to do a Guide Star Search
   */
  case class ObservationInfo(objectName: Option[String], instrumentName: Option[String], strategy: Option[AgsStrategy], validStrategies: List[SupportedStrategy], conditions: Option[Conditions], catalog: CatalogName) {
    def catalogQuery:List[CatalogQuery] = validStrategies.collect {
        case SupportedStrategy(s, _, query) if s == strategy => query
      }.flatten
  }

  object ObservationInfo {

    /**
     * Converts an AgsStrategy to a simpler description to be stored in the UI model
     */
    private def toSupportedStrategy(obsCtx: ObsContext, strategy: AgsStrategy, mt: MagnitudeTable):SupportedStrategy = {
      val pb = strategy.magnitudes(obsCtx, mt).map(k => ProbeLimits(strategy.probeBands, obsCtx, k._2)).headOption
      val queries = strategy.catalogQueries(obsCtx, mt)
      SupportedStrategy(strategy, pb.flatten, queries)
    }

    def apply(ctx: ObsContext, mt: MagnitudeTable):ObservationInfo = ObservationInfo(
      Option(ctx.getTargets.getBase).map(_.getTarget.getName),
      Option(ctx.getInstrument).map(_.getTitle),
      AgsRegistrar.currentStrategy(ctx),
      AgsRegistrar.validStrategies(ctx).map(toSupportedStrategy(ctx, _, mt)),
      ctx.getConditions.some,
      UCAC4)

  }

  /**
   * Represents a table column description
   * @tparam T The type of the column
   */
  abstract class CatalogNavigatorColumn[T >: Null] {
    def title: String

    def lens: PLens[Target, T]

    // Display the value in the table as String. values are Any in the table model
    def displayValue(t: Any): Option[String] = t.toString.some

    // Extract the data from the target via the lens
    def render(target: Target): Option[T] = lens.get(target)

    // Indicates the class of the column
    def clazz:Class[_] = manifest.runtimeClass
  }

  case class IdColumn(title: String) extends CatalogNavigatorColumn[String] {
    override val lens = Target.name
  }

  case class RAColumn(title: String) extends CatalogNavigatorColumn[RightAscension] {
    override val lens = Target.coords >=> Coordinates.ra.partial

    override def displayValue(t: Any) = Option(t).collect {
      case r: RightAscension => r.toAngle.formatHMS
    }
  }

  case class DECColumn(title: String) extends CatalogNavigatorColumn[Declination] {
    override val lens = Target.coords >=> Coordinates.dec.partial

    override def displayValue(t: Any) = Option(t).collect {
      case d: Declination => d.formatDMS
    }
  }

  case class DistanceColumn(base: Coordinates, title: String) extends CatalogNavigatorColumn[Angle] {
    val distance: Coordinates @> Angle = Lens(c => Store(r => c, Coordinates.difference(base, c).distance))

    override val lens = Target.coords >=> distance.partial

    override def displayValue(t: Any) = Option(t).collect {
      case a: Angle => f"${a.toArcmins}%.2f"
    }
  }

  case class PMRAColumn(title: String) extends CatalogNavigatorColumn[RightAscensionAngularVelocity] {
    override val lens = Target.pm >=> ProperMotion.deltaRA.partial

    override def displayValue(t: Any) = Option(t).collect {
      case r: RightAscensionAngularVelocity => f"${r.velocity.masPerYear}%.2f"
    }
  }

  case class PMDecColumn(title: String) extends CatalogNavigatorColumn[DeclinationAngularVelocity] {
    override val lens = Target.pm >=> ProperMotion.deltaDec.partial

    override def displayValue(t: Any) = Option(t).collect {
      case d: DeclinationAngularVelocity => f"${d.velocity.masPerYear}%.2f"
    }
  }

  case class MagnitudeColumn(band: MagnitudeBand) extends CatalogNavigatorColumn[Magnitude] {
    override val title = band.name
    // Lens from list of magnitudes to the band's magnitude if present
    val bLens: List[Magnitude] @?> Magnitude = PLens(ml => ml.find(_.band === band).map(m => Store(b => sys.error("read-only lens"), m)))
    override val lens = Target.magnitudes >=> bLens

    override def displayValue(t: Any) = Option(t).collect {
      case m: Magnitude => f"${m.value}%.2f"
    }
  }

  // Required to give limits to the existential type list
  type ColumnsList = List[CatalogNavigatorColumn[A] forSome { type A >: Null <: AnyRef}]

  def baseColumnNames(base: Coordinates): ColumnsList = List(IdColumn("Id"), RAColumn("RA"), DECColumn("Dec"), DistanceColumn(base, "Dist. [arcmin]"))
  val pmColumns: ColumnsList  = List(PMRAColumn("µ RA"), PMDecColumn("µ Dec"))
  val magColumns = MagnitudeBand.all.map(MagnitudeColumn)

  /**
   * Data model for the main table of the catalog navigator
   */
  case class TargetsModel(base: Coordinates, targets: List[SiderealTarget]) extends AbstractTableModel {
    // Available columns from the list of targets
    val columns:ColumnsList = {
      val bandsInTargets = targets.flatMap(_.magnitudes).map(_.band).distinct
      val hasPM = targets.exists(_.properMotion.isDefined)
      val pmCols = if (hasPM) pmColumns else Nil
      val magCols = magColumns.filter(m => bandsInTargets.contains(m.band))

      baseColumnNames(base) ::: pmCols ::: magCols
    }

    override def getRowCount = targets.length

    override def getColumnCount = columns.size

    override def getColumnName(column: Int): String =
      columns(column).title

    override def getValueAt(rowIndex: Int, columnIndex: Int):AnyRef =
      targets.lift(rowIndex).flatMap { t =>
        columns.lift(columnIndex) >>= {_.render(t)}
      }.orNull

    override def getColumnClass(columnIndex: Int): Class[_] = columns(columnIndex).clazz

    def rendererComponent(value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Option[Component]= {
      columns.lift(column).flatMap { c =>
        c.displayValue(value).map(new Label(_) {
          horizontalAlignment = Alignment.Right
        })
      }
    }
  }
}
