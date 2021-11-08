package edu.gemini.phase2.template.factory.impl.niri

import edu.gemini.spModel.gemini.niri.{SeqConfigNIRI, InstNIRI}
import edu.gemini.spModel.gemini.niri.Niri._
import edu.gemini.spModel.gemini.niri.blueprint.SpNiriBlueprint
import edu.gemini.phase2.template.factory.impl._
import edu.gemini.pot.sp.{ISPGroup, ISPObservation}
import scala.collection.JavaConverters._
import InstNIRI.{READ_MODE_PROP, FILTER_PROP, EXPOSURE_TIME_PROP, COADDS_PROP}
import edu.gemini.spModel.seqcomp.{SeqRepeatDarkObs, SeqConfigComp}
import edu.gemini.spModel.core.SPProgramID

trait NiriBase extends GroupInitializer[SpNiriBlueprint] with TemplateDsl with AltairSupport {

  val program = "NIRI PHASE I/II MAPPING BPS"
  val seqConfigCompType = SeqConfigNIRI.SP_TYPE

  implicit def pimpInst(obs:ISPObservation) = new {

    val ed = ObservationEditor[InstNIRI](obs, instrumentType, SeqConfigNIRI.SP_TYPE)

    def setFilter(d:Filter):Either[String, Unit] =
      ed.updateInstrument(_.setFilter(d))

    def setFilters(lst:Iterable[Filter]):Either[String, Unit] = for {
      _ <- lst.headOption.toRight("One or more filters must be specified.").right
      _ <- setFilter(lst.head).right
      _ <- ed.iterateFirst(InstNIRI.FILTER_PROP.getName, lst.toList).right
    } yield ()

    def setDisperser(d:Disperser):Either[String, Unit] = ed.updateInstrument(_.setDisperser(d))
    def setMask(d:Mask):Either[String, Unit] = ed.updateInstrument(_.setMask(d))
    def setExposureTime(d:Double):Either[String, Unit] = ed.updateInstrument(_.setExposureTime(d))
    def setCoadds(d:Int):Either[String, Unit] = ed.updateInstrument(_.setCoadds(d))
    def setReadMode(d:ReadMode):Either[String, Unit] = ed.updateInstrument(_.setReadMode(d))
    def setWellDepth(d:WellDepth):Either[String, Unit] = ed.updateInstrument(_.setWellDepth(d))
    def filter:Either[String, Filter] = ed.instrumentDataObject.right.map(_.getFilter)
    def camera:Either[String, Camera] = ed.instrumentDataObject.right.map(_.getCamera)

  }

  // Local imports


  // HACK: override superclass initialize to hang onto db reference
  var db:Option[TemplateDb] = None
  override def initialize(db:TemplateDb, pid: SPProgramID):Maybe[ISPGroup] =
    try {
      this.db = Some(db)
      super.initialize(db, pid)
    } finally {
      this.db = None
    }

  def attempt[A](a: => A) = tryFold(a) {e =>
    e.printStackTrace()
    e.getMessage
  }

  // DSL Setters
  def setFilter = Setter[Filter](sys.error("None"))(_.setFilter(_))
  def setFilters = Setter(blueprint.filters.asScala)(_.setFilters(_))
  def setAltair = AltairSetter(blueprint.altair)

  def setExposuresAndCoadds(map:Map[Filter, Exposure])(o:ISPObservation):Maybe[Unit] = {

    // Given a filter and camera, return the exposure and coadds
    def ec(f:Filter, c:Camera):Maybe[(Double, Int)] =
      for {
        e <- map.get(f).toRight("No exposure data found for filter %s".format(f)).right
        ec <- e.exposureAndCoadds(c).right
      } yield ec

    // Given a step, return a new step with the exposure and coadds set (bsed on filter and camera)
    def modifyStep(s:Map[String, Any]):Maybe[Map[String, Any]] =
      for {
        f <- s.get(FILTER_PROP.getName).toRight("No filter found.").right.map(_.asInstanceOf[Filter]).right
        //    c <- s.get(CAMERA_PROP.getName).toRight("No camera found.").right.map(_.asInstanceOf[Camera]).right
        c <- o.camera.right // set in static component, not iterator
        ec <- ec(f, c).right
      } yield s + (EXPOSURE_TIME_PROP.getName -> ec._1) +
                  (COADDS_PROP.getName -> ec._2) +
                  (READ_MODE_PROP.getName -> readModeForExposure(ec._1))


    // Do it!
    for {

      // Static component
      f <- o.filter.right
      c <- o.camera.right
      ec <- ec(f, c).right
      _ <- o.setExposureTime(ec._1).right
      _ <- o.setCoadds(ec._2).right
      _ <- o.setReadMode(readModeForExposure(ec._1)).right

      // Sequence
      _ <- o.ed.modifySeq(modifyStep _).right

    } yield ()

  }

