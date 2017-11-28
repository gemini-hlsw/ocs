package edu.gemini.qv.plugin.table.renderer

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.qpt.shared.sp.{Band, Obs, Prog}
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, GmosSouthType}
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import edu.gemini.spModel.gemini.michelle.MichelleParams
import edu.gemini.spModel.gemini.nici.NICIParams
import edu.gemini.spModel.gemini.niri.Niri
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{ElevationConstraintType, SkyBackground, WaterVapor}
import edu.gemini.spModel.gemini.trecs.TReCSParams
import edu.gemini.spModel.`type`.DisplayableSpType
import java.awt.Color
import java.util.UUID
import javax.swing.JTextPane
import javax.swing.text.{MutableAttributeSet, StyleConstants}

import edu.gemini.shared.util.DateTimeUtils

import scala.collection.JavaConversions._
import scala.swing.{Component, TextArea}

/**
 * Show all observation ids in the table cell.
 */
object ObservationsRenderer extends CellRenderer {
  def label = "Observations"
  def createCell(obs: Set[Obs]) = new ObsCell(obs)

  class ObsCell(obs: Set[Obs]) extends TextArea with Cell {
    obs.foreach(o => append(o.getObsId + "\n"))
  }
}

/**
 * Show all program ids in the table cell.
  */
object ProgramsRenderer extends CellRenderer {
  def label = "Programs"
  def createCell(obs: Set[Obs]) = new ProgCell(obs)

  class ProgCell(obs: Set[Obs]) extends TextArea with Cell {
    private val programs: Set[Prog] = obs.map(_.getProg)
    programs.foreach(p => append(p.getProgramId.stringValue() + "\n"))
  }
}

/**
 * Show all "encoded" observation ids in the table cell.
 * This renderer is based on the so-called "Big Sheet" that has been used in the past at GN for planning
 * purposes.
 * Note: In order to be able to use different colors etc. in the cell we have to use a TextPane instead
 * of a TextArea.
 */
object EncodedObservationsRenderer extends CellRenderer {
  def label = "Encoded Observations (Big Sheet)"
  def createCell(obs: Set[Obs]) = new EncObsCell(obs)

  class EncObsCell(obs: Set[Obs]) extends TextPane with Cell {

    tooltip = "Click to subselect this set of observations."

    val doc = styledDocument
    val styleMap = styleByProgram(obs)
    obs.foreach(o => {
      doc.insertString(doc.getLength, s"${encObs(o)}\n", styleMap(o.getProg))
    })
    doc.insertString(doc.getLength, "\n", addStyle(Color.black))


    // == some helpers for encoding

    /** Encodes the given observation as a string. */
    private def encObs(o: Obs): String =
      new StringBuilder().
        append(encObsId(o)).append("/").
        append(encInstrument(o)).append("/").
        append(encTime(o)).append("/").
        append(encConfig(o)).append("/").
        append(encBG(o)).
        append(encWV(o)).
        append(encTW(o)).
        append(encEC(o)).
        toString()

    /** Encodes the observation id (i.e. program number and obs id) */
    private def encObsId(o: Obs): String = {
      val pid = o.getProg.getStructuredProgramId
      Option(pid.getSemester).map(_.takeRight(2)).getOrElse("") +
        pid.getType +
        pid.getNumber + "-" +
        o.getObsNumber
    }

    /** Encodes instruments as a string. */
    private def encInstrument(o: Obs): String = o.getInstrumentComponentType match {
      case SPComponentType.INSTRUMENT_NIFS => if (o.getAO) "FA" else "F"
      case SPComponentType.INSTRUMENT_MICHELLE => "M"
      case SPComponentType.INSTRUMENT_NIRI => if (o.getAO) "NA" else "N"
      case SPComponentType.INSTRUMENT_GMOS => "G"
      case SPComponentType.INSTRUMENT_GMOSSOUTH => "G"
      case SPComponentType.INSTRUMENT_GNIRS => if (o.getAO) "SA" else "S"
      case SPComponentType.INSTRUMENT_NICI => "C"
      case SPComponentType.INSTRUMENT_FLAMINGOS2 => "F2"
      case SPComponentType.INSTRUMENT_GSAOI => "MC"
      case SPComponentType.INSTRUMENT_TEXES => "X"
      case SPComponentType.INSTRUMENT_TRECS => "T"
      case SPComponentType.INSTRUMENT_VISITOR => "V"
      case _ => "?"
    }

