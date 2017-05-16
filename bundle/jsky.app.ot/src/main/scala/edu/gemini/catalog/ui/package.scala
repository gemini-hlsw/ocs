package edu.gemini.catalog.ui

import javax.swing.JTable
import javax.swing.table._

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.api.{AgsAnalysis, AgsGuideQuality, AgsRegistrar, AgsStrategy}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.api._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{InstGmosSouth, InstGmosNorth}
import edu.gemini.spModel.gemini.gnirs.{GNIRSConstants, InstGNIRS}
import edu.gemini.spModel.gemini.gpi.Gpi
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.michelle.InstMichelle
import edu.gemini.spModel.gemini.nici.InstNICI
import edu.gemini.spModel.gemini.nifs.InstNIFS
import edu.gemini.spModel.gemini.niri.InstNIRI
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.gemini.phoenix.InstPhoenix
import edu.gemini.spModel.gemini.texes.InstTexes
import edu.gemini.spModel.gemini.trecs.InstTReCS
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.spModel.guide.ValidatableGuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.{PosAngleConstraint, PosAngleConstraintAware}
import edu.gemini.pot.ModelConverters._

import scala.language.existentials
import scala.swing.{Alignment, Label, Component}
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
 * Locally describe an ags strategy including its limits and the query that would trigger
 */
case class SupportedStrategy(strategy: AgsStrategy, query: List[CatalogQuery], altairMode: Option[AltairParams.Mode])

object SupportedStrategy {
  implicit val order:Order[SupportedStrategy] = Order.orderBy(_.strategy.key.id)
  implicit val ordering:scala.Ordering[SupportedStrategy] = order.toScalaOrdering
}

/**
 * Describes the observation used to do a Guide Star Search, it is pretty much a copy of ObsContext but it can be built
 * out of the query fields on the CQT
 */
case class ObservationInfo(ctx: Option[ObsContext],
                           objectName: Option[String],
                           baseCoordinates: Option[Coordinates],
                           instrument: Option[SPComponentType],
                           strategy: Option[SupportedStrategy],
                           validStrategies: List[SupportedStrategy],
                           conditions: Option[Conditions],
                           positionAngle: Angle,
                           allowPAFlip: Boolean,
                           offsets: Set[Offset],
                           catalog: CatalogName,
                           mt: MagnitudeTable) {
  def catalogQuery:List[CatalogQuery] = validStrategies.collect {
      case SupportedStrategy(s, query, _) if s == strategy => query
    }.flatten

  /**
   * Attempts to find the guide probe for the selected strategy
   */
  def guideProbe: Option[ValidatableGuideProbe] =
    strategy.flatMap(_.strategy.guideProbes.headOption.collect {
      case v: ValidatableGuideProbe => v
    })

  /**
   * An obscontext is required for guide quality calculation. The method below will attempt to create a context out of the information on the query form
   * TODO: Review if this is the best way to proceed
   */
  val toContext: Option[ObsContext] = ctx.orElse {
    val inst = instrument.collect {
      case SPComponentType.INSTRUMENT_FLAMINGOS2 => new Flamingos2()
      case SPComponentType.INSTRUMENT_GMOS       => new InstGmosNorth()
      case SPComponentType.INSTRUMENT_GMOSSOUTH  => new InstGmosSouth()
      case SPComponentType.INSTRUMENT_GNIRS      => new InstGNIRS()
      case SPComponentType.INSTRUMENT_GPI        => new Gpi()
      case SPComponentType.INSTRUMENT_GSAOI      => new Gsaoi()
      case SPComponentType.INSTRUMENT_MICHELLE   => new InstMichelle()
      case SPComponentType.INSTRUMENT_NICI       => new InstNICI()
      case SPComponentType.INSTRUMENT_NIFS       => new InstNIFS()
      case SPComponentType.INSTRUMENT_NIRI       => new InstNIRI()
      case SPComponentType.INSTRUMENT_PHOENIX    => new InstPhoenix()
      case SPComponentType.INSTRUMENT_TEXES      => new InstTexes()
      case SPComponentType.INSTRUMENT_TRECS      => new InstTReCS()
      case SPComponentType.INSTRUMENT_VISITOR    => new VisitorInstrument()
    }
    val site = instrument.collect {
      case SPComponentType.INSTRUMENT_VISITOR    => None // Setting the site as None will make the CatalogQueryTool select the site from the selected probe
      case _                                     => inst.flatMap(_.getSite.asScala.headOption)
    }
    (baseCoordinates |@| inst |@| conditions){ (c, i, cond) =>
      val target = new SPTarget(c.ra.toAngle.toDegrees, c.dec.toDegrees) <| {_.setName(~objectName)}
      val env = TargetEnvironment.create(target)
      // To calculate analysis of guide quality, it is required the site, instrument and conditions
      val altair = strategy.collect {
        case SupportedStrategy(_, _, Some(m)) => new InstAltair() <| {_.setMode(m)}
      }
      ObsContext.create(env, i, site.flatten.asGeminiOpt, cond, offsets.map(_.toOldModel).asJava, altair.orNull, JNone.instance()).withPositionAngle(positionAngle)
    }
  }
}