  def createDarkSequences(sci:Seq[Int])(o:ISPObservation):Maybe[Unit] = {

    // # Dark exposures
    // IN DAY OBSERVATIONS:
    // FOR EACH UNIQUE COMBINATION OF EXPOSURE TIME AND COADD in SCI,
    // create a NIRI Sequence with a Manual Dark beneath. The EXPOSURE TIME
    // and COADDs are set in the Dark component. The READ MODE is set in the
    // iterator, see below, based on the exposure time. One iterator is
    // present in the BP libraries, more may be added.
    //
    // # Dark example
    // # JHK f/6 imaging
    // # J: 60sec, 1 coadd => Read Mode = Low Background (1-2.5um: Faint Object...)
    // # H: 15sec, 4 coadds => Read Mode = Medium Background (1-2.5um: JHK and ...)
    // # K: 30sec, 2 coadds => Read Mode = Medium Background (3-5um: Imaging/Spect...)
    // #
    // # In the calibration observation created from {4} the sequence is
    // # NIRI Flats
    // #   Filter
    // #      J
    // #      H
    // #      K
    // #      - Flat
    // # NIRI sequence
    // #    Read mode
    // #      1-2.5um: Faint Object Narrow-band Imaging/Spectroscopy
    // #      - Manual Dark (10x observe, 60 sec, 1 coadds)
    // # NIRI sequence
    // #    Read mode
    // #      1-2.5um: JHK and Bright Object Narrow-band Imaging/Spectroscopy
    // #      - Manual Dark (10x observe, 15 sec, 4 coadds)
    // #      - Manual Dark (10x observe, 30 sec, 2 coadds)

    val g = o.getParent.asInstanceOf[ISPGroup]

    // Distinct combos of exposure time and coadds
    val combos:Maybe[List[(Double, Int)]] = for {
      os <- sci.mapM(g(_)).right
      itss <- os.mapM(_.ed.instrumentIterators).right
    } yield {
      (for {
        its <- itss
        it <- its
        dobj = it.getDataObject.asInstanceOf[SeqConfigComp]
        step <- transpose1[String, Any](toMap(dobj.getSysConfig))
      } yield (step(EXPOSURE_TIME_PROP.getName).asInstanceOf[Double], step(COADDS_PROP.getName).asInstanceOf[Int])).distinct
    }

    // Given a combo, add the appopriate sequence
    def addDark(ex:Double, co:Int):Maybe[Unit] =
      attempt {

        val seq = o.getSeqComponent
        val fact = db.get.odb.getFactory

        // Ok, add the NIRI sequence first, setting the read mode as we go
        val niriSeq = fact.createSeqComponent(o.getProgram, SeqConfigNIRI.SP_TYPE, null)
        val niriDataObj = new SeqConfigNIRI
        val niriSysConf = niriDataObj.getSysConfig
        val niriParams = Map(READ_MODE_PROP.getName -> readModeForExposure(ex)).mapValues(List(_))
        niriSysConf.removeParameters()
        niriSysConf.putParameters(toParams(niriParams))
        niriDataObj.setSysConfig(niriSysConf)
        niriSeq.setDataObject(niriDataObj)
        seq.addSeqComponent(niriSeq)

        // Now add the dark
        val darkSeq = fact.createSeqComponent(o.getProgram, SeqRepeatDarkObs.SP_TYPE, null)
        val darkDataObj = new SeqRepeatDarkObs
        darkDataObj.setStepCount(10)
        darkDataObj.setExposureTime(ex)
        darkDataObj.setCoaddsCount(co)
        darkSeq.setDataObject(darkDataObj)
        niriSeq.addSeqComponent(darkSeq)

      }

    for {
      cs <- combos.right
      _ <- cs.mapM((addDark _).tupled).right
    } yield ()

  }

  def setWellDepth(o:ISPObservation):Maybe[Unit] = for {
    f <- o.filter.right
    _ <- o.setWellDepth(wellDepthForFilter(f)).right
  } yield ()

  // IF Exptime >= 45s:   SET  Read Mode = Low Background
  // ELSEIF Exptime >= 1.0s:  SET Read Mode = Medium Background
  // ELSE:                 SET Read Mode = High Background
  def readModeForExposure(ex:Double) =
         if (ex >= 45.0) ReadMode.IMAG_SPEC_NB
    else if (ex >= 1.0)  ReadMode.IMAG_1TO25
    else                 ReadMode.IMAG_SPEC_3TO5

  // # Well depth
  // IF WAVE (see NIRI_exptimes.xls) > 3micron (equally FILTER(S) includes H20
  // Ice, hydrocarbon, L(prime), Br(alpha) cont, Br(alpha), M(prime)): SET
  // Deep well (3-5 um)
  // ELSE SET Shallow Well (1-2.5 um)  (default)
  def wellDepthForFilter(f:Filter):WellDepth =
    if (f.getWavelength >= 3.0) WellDepth.DEEP else WellDepth.SHALLOW

  case class Exposure(f6:(Double, Int), f14:(Double, Int), f32:(Double, Int)) {
    def exposureAndCoadds(c:Camera):Maybe[(Double, Int)] = c match {
      case Camera.F6 => Right(f6)
      case Camera.F14 => Right(f14)
      case Camera.F32 => Right(f32)
      case Camera.F32_PV => Right(f32)
      case _ => Left("No exposures/coadds found for camera " + c)
    }
  }


}