    /** Encoded the observations planned time. */
    private def encTime(o: Obs): String = DateTimeUtils.msToHHMM(o.getRemainingTime)

    /**
     * Encodes an instrument configuration.
     * Ugly collection of special and heuristic and ad-hoc rules for converting filter, disperser and FPU names
     * into a (hopefully) unique short form. Doing this based on Strings may seem counter intuitive, but as it turns
     * out implementing all of the rules below based on the actual enum values would be much more elaborate.
     * @param o
     * @return
     */
    private def encConfig(o: Obs): String = {
      def displayStr(values: Set[AnyRef]): String =
        if (values.isEmpty) ""
        else values.head match {
          case d: DisplayableSpType => d.displayValue
          case d => d.toString
        }

      // encode filter, if present (first 5 characters without white spaces)
      def filterStr(filters: Set[AnyRef]): String =
        displayStr(filters).
          replaceAll("""^CO 2""", "CO2").  // don't loose the 2 and 3 of CO2 and CO3
          replaceAll("""^CO 3""", "CO3").  // when cleaning space + everything after
          replaceAll("""\s.*""", "").      // get rid of everything after space
          replaceAll("""_.*""", "").       // get rid of everything after underscore
          replaceAll(""":.*""", "").       // get rid of everything after colon
          take(5)

      // encode disperser, if present (first 5 characters without white spaces)
      def disperserStr(dispersers: Set[AnyRef]): String =
        displayStr(dispersers).
          replaceAll("""^R=\d\d\d\d """, ""). // special rule for F2
          replaceAll("""^Echelon""", "E").    // special rule for Texes
          replaceAll("""l/mm grating""", ""). // remove some noise..
          replaceAll("""Grating""", "").      // remove some noise..
          replaceAll("""\s""", "").           // get rid of all white spaces
          take(5).                            // take first 5 of whatever is left
          replaceAll("""_""", "")             // remove some noise..


      // encode fpu, if present; special care needed for custom masks
      def fpuStr(fpus: Set[AnyRef]): String = {
        val str: String = displayStr(fpus)
        if (o.getCustomMask != null && !o.getCustomMask.isEmpty) {
          "MOS-" + o.getCustomMask.takeRight(2)
        } else {
          val a =
            if (str.contains("ongslit")) "LS"
            else if (str.contains("Occulting disk")) "OD"
            else if (str.contains("Occulting Disk")) "OD"
            else if (str.contains("N and S")) "NS"
            else ""
          a ++ str.
            replaceAll("""[L|l]ongslit""", ""). // filter out "noise", i.e. anything that does not really give information
            replaceAll("""Occulting [D|d]isk""", "").
            replace("N and S", "").
            replace("arcsec", "").
            replaceAll("""\s""", "").           // get rid of all white spaces
            take(4)                             // take first 4 of whatever is left
        }
       }

      // figure out if the instrument in this configuration is set up for imaging (depends on FPU)
      def isImaging: Boolean = o.getInstrumentComponentType match {
        case SPComponentType.INSTRUMENT_NIFS =>
          false
        case SPComponentType.INSTRUMENT_MICHELLE =>
          o.getFocalPlanUnits.contains(MichelleParams.Mask.MASK_IMAGING)
        case SPComponentType.INSTRUMENT_NIRI =>
          o.getFocalPlanUnits.contains(Niri.Mask.MASK_IMAGING)
        case SPComponentType.INSTRUMENT_GMOS =>
          o.getFocalPlanUnits.contains(GmosNorthType.FPUnitNorth.FPU_NONE)
        case SPComponentType.INSTRUMENT_GMOSSOUTH =>
          o.getFocalPlanUnits.contains(GmosSouthType.FPUnitSouth.FPU_NONE)
        case SPComponentType.INSTRUMENT_GNIRS =>
          o.getFocalPlanUnits.contains(GNIRSParams.SlitWidth.ACQUISITION)
        case SPComponentType.INSTRUMENT_NICI =>
          o.getFocalPlanUnits.contains(NICIParams.FocalPlaneMask.CLEAR)
        case SPComponentType.INSTRUMENT_FLAMINGOS2 =>
          o.getFocalPlanUnits.contains(Flamingos2.FPUnit.FPU_NONE)
        case SPComponentType.INSTRUMENT_GSAOI =>
          true
        case SPComponentType.INSTRUMENT_TEXES =>
          false
        case SPComponentType.INSTRUMENT_TRECS =>
          val imagingMasks: Set[AnyRef] = Set(
            TReCSParams.Mask.MASK_IMAGING,
            TReCSParams.Mask.MASK_IMAGING_W
          )
          !o.getFocalPlanUnits.intersect(imagingMasks).isEmpty
        case SPComponentType.INSTRUMENT_VISITOR =>
          false
        case _ => false
      }

      // finally: for imaging just show the filter, otherwise show disperser and FPU
      if (isImaging)
        "img-" ++ filterStr(o.getFilters.toSet)
      else
        disperserStr(o.getDispersers.toSet) + "/" +  fpuStr(o.getFocalPlanUnits.toSet)

    }