object ObservationInfo {
  val DefaultInstrument = SPComponentType.INSTRUMENT_VISITOR

  // Observation context loaded initially with default parameters
  val zero = new ObservationInfo(None, "".some, Coordinates.zero.some, DefaultInstrument.some, None, Nil, SPSiteQuality.Conditions.BEST.some, Angle.zero, false, Set.empty, UCAC4, ProbeLimitsTable.loadOrThrow())

  val InstList = List(
    Flamingos2.INSTRUMENT_NAME_PROP        -> SPComponentType.INSTRUMENT_FLAMINGOS2,
    InstGmosNorth.INSTRUMENT_NAME_PROP     -> SPComponentType.INSTRUMENT_GMOS,
    InstGmosSouth.INSTRUMENT_NAME_PROP     -> SPComponentType.INSTRUMENT_GMOSSOUTH,
    GNIRSConstants.INSTRUMENT_NAME_PROP    -> SPComponentType.INSTRUMENT_GNIRS,
    //Gpi.INSTRUMENT_NAME_PROP               -> SPComponentType.INSTRUMENT_GPI, GPI Doesn't have AGS Strategies defined
    InstMichelle.INSTRUMENT_NAME_PROP      -> SPComponentType.INSTRUMENT_MICHELLE,
    InstNICI.INSTRUMENT_NAME_PROP          -> SPComponentType.INSTRUMENT_NICI,
    InstNIFS.INSTRUMENT_NAME_PROP          -> SPComponentType.INSTRUMENT_NIFS,
    InstNIRI.INSTRUMENT_NAME_PROP          -> SPComponentType.INSTRUMENT_NIRI,
    InstPhoenix.INSTRUMENT_NAME_PROP       -> SPComponentType.INSTRUMENT_PHOENIX,
    InstTexes.INSTRUMENT_NAME_PROP         -> SPComponentType.INSTRUMENT_TEXES,
    InstTReCS.INSTRUMENT_NAME_PROP         -> SPComponentType.INSTRUMENT_TRECS,
    "Visitor"                              -> SPComponentType.INSTRUMENT_VISITOR
  )

  val InstMap = InstList.map(i => (i._2, i._1)).toMap

  /**
   * Converts an AgsStrategy to a simpler description to be stored in the UI model
   */
  def toSupportedStrategy(obsCtx: ObsContext, strategy: AgsStrategy, mt: MagnitudeTable):SupportedStrategy = {
    val queries = strategy.catalogQueries(obsCtx, mt)
    val mode = obsCtx.getAOComponent.asScalaOpt.collect {
      case a: InstAltair => a.getMode
    }
    SupportedStrategy(strategy, queries, mode)
  }

  def expandAltairModes(obsCtx: ObsContext): List[ObsContext] = obsCtx.getAOComponent.asScalaOpt match {
    case Some(i: InstAltair) => AltairParams.Mode.values().toList.map(m => obsCtx.withAOComponent(new InstAltair() <| {_.setMode(m)})) :+ obsCtx.withoutAOComponent()
    case _                   => List(obsCtx)
  }

  val PosAngleConstraints = Set(PosAngleConstraint.FIXED_180, PosAngleConstraint.PARALLACTIC_OVERRIDE)

