package edu.gemini.spModel.io.impl.migration.to2015B

import java.io.File
import java.util.UUID

import edu.gemini.pot.sp.{SPComponentType, ISPObservation, ISPRootNode, ISPProgram}
import edu.gemini.pot.util.POTUtil
import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.io.impl.PioSpXmlParser
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.shared.util.immutable.ScalaConverters._

import scala.collection.JavaConverters._
import scalaz._, Scalaz._
import scalaz.effect._

// Gather some stats about the target use
object TargetSurvey extends SafeApp {

  val factory = POTUtil.createFactory(UUID.fromString("D1DA801D-7065-49BE-BF47-915F69933444"))
  val parser  = new PioSpXmlParser(factory)

  implicit class ISPRootNodeOps(r: ISPRootNode) {
    def asProgram: Option[ISPProgram] =
      r match {
        case p: ISPProgram => Some(p)
        case _             => None
      }
  }

  type Stats = Map[(String, String), (Int, Map[Magnitude.Band, Int])]

  def magnitudeMap(t: SPTarget): Map[Magnitude.Band, Int] =
    t.getMagnitudes.asScalaList.map(_.getBand).strengthR(1).toMap

  def examineTarget(t: SPTarget): Stats =
    Map((t.getTarget.getClass.getSimpleName, t.getCoordSysAsString) -> ((1, magnitudeMap(t))))

  def examineTargetEnv(e: TargetEnvironment): Stats =
    e.getTargets.asScala.toList.foldMap(examineTarget)

  def examineObservation(o: ISPObservation): Stats =
    o.getObsComponents.asScala.toList.fproduct(_.getType).collect {
      case (c, SPComponentType.TELESCOPE_TARGETENV) =>
        c.getDataObject.asInstanceOf[TargetObsComp].getTargetEnvironment
    } .foldMap(examineTargetEnv)

  def examineProgram(p: ISPProgram): Stats =
    p.getAllObservations.asScala.toList.foldMap(examineObservation)

  def examineFile(f: File): IO[Stats] =
    for {
      _ <- IO.putStrLn(f.getPath)
      r <- IO(parser.parseDocument(f))
    } yield r.asProgram.foldMap(examineProgram)

  def xmlFiles(dir: File): IO[List[File]] =
    IO(dir.listFiles.toList.filter(_.getName.toLowerCase.endsWith(".xml")))

  override def runc: IO[Unit] =
    for {
      fs <- xmlFiles(new File("/Users/rnorris/Scala/ocs-arch/20140922-0730"))
      ss <- fs.traverseU(examineFile)
      _  <- ss.suml.toList.map(_.toString).traverseU(IO.putStrLn)
    } yield ()

}