    /** Encodes sky background conditions. */
    private def encBG(o: Obs) = o.getSkyBackground match {
      case SkyBackground.ANY => "B"
      case SkyBackground.PERCENT_80 => "G"
      case _ => "D"
    }

    /** Encodes water vapor conditions. */
    private def encWV(o: Obs) = o.getWaterVapor match {
      case WaterVapor.ANY => ""
      case WaterVapor.PERCENT_80 => "/W8"
      case WaterVapor.PERCENT_50 => "/W5"
      case WaterVapor.PERCENT_20 => "/W2"
    }

    /** Encodes timing windows. */
    private def encTW(o: Obs) = if (o.getTimingWindows.isEmpty) "" else "/TW"

    /** Encodes elevation constraints. */
    private def encEC(o: Obs) = if (o.getElevationConstraintType == ElevationConstraintType.NONE) "" else "/EC"

    /** Define a custom color coding for the big sheet based on the different bands. */
    private def styleByProgram(observations: Set[Obs]): Map[Prog, MutableAttributeSet] = {
      import Band._
      def styleFor(p: Prog) = (p.getBandEnum, p.getUsedTime) match {
        case (Band1, _)      => addStyle(QvGui.Red)
        case (Band2, _)      => addStyle(QvGui.Green)
        case (Band3, 0)      => addStyle(Color.cyan)
        case (Band3, _)      => addStyle(QvGui.Blue, bold = true)     // started B3 programs are of special interest
        case (Band4, _)      => addStyle(Color.black)
        case (Undefined, _)  => addStyle(Color.yellow.darker())
      }
      observations.map(_.getProg).map(p => p -> styleFor(p)).toMap
    }

  }

  /** A silly little wrapper to allow usage of java JTextPane as a Scala Swing component. */
  class TextPane extends Component {
    override lazy val peer = new JTextPane
    def styledDocument = peer.getStyledDocument
    def addStyle(color: Color, bold: Boolean = false) = {
      val style = peer.addStyle(UUID.randomUUID().toString, null)
      StyleConstants.setForeground(style, color)
      StyleConstants.setBold(style, bold)
      style
    }
  }


}