  def apply(ctx: ObsContext, mt: MagnitudeTable):ObservationInfo = ObservationInfo(
    ctx.some,
    Option(ctx.getTargets.getAsterism).map(_.name),
    Option(ctx.getTargets.getAsterism).flatMap(_.getSkycalcCoordinates(ctx.getSchedulingBlockStart).asScalaOpt).map(_.toNewModel),
    Option(ctx.getInstrument.getType),
    AgsRegistrar.currentStrategy(ctx).map(toSupportedStrategy(ctx, _, mt)),
    expandAltairModes(ctx).flatMap(c => AgsRegistrar.validStrategies(c).map(toSupportedStrategy(c, _, mt))).sorted,
    ctx.getConditions.some,
    ctx.getPositionAngle,
    Option(ctx.getInstrument).collect{case p: PosAngleConstraintAware => PosAngleConstraints.contains(p.getPosAngleConstraint)}.getOrElse(false),
    ctx.getSciencePositions.asScala.map(_.toNewModel).toSet,
    UCAC4,
    mt)

}

/**
 * Represents a table column description
 * @tparam T The type of the column
 */
abstract class CatalogNavigatorColumn[T >: Null: Manifest] {
  def title: String

  def lens: PLens[Target, T]

  // Display the value in the table as String. values are Any in the table model
  def displayValue(t: Any): Option[String] = Option(t).map(_.toString)

  // Extract the data from the target via the lens
  def render(target: Target): Option[T] = lens.get(target)

  // Indicates the class of the column
  def clazz:Class[_] = manifest.runtimeClass

  // Returns the ordering for the column
  def ordering: scala.math.Ordering[T] // TODO Could we make this generic? implicitly[Ordering[T]] does not work :S
}

case class IdColumn(title: String) extends CatalogNavigatorColumn[String] {
  override val lens = Target.name.partial

  def ordering = implicitly[scala.math.Ordering[String]]
}

object GuidingQuality {
  implicit val analysisOrder:Order[AgsAnalysis] = Order.orderBy(_.quality)
  val paFlip = Angle.fromDegrees(180)

  // Calculate the guiding quality of the target, allowing for PA flipping
  def target2Analysis(info: Option[ObservationInfo], t: Target):Option[AgsAnalysis] =
    if (info.exists(_.allowPAFlip)) {
      // Note we use min, as AgsGuideQuality is better when the position on the index is lower
      target2Analysis(info, t, Angle.zero) min target2Analysis(info, t, paFlip)
    } else {
      target2Analysis(info, t, Angle.zero)
    }

  // Calculate the guiding quality of the target at a given PA
  private def target2Analysis(info: Option[ObservationInfo], t: Target, shift: Angle):Option[AgsAnalysis] = {
    (for {
      o                                           <- info
      s                                           <- o.strategy
      gp                                          <- o.guideProbe
      st @ SiderealTarget(_, _, _, _, _, _, _, _) = t
      ctx                                         <- o.toContext
    } yield s.strategy.analyze(ctx.withPositionAngle(ctx.getPositionAngle + shift), o.mt, gp, st)).flatten
  }
}

case class GuidingQuality(info: Option[ObservationInfo], title: String) extends CatalogNavigatorColumn[AgsGuideQuality] {
  val gf: SiderealTarget @?> AgsGuideQuality = PLens(t => GuidingQuality.target2Analysis(info, t).map(p => Store(q => sys.error("Not in use"), p.quality)))

  override val lens: Target @?> AgsGuideQuality = PLens(_.fold(
    PLens.nil.run,
    gf.run _ andThen (_.map(_.map(x => x: Target))),
    PLens.nil.run
  ))

  def ordering = implicitly[scala.math.Ordering[AgsGuideQuality]]
}

case class RAColumn(title: String) extends CatalogNavigatorColumn[RightAscension] {
  override val lens = Target.coords >=> Coordinates.ra.partial

  override def displayValue(t: Any) = Option(t).collect {
    case r: RightAscension => r.toAngle.formatHMS
  }

  def ordering = implicitly[scala.math.Ordering[RightAscension]]
}

case class DECColumn(title: String) extends CatalogNavigatorColumn[Declination] {
  override val lens = Target.coords >=> Coordinates.dec.partial

  override def displayValue(t: Any) = Option(t).collect {
    case d: Declination => d.formatDMS
  }

  def ordering = implicitly[scala.math.Ordering[Declination]]
}

case class DistanceColumn(base: Coordinates, title: String) extends CatalogNavigatorColumn[Angle] {
  val distance: Coordinates @> Angle = Lens(c => Store(r => c, Coordinates.difference(base, c).distance))

  override val lens = Target.coords >=> distance.partial

  override def displayValue(t: Any) = Option(t).collect {
    case a: Angle => f"${a.toArcmins}%.2f"
  }

  def ordering = implicitly[scala.math.Ordering[Angle]]
}

case class PMRAColumn(title: String) extends CatalogNavigatorColumn[RightAscensionAngularVelocity] {
  override val lens = Target.pm >=> ProperMotion.deltaRA.partial

  override def displayValue(t: Any) = Option(t).collect {
    case r: RightAscensionAngularVelocity => f"${r.velocity.masPerYear}%.2f"
  }

  def ordering = implicitly[scala.math.Ordering[RightAscensionAngularVelocity]]
}

case class PMDecColumn(title: String) extends CatalogNavigatorColumn[DeclinationAngularVelocity] {
  override val lens = Target.pm >=> ProperMotion.deltaDec.partial

  override def displayValue(t: Any) = Option(t).collect {
    case d: DeclinationAngularVelocity => f"${d.velocity.masPerYear}%.2f"
  }

  def ordering = implicitly[scala.math.Ordering[DeclinationAngularVelocity]]
}

case class MagnitudeColumn(band: MagnitudeBand) extends CatalogNavigatorColumn[Magnitude] {
  override val title = band.name
  // Lens from list of magnitudes to the band's magnitude if present
  val bLens: List[Magnitude] @?> Magnitude = PLens(ml => ml.find(_.band === band).map(m => Store(b => sys.error("read-only lens"), m)))
  override val lens = Target.magnitudes >=> bLens

  override def displayValue(t: Any) = Option(t).collect {
    case m: Magnitude => f"${m.value}%.2f"
  }

  def ordering = implicitly[scala.math.Ordering[Magnitude]]
}

/**
 * Data model for the main table of the catalog navigator
 */
case class TargetsModel(info: Option[ObservationInfo], base: Coordinates, radiusConstraint: RadiusConstraint, targets: List[SiderealTarget]) extends AbstractTableModel {
  // Required to give limits to the existential type list
  type ColumnsList = List[CatalogNavigatorColumn[A] forSome { type A >: Null <: AnyRef}]

  def baseColumnNames(base: Coordinates): ColumnsList = List(GuidingQuality(info, ""), IdColumn("Id"), RAColumn("RA"), DECColumn("Dec"), DistanceColumn(base, "Dist. [arcmin]"))
  val pmColumns: ColumnsList  = List(PMRAColumn("µ RA"), PMDecColumn("µ Dec"))
  val magColumns = MagnitudeBand.all.map(MagnitudeColumn)

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
    ~columns.lift(column).map(_.title)

  override def getValueAt(rowIndex: Int, columnIndex: Int):AnyRef =
    targets.lift(rowIndex).flatMap { t =>
      columns.lift(columnIndex) >>= {_.render(t)}
    }.orNull

  override def getColumnClass(columnIndex: Int): Class[_] = columns(columnIndex).clazz

  // The control can be reused by the renderer
  val label = new Label("")

  def rendererComponent(value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int, table: JTable): Option[Component]= {
    columns.lift(column).flatMap { c =>
      c.displayValue(value).map { v =>
        label.text = v
        label.horizontalAlignment = Alignment.Right

        // Required to set the background
        label.opaque = true

        if (isSelected) {
          label.background = table.getSelectionBackground
          label.foreground = table.getSelectionForeground
        } else {
          label.background = table.getBackground
          label.foreground = table.getForeground
        }
        label
      }
    }
  }

  def renderAt(row: Int, column: Int):Option[String] = {
    targets.lift(row).flatMap { t =>
      columns.lift(column) >>= {c => c.render(t).flatMap(c.displayValue)}
    }
  }

  // Returns a Table Sorter depending on the available columns
  def sorter =
    new TableRowSorter[TargetsModel](this) <| { _.toggleSortOrder(0) } <| { sorter =>
      columns.zipWithIndex.foreach {
        case (column, i)        => sorter.setComparator(i, column.ordering)
      }
    } <| { _.sort() }
}

