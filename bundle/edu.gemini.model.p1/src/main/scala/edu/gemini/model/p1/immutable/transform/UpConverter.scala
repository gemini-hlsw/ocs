package edu.gemini.model.p1.immutable.transform

import xml.{Text, Node => XMLNode}
import scalaz._
import Scalaz._
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.{mutable => M}
import edu.gemini.model.p1.immutable.transform.XMLConverter._

import scala.xml.transform.BasicTransformer

case class ConversionResult(transformed: Boolean, from: Semester, changes: Seq[String], root: XMLNode)

object UpConverter {
  type UpConversionResult = ValidationNel[String, ConversionResult]

  private val VersionToSemesterRegex = """(\d\d\d\d)\.(1|2)\.(\d*)""".r // Extract semester from the proposal version

  private def toSemester(version: String):Semester = version match {
    case "2013.2.1"                        => Semester(2013, SemesterOption.B)
    case "1.0.14"                          => Semester(2013, SemesterOption.A)
    case "1.0.0"                           => Semester(2012, SemesterOption.B)
    case VersionToSemesterRegex(s, "1", _) => Semester(s.toInt, SemesterOption.A)
    case VersionToSemesterRegex(s, "2", _) => Semester(s.toInt, SemesterOption.B)
    case _                                 => Semester.current
  }

  def upConvert(node: XMLNode):UpConversionResult = {
    def toConversionResult(result: StepResult) = {
      val originalVersion = (node \ "@schemaVersion").text
      ConversionResult(originalVersion != Proposal.currentSchemaVersion, toSemester(originalVersion), result.change, result.node.head)
    }
    convert(node).map(toConversionResult)
  }

  // Sequence of conversions for proposals from a given semester. Cleaned up for 2017A since older conversion multiples to increment version no longer necessary.
  val from2023B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, LastStepConverter(Semester(2023, SemesterOption.B)))
  val from2023A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, LastStepConverter(Semester(2023, SemesterOption.A)))
  val from2022B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, LastStepConverter(Semester(2022, SemesterOption.B)))
  val from2022A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, LastStepConverter(Semester(2022, SemesterOption.A)))
  val from2021B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, LastStepConverter(Semester(2021, SemesterOption.B)))
  val from2021A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, LastStepConverter(Semester(2021, SemesterOption.A)))
  val from2020B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, LastStepConverter(Semester(2020, SemesterOption.B)))
  val from2020A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A ,SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, LastStepConverter(Semester(2020, SemesterOption.A)))
  val from2019B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, LastStepConverter(Semester(2019, SemesterOption.B)))
  val from2019A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, LastStepConverter(Semester(2019, SemesterOption.A)))
  val from2018B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, LastStepConverter(Semester(2018, SemesterOption.B)))
  val from2018A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, LastStepConverter(Semester(2018, SemesterOption.A)))
  val from2017B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, LastStepConverter(Semester(2017, SemesterOption.B)))
  val from2017A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2017, SemesterOption.A)))
  val from2016B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2016, SemesterOption.B)))
  val from2016A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2016, SemesterOption.A)))
  val from2015B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015BTo2016A, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2015, SemesterOption.B)))
  val from2015A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015BTo2016A, SemesterConverter2015ATo2015B, SemesterConverter2017ATo2017B,   LastStepConverter(Semester(2015, SemesterOption.A)))
  val from2014B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A ,SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015ATo2015B, SemesterConverter2014BTo2015A, SemesterConverter2014BTo2014BSV, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2014, SemesterOption.B)))
  val from2014A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015ATo2015B, SemesterConverter2014BTo2015A, SemesterConverter2014BTo2014BSV, SemesterConverter2014ATo2014B, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2014, SemesterOption.A)))
  val from2013B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015ATo2015B, SemesterConverter2014BTo2015A, SemesterConverter2014BTo2014BSV, SemesterConverter2013BTo2014A, SemesterConverter2014ATo2014B, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2013, SemesterOption.B)))
  val from2013A:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015ATo2015B, SemesterConverter2014BTo2015A, SemesterConverter2014BTo2014BSV, SemesterConverter2013ATo2013B, SemesterConverter2013BTo2014A, SemesterConverter2014ATo2014B, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2013, SemesterOption.A)))
  val from2012B:List[SemesterConverter] = List(SemesterConverterToCurrent, SemesterConverter2023BTo2024A, SemesterConverter2023ATo2023B, SemesterConverter2022BTo2023A, SemesterConverter2022ATo2022B, SemesterConverter2021BTo2022A, SemesterConverter2021ATo2021B, SemesterConverter2020BTo2021A, SemesterConverter2020ATo2020B, SemesterConverter2019BTo2020A, SemesterConverter2019ATo2019B, SemesterConverter2018BTo2019A, SemesterConverter2018ATo2018B, SemesterConverter2017BTo2018A, SemesterConverter2016BTo2017A, SemesterConverter2016ATo2016B, SemesterConverter2015ATo2015B, SemesterConverter2014BTo2015A, SemesterConverter2014BTo2014BSV, SemesterConverter2012BTo2013A, SemesterConverter2013ATo2013B, SemesterConverter2013BTo2014A, SemesterConverter2014ATo2014B, SemesterConverter2017ATo2017B, LastStepConverter(Semester(2012, SemesterOption.B)))

  /**
   * Converts one version of a proposal node to the next
   *
   * @param node Root node of the proposal
   * @return The result of the operation containing either an error or a success that contains a list of change description and a converted XML
   */
  def convert(node: XMLNode):Result = node match {
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == Proposal.currentSchemaVersion =>
      StepResult(Nil, node).successNel[String]
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2023.2.[12]") =>
      from2023B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2023.1.[12]") =>
      from2023A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2022.2.[12]") =>
      from2022B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2022.1.[123]") =>
      from2022A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2021.2.1") =>
      from2021B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2021.1.[123]") =>
      from2021A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2020.2.1")   =>
      from2020B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2020.1.1")   =>
      from2020A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2019.2.[12]")   =>
      from2019B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2019.1.[12]")   =>
      from2019A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2018.2.[12]")   =>
      from2018B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2018.1.1"                    =>
      from2018A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2017.2.[12]")   =>
      from2017B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2017.1.1"                    =>
      from2017A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2016.2.[12]")   =>
      from2016B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2016.1.1"                    =>
      from2016A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2015.2.1"                    =>
      from2015B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2015.1.[12]")   =>
      from2015A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text.matches("2014.2.[12]")   =>
      from2014B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2014.1.2"                    =>
      from2014A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2014.1.1"                    =>
      from2014A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "2013.2.1"                    =>
      from2013B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "1.0.14"                      =>
      from2013A.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").text == "1.0.0"                       =>
      from2012B.concatenate.convert(node)
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@schemaVersion").isEmpty                               =>
      "I don't know how to handle a proposal without version".failureNel
    case p @ <proposal>{ns @ _*}</proposal>                                                                 =>
      val version = (p \\ "@schemaVersion").text
      s"I don't know how to handle a proposal with version $version".failureNel
    case _                                                                                                  =>
      "Unknown xml file format".failureNel
  }
}

/**
 * Each semester needs to define its own SemesterConverter to account for the P1 model changes of that semester ONLY
 *
 * This way a proposal will be converted from one semester to the next from its original semester to the current one
 *
 */
trait SemesterConverter {
  val transformers: List[TransformFunction]
  private def compact(s:StepResult) = StepResult(s.change.distinct, s.node)
  def convert(node: XMLNode): Result = XMLConverter.transform(node, transformers:_*).map(compact)

  def removeBlueprint(instrument:String, name:String): (TransformFunction, String) = {
    val msg = s"The original proposal contained $name observations. The instrument is not available and those resources have been removed."
    val transform: TransformFunction = {
      case p @ <blueprints>{ns @ _*}</blueprints> if (p \\ instrument).nonEmpty =>
        // Remove nodes that match the instrument but preserve the rest
        val filtered = ns.filter{n => (n \\ instrument).isEmpty}
        StepResult(msg, <blueprints>{filtered}</blueprints>).successNel
    }
    (transform, msg)
  }
}

object SemesterConverter {

  // definition of a ZeroConverter
  private case object ZeroSemesterConverter extends SemesterConverter {
    override val transformers: List[TransformFunction] = Nil
  }

  // Monoid conversions
  implicit val zeroVersionConverter:Monoid[SemesterConverter] = Monoid.instance[SemesterConverter]((a, b) => new SemesterConverter { override val transformers = a.transformers |+| b.transformers}, ZeroSemesterConverter)
}

/**
 * This converter is to include any possible last message to the conversion log without
 */
case class LastStepConverter(semester: Semester) extends SemesterConverter {
  val notifyToUseOlderPIT: TransformFunction = {
    case n =>
      StepResult(s"Please use the PIT from semester ${semester.display} to view the unmodified proposal", n).successNel
  }
  override val transformers = List(notifyToUseOlderPIT)
}

/**
 * This converter supports migrating to 2023B
 */
case object SemesterConverter2023BTo2024A extends SemesterConverter {
  val renameSRPlusSky: TransformFunction = {
    case p @ <ghost>{ns @ _*}</ghost> =>
      object srTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <name>{name}</name>                                   =>
            // There is an interim model with some typos we need to consider
            <name>{name.text.replaceAll("Ghost", "GHOST").replaceAll("SRIFU1 Target \\+ SRIFU2 Sky", "SRIFU + Sky").replaceAll("SRIFU1 Sky \\+ SRIUF2 Target", "SRIFU + Sky").replaceAll("SRIFU1 Sky \\+ SRIFU2 Target", "SRIFU + Sky")}</name>
          case <targetMode>SRIFU1 Target + SRIFU2 Sky</targetMode>   => <targetMode>SRIFU + Sky</targetMode>
          case <targetMode>SRIFU1 Target + SRIUF2 Sky</targetMode>   => <targetMode>SRIFU + Sky</targetMode>
          case <targetMode>SRIFU1 Sky + SRIFU2 Target</targetMode>   => <targetMode>SRIFU + Sky</targetMode>
          case <targetMode>SRIFU1 Sky + SRIUF2 Target</targetMode>   => <targetMode>SRIFU + Sky</targetMode>
          case elem: xml.Elem                                        => elem.copy(child = elem.child.flatMap(transform))
          case _                                                     => n
        }
      }
      StepResult(s"The SRIFU[1|2] + SRIFU[1|2] Sky modes are now just SRIFU + Sky.", <ghost>{srTransformer.transform(ns)}</ghost>).successNel
  }

  override val transformers: List[TransformFunction] = List(renameSRPlusSky)
}

/**
 * This converter supports migrating to 2023B
 */
case object SemesterConverter2023ATo2023B extends SemesterConverter {
  private val niriFilterMapping: M.NiriFilter => M.GnirsFilter = _ match {
    case M.NiriFilter.BBF_Y    => M.GnirsFilter.Y
    case M.NiriFilter.BBF_J    => M.GnirsFilter.ORDER_5 // J
    case M.NiriFilter.BBF_H    => M.GnirsFilter.ORDER_4 // H\
    case M.NiriFilter.BBF_K | M.NiriFilter.BBF_KPRIME | M.NiriFilter.BBF_KSHORT 
                               => M.GnirsFilter.ORDER_3 // K
    case M.NiriFilter.NBF_H210 => M.GnirsFilter.H2
    case M.NiriFilter.NBF_HC   => M.GnirsFilter.PAH
    case _                     => M.GnirsFilter.ORDER_4 // H
  }

  private val niriCameraMapping: M.NiriCamera => M.GnirsPixelScale = _ match {
    case M.NiriCamera.F6                     => M.GnirsPixelScale.PS_015
    case M.NiriCamera.F14 | M.NiriCamera.F32 => M.GnirsPixelScale.PS_005
  }

  lazy val niriToGNIRSMessage: String = "NIRI Gemini North proposal has been migrated to GNIRS instead."
  val niriNameRegex = """NIRI (.*) (f/\d* \(.* FoV\)) (.*)""".r //(None|Altair .*)(f/\\d*).*".r
  def transformNiriName(name: String) = name match {
    case niriNameRegex(a, b, c) => s"GNIRS $a ${niriCameraMapping(M.NiriCamera.fromValue(b)).value()} ${niriFilterMapping(M.NiriFilter.fromValue(c)).value()}"
    case _                      => name
  }
  val niriToGNIRS: TransformFunction = {
    case p @ <niri>{ns @ _*}</niri> =>
      object NiriToGnirsTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case p @ <niri>{q @ _*}</niri> => <imaging id={p.attribute("id")}>{q.map(transform)}<visitor>false</visitor></imaging>
          case <name>{n}</name>          => <name>{transformNiriName(n.text)}</name>
          case <camera>{f}</camera>      => <pixelScale>{niriCameraMapping(M.NiriCamera.fromValue(f.text)).value()}</pixelScale>
          case <filter>{f}</filter>      => <filter>{niriFilterMapping(M.NiriFilter.fromValue(f.text)).value()}</filter>
          case elem: xml.Elem            => elem.copy(child = elem.child.flatMap(transform))
          case _                         => n
        }
      }
      StepResult(niriToGNIRSMessage, <gnirs>{NiriToGnirsTransformer.transform(ns)}</gnirs>).successNel
  }

  lazy val gracesToMaroonXMessage: String = "Graces Gemini North proposal has been migrated to Maroon-X instead."
  val gracesToMaroonX: TransformFunction = {
    case p @ <graces>{ns @ _*}</graces> =>
      object GracesToMaroonXTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case p @ <Graces>{q @ _*}</Graces> => <MaroonX id={p.attribute("id")}><name>{MaroonXBlueprint().name}</name><visitor>true</visitor></MaroonX>
          case elem: xml.Elem            => elem.copy(child = elem.child.flatMap(transform))
          case _                         => n
        }
      }
      StepResult(gracesToMaroonXMessage, <maroonx>{GracesToMaroonXTransformer.transform(ns)}</maroonx>).successNel
  }

  override val transformers: List[TransformFunction] = List(niriToGNIRS, gracesToMaroonX)
}

/**
 * This converter supports migrating to 2023A
 */
case object SemesterConverter2022BTo2023A extends SemesterConverter {
  // REL-4175 Convert classical to queue
  val upgradeClassical: TransformFunction = {
    case p @ <proposalClass>{ns @ _*}</proposalClass> if (p \ "classical").nonEmpty =>
      object ClassicalTransformer extends BasicTransformer {
        override def transform(n : xml.Node): xml.NodeSeq = n match {
          case s @ <classical>{ns @ _*}</classical>
             if s.attribute("key").isDefined    =>
            if (s.attribute("jwstSynergy").isDefined)
              <queue key={s.attribute("key")} jwstSynergy={s.attribute("jwstSynergy")} tooOption="None">{ns}</queue>
            else
              <queue key={s.attribute("key")} jwstSynergy="false" tooOption="None">{ns}</queue>
          case s @ <classical>{ns @ _*}</classical> =>
            if (s.attribute("jwstSynergy").isDefined)
              <queue jwstSynergy={s.attribute("jwstSynergy")} tooOption="None">{ns}</queue>
            else
              <queue jwstSynergy="false" tooOption="None">{ns}</queue>
          case elem: xml.Elem                   =>
            elem.copy(child = elem.child.flatMap(transform))
          case _                                => n
        }
      }
      StepResult("Classical proposal converted to queue", <proposalClass>{ClassicalTransformer.transform(ns)}</proposalClass>).successNel
  }

  def removeGNIRSRedCameraBlueprint: TransformFunction = {
    val msg = """The original proposal contained a GNIRS observation using the red-camera and pixel scale 0.15". This combination has been removed"""
    // Remove nodes that match the instrument with the specific configuration
    def shouldRemove(ns: Seq[scala.xml.Node]): Boolean =
      ns.exists{(n: scala.xml.Node) =>
        val gnirsSpec = (n \\ "gnirs" \\ "spectroscopy")
        val ps = gnirsSpec.headOption.map(_ \\ "pixelScale").exists(_.text == """0.15"/pix""")
        val cw = gnirsSpec.headOption.map(_ \\ "centralWavelength").exists(_.text == ">=2.5um")
        ps && cw
      }

    val transform: TransformFunction = {
      case p @ <blueprints>{ns @ _*}</blueprints> if (p \\ "gnirs").nonEmpty && shouldRemove(ns) =>
        // Remove nodes that match the instrument with the specific configuration
        val filtered = ns.filterNot {n =>
          val gnirsSpec = (n \\ "gnirs" \\ "spectroscopy")
          val ps = gnirsSpec.headOption.map(_ \\ "pixelScale").exists(_.text == """0.15"/pix""")
          val cw = gnirsSpec.headOption.map(_ \\ "centralWavelength").exists(_.text == ">=2.5um")
          ps && cw
        }
        StepResult(msg, <blueprints>{filtered}</blueprints>).successNel
    }
    transform
  }
  override val transformers: List[TransformFunction] = List(/*upgradeClassical,*/ removeGNIRSRedCameraBlueprint)
}

/**
 * This converter supports migrating to 2022B.
 */
case object SemesterConverter2022ATo2022B extends SemesterConverter {
  // Model for attachments has changed
  val upgradeAttachments: TransformFunction = {
    case m @ <attachment>{ns}</attachment> =>
      StepResult("Updated existing attachment value", <firstAttachment>{ns}</firstAttachment><secondAttachment></secondAttachment>).successNel
  }

  val specialToo: TransformFunction = {
    case p @ <proposalClass>{ns @ _*}</proposalClass> if (p \ "special").nonEmpty =>
      object SpecialToOTransformer extends BasicTransformer {
        override def transform(n : xml.Node): xml.NodeSeq = n match {
          case s @ <special>{ex @ _*}</special>
             if s.attribute("key").isDefined    =>
            <special key={s.attribute("key")} tooOption="None" jwstSynergy="false">{ex}</special>
          case s @ <special>{ex @ _*}</special> =>
            <special tooOption="None" jwstSynergy="false">{ex}</special>
          case elem: xml.Elem                   =>
            elem.copy(child = elem.child.flatMap(transform))
          case _                                => n
        }
      }
      StepResult("Updated jwstSynergy and ToO choice", <proposalClass>{SpecialToOTransformer.transform(ns)}</proposalClass>).successNel
  }

  override val transformers: List[TransformFunction] = List(upgradeAttachments, specialToo)
}
/**
 * This converter supports migrating to 2022A.
 */
case object SemesterConverter2021BTo2022A extends SemesterConverter {
  lazy val planetarySystemCategoryMessage: String = "The 'Planetary systems' category is now classified as 'Exoplanet Other'."
  lazy val starPlanetFormationCategoryMessage: String = "The 'Star and planet formation' category is now classified as 'Star Formation'."
  lazy val starStellarEvolutionCategoryMessage: String = "The 'Stars and stellar evolution' category is now classified as 'Stellar Astrophysics, Evolution, Supernovae, Abundances'."
  lazy val formationEvolutionCategoryMessage: String = "The 'Formation and evolution of compact objects' category is now classified as 'Stellar Remnants/Compact Objects, WD, NS, BH'."
  lazy val resolvedPopulationCategoryMessage: String = "The 'Resolved stellar populations and their environments' category is now classified as 'Stellar Remnants/Compact Objects, WD, NS, BH'."
  lazy val galaxyEvolutionCategoryMessage: String = "The 'Galaxy evolution' category is now classified as 'Extragalactic Other'."
  lazy val cosmologyCategoryMessage: String = "The 'Cosmology and fundamental physics' category is now classified as 'Cosmology, Fundamental Physics, Large Scale Structure'."

  val categoryUpdateTransform: TransformFunction = {
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Planetary systems") =>
      StepResult(planetarySystemCategoryMessage, <proposal tacCategory={TacCategory.EXOPLANET_OTHER.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Star and planet formation") =>
      StepResult(starPlanetFormationCategoryMessage, <proposal tacCategory={TacCategory.STAR_FORMATION.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Stars and stellar evolution") =>
      StepResult(starStellarEvolutionCategoryMessage, <proposal tacCategory={TacCategory.STELLAR_ASTROPHYSICS_EVOLUTION_SUPERNOVAE_ABUNDANCES.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Formation and evolution of compact objects") =>
      StepResult(formationEvolutionCategoryMessage, <proposal tacCategory={TacCategory.STELLAR_REMNANTS_COMPACT_OBJECTS_WD_NS_BH.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Resolved stellar populations and their environments") =>
      StepResult(resolvedPopulationCategoryMessage, <proposal tacCategory={TacCategory.STELLAR_POPULATIONS_CLUSTERS_CHEMICAL_EVOLUTION.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Galaxy evolution") =>
      StepResult(galaxyEvolutionCategoryMessage, <proposal tacCategory={TacCategory.EXTRAGALACTIC_OTHER.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Cosmology and fundamental physics") =>
      StepResult(cosmologyCategoryMessage, <proposal tacCategory={TacCategory.COSMOLOGY_FUNDAMENTAL_PHYSICS_LARGE_SCALE_STRUCTURE.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
  }

  val removeKeywords: TransformFunction = {
    case p @ <proposal>{ns @ _*}</proposal> =>
      val b = ns.filter {
        case <keywords>{_ @ _*}</keywords> =>
          false
        case a => true
      }
      StepResult("Removed keywords", <proposal tacCategory={p.attribute("tacCategory")} schemaVersion={Proposal.currentSchemaVersion}>{b}</proposal>).successNel
  }

  override val transformers: List[TransformFunction] = List(removeKeywords, categoryUpdateTransform)
}

/**
 * This converter supports migrating to 2021A.
 */
case object SemesterConverter2021ATo2021B extends SemesterConverter {
  val InvestigatorPrelude = "Investigator"
  val COInvestigatorPrelude = "Co Investigator"
  def noaoTransformMessage(prelude: String) = s"$prelude assigned to NSF's NOIRLab"
  def gnTransformMessage(prelude: String) = s"$prelude assigned to Gemini Observatory/NSF's NOIRLab(North)"
  def gsTransformMessage(prelude: String) = s"$prelude assigned to Gemini Observatory/NSF's NOIRLab(South)"
  def tololoTransformMessage(prelude: String) = s"$prelude assigned to Cerro Tololo Inter-American Observatory/NSF’s NOIRLab"

  val noirLabInstitutions: TransformFunction = {
    case p @ <coi>{ns @ _*}</coi> if (ns \\ "institution").exists(_.text == "NOAO")=>
      val ns2 = ns.collect {
        case <institution>NOAO</institution> =>
          <institution>NSF's NOIRLab</institution>
        case a => a
      }
      StepResult(noaoTransformMessage(COInvestigatorPrelude), <coi id={p.attribute("id")}>{ns2}</coi>).successNel
    case <address>{r @ _*}</address> if (r \\ "institution").exists(_.text == "NOAO") =>
      StepResult(noaoTransformMessage(InvestigatorPrelude),
        <address>
          <institution>NSF's NOIRLab</institution>
          <address>
            PO Box 26732
            950 North Cherry Avenue
            Tucson
            AZ
            85719
          </address>
          <country>USA</country>
        </address>
      ).successNel
  }
  val gnInstitutions: TransformFunction = {
    case p @ <coi>{ns @ _*}</coi> if (ns \\ "institution").exists(_.text == "Gemini Observatory - North")=>
      val ns2 = ns.collect {
        case <institution>Gemini Observatory - North</institution> =>
          <institution>Gemini Observatory/NSF’s NOIRLab (North)</institution>
        case a => a
      }
      StepResult(gnTransformMessage(COInvestigatorPrelude), <coi id={p.attribute("id")}>{ns2}</coi>).successNel
    case <address>{r @ _*}</address> if (r \\ "institution").exists(_.text == "Gemini Observatory - North") =>
      StepResult(gnTransformMessage(InvestigatorPrelude),
        <address>
          <institution>Gemini Observatory/NSF’s NOIRLab (North)</institution>
          <address>
            Gemini Observatory
            670 N. A'ohoku Place
            Hilo
            HI
            96720
          </address>
          <country>USA</country>
        </address>
      ).successNel
  }
  val gsInstitutions: TransformFunction = {
    case p @ <coi>{ns @ _*}</coi> if (ns \\ "institution").exists(_.text == "Gemini Observatory - South")=>
      val ns2 = ns.collect {
        case <institution>Gemini Observatory - South</institution> =>
          <institution>Gemini Observatory/NSF’s NOIRLab (South)</institution>
        case a => a
      }
      StepResult(gsTransformMessage(COInvestigatorPrelude), <coi id={p.attribute("id")}>{ns2}</coi>).successNel
    case <address>{r @ _*}</address> if (r \\ "institution").exists(_.text == "Gemini Observatory - South") =>
      StepResult(gsTransformMessage(InvestigatorPrelude),
        <address>
          <institution>Gemini Observatory/NSF’s NOIRLab (South)</institution>
          <address>
            Casilla 603
            La Serena
          </address>
          <country>Chile</country>
        </address>
      ).successNel
  }
  val tololoInstitutions: TransformFunction = {
    case p @ <coi>{ns @ _*}</coi> if (ns \\ "institution").exists(_.text == "Cerro Tololo Interamerican Observatory") =>
      val ns2 = ns.collect {
        case <institution>Cerro Tololo Interamerican Observatory</institution> =>
          <institution>Cerro Tololo Inter-American Observatory/NSF’s NOIRLab</institution>
        case a => a
      }
      StepResult(tololoTransformMessage(COInvestigatorPrelude), <coi id={p.attribute("id")}>{ns2}</coi>).successNel
    case <address>{r @ _*}</address> if (r \\ "institution").exists(_.text == "Cerro Tololo Interamerican Observatory") =>
      StepResult(tololoTransformMessage(InvestigatorPrelude),
        <address>
          <institution>Cerro Tololo Inter-American Observatory/NSF’s NOIRLab</institution>
          <address>
            Casilla 603
            La Serena
          </address>
          <country>Chile</country>
        </address>
      ).successNel
    }
  override val transformers: List[TransformFunction] = List(noirLabInstitutions, gnInstitutions, gsInstitutions, tololoInstitutions)
}

  /**
  * This converter supports migrating to 2021A.
  */
case object SemesterConverter2020BTo2021A extends SemesterConverter {

  val AuSubmissionMessage = "Submission(s) to Australia have been removed."

  val removeAustraliaSubmissions: TransformFunction = {
    case <ngo>{ns @ _*}</ngo> if (ns \\ "partner").exists(_.text == "au") =>
      StepResult(AuSubmissionMessage, Nil).successNel
  }

  override val transformers: List[TransformFunction] =
    List(removeAustraliaSubmissions)

}

/**
  * This converter supports migrating to 2020B.
  */
case object SemesterConverter2020ATo2020B extends SemesterConverter {
  // REL-3769 will be here when the requirements are finalized.
  val gpiRemover: TransformFunction = removeBlueprint("gpi", "GPI")._1

  val subaruComicsMessage: String = "Subaru proposal with COMICS has been removed."
  val subaruComicsTransform: TransformFunction = {
  case p @ <blueprints>{ns @ _*}</blueprints> if (p \\ "instrument").exists(_.text == "COMICS") =>
    val filtered = ns.filter{n => !(n \\ "subaru" \\ "instrument").exists(_.text == "COMICS")}
    StepResult(subaruComicsMessage, <blueprints>{filtered}</blueprints>).successNel
  }

  val subaruFmosMessage: String = "Subaru proposal with FMOS has been removed."
  val subaruFmosTransform: TransformFunction = {
    case p @ <blueprints>{ns @ _*}</blueprints> if (p \\ "instrument").exists(_.text == "FMOS") =>
      val filtered = ns.filter{n => !(n \\ "subaru" \\ "instrument").exists(_.text == "FMOS")}
      StepResult(subaruFmosMessage, <blueprints>{filtered}</blueprints>).successNel
  }

  override val transformers: List[TransformFunction] = List(gpiRemover, subaruComicsTransform, subaruFmosTransform)
}

/**
  *  This Converter will support migrating to 2020A
  */
case object SemesterConverter2019BTo2020A extends SemesterConverter {
  val subaruSuprimeMessage: String = "Subaru proposal with Suprime Cam has been migrated to Hyper Suprime Cam."
  val subaruSuprimeTransform: TransformFunction = {
    case p @ <subaru>{ns @ _*}</subaru> if (p \\ "instrument").exists(_.text == "Suprime Cam") =>
      object SuprimeTransform extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <instrument>{_ @ _*}</instrument> => <instrument>{"Hyper Suprime Cam"}</instrument>
          case <name>{name}</name>               => <name>{name.text.replaceFirst("Suprime Cam", "Hyper Suprime Cam")}</name>
          case elem: xml.Elem                    => elem.copy(child = elem.child.flatMap(transform))
          case _                                 => n
        }
      }
      StepResult(subaruSuprimeMessage, <subaru id={p.attribute("id")}>{SuprimeTransform.transform(ns)}</subaru>).successNel
  }

  lazy val genderMessage: String = "Add gender field"
  val genderTransform: TransformFunction = {
    case p @ <pi>{ns @ _*}</pi> =>
      val addGender = ns :+ <gender>None selected</gender>
      StepResult(genderMessage, <pi id={p.attribute("id")}>{addGender}</pi>).successNel
    case p @ <coi>{ns @ _*}</coi> =>
      val addGender = ns :+ <gender>None selected</gender>
      StepResult(genderMessage, <coi id={p.attribute("id")}>{addGender}</coi>).successNel
  }

  lazy val solarSystemCategoryMessage: String = "The Solar System category is now classified as Planetary systems."
  lazy val galacticCategoryMessage: String = "The Galactic category is now classified as Stars and stellar evolution."
  lazy val extragalacticCategoryMessage: String = "The Extragalactic category is now classified as Galaxy evolution."
  val categoryUpdateTransform: TransformFunction = {
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Solar System") =>
      StepResult(solarSystemCategoryMessage, <proposal tacCategory={TacCategory.SOLAR_SYSTEM_OTHER.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Galactic") =>
      StepResult(galacticCategoryMessage, <proposal tacCategory={TacCategory.GALACTIC_OTHER.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").exists(_.text == "Extragalactic") =>
      StepResult(extragalacticCategoryMessage, <proposal tacCategory={TacCategory.EXTRAGALACTIC_OTHER.value()} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
  }

  val texesRemover: TransformFunction = removeBlueprint("texes", "Texes")._1
  val phoenixRemover: TransformFunction = removeBlueprint("phoenix", "Phoenix")._1

  override val transformers: List[TransformFunction] = List(subaruSuprimeTransform, genderTransform, categoryUpdateTransform, texesRemover, phoenixRemover)
}


case object SemesterConverter2019ATo2019B extends SemesterConverter {
  lazy val dssiGSToZorroMessage: String = "DSSI Gemini South proposal has been migrated to Zorro instead."
  val dssiGSToZorro: TransformFunction = {
    case p @ <dssi>{ns @ _*}</dssi> if (p \\ "Dssi" \\ "site").map(_.text).exists(_.equals(Site.GS.name)) =>
      object DssiGSZorroTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case p @ <Dssi>{q @ _*}</Dssi> => <Zorro id={p.attribute("id")}>{q.map(transform) +: <mode>{ZorroMode.SPECKLE.value}</mode>}</Zorro>
          case <name>{_}</name>          => <name>{ZorroBlueprint(ZorroMode.SPECKLE).name}</name>
          case elem: xml.Elem            => elem.copy(child = elem.child.flatMap(transform))
          case _                         => n
        }
      }
      StepResult(dssiGSToZorroMessage, <zorro>{DssiGSZorroTransformer.transform(ns)}</zorro>).successNel
  }

  override val transformers = List(dssiGSToZorro)
}

/**
 * This converter will upgrade to 2019A.
 */
case object SemesterConverter2018BTo2019A extends SemesterConverter {
    // REL-3552: No GMOS-N + Altair offered for 2019A. Remove Altair from component.
    // The name of the instrument is auto-generated from its properties, so we don't worry about the <name>...</name> tag.
    private lazy val gmosnAltairLGSRE  = " Altair Laser Guidestar( w/ (PWFS1|OIWFS))?"
    private lazy val gmosnAltairNGSRE  = " Altair Natural Guidestar( w/ Field Lens)?"
    lazy val gmosnAltairRemoverMessage = "GMOS-N does not offer Altair in 2019A. Altair component removed."
    val gmosnAltairRemover: TransformFunction = {
      case p @ <gmosN>{ns @ _*}</gmosN> if (p \\ "altair" \ "lgs").nonEmpty || (p \\ "altair" \\ "ngs").nonEmpty =>
        object GmosNAltairRemover extends BasicTransformer {
          override def transform(n: xml.Node): xml.NodeSeq = n match {
            case <altair>{_ @ _*}</altair> => <altair><none/></altair>
            case <name>{name}</name>       => <name>{name.text.replaceFirst(gmosnAltairLGSRE, "").replaceFirst(gmosnAltairNGSRE, "")}</name>
            case elem: xml.Elem            => elem.copy(child = elem.child.flatMap(transform))
            case _                         => n
          }
        }
        StepResult(gmosnAltairRemoverMessage, <gmosN>{GmosNAltairRemover.transform(ns)}</gmosN>).successNel
    }

  // REL-3485: F2 MOS not available for 2019A.
  val f2MOSNotAvailable: TransformFunction = {
    case f @ <flamingos2>{ns @ _*}</flamingos2> if (f \ "mos").nonEmpty =>
      StepResult("Flamingos2 Multi-Object Spectroscopy is not offered", f).successNel
  }

  // REL-3494: CFH now only offered in queue proposal class.
  val cfhClassicalToQueue: TransformFunction = {
    case p @ <proposalClass>{ns @ _*}</proposalClass> if (p \ "classical").nonEmpty && (p \\ "partner").map(_.text).exists(_.equals(ExchangePartner.CFH.value)) =>
      object ClassicalToQueueTransformer extends BasicTransformer {
        override def transform(n : xml.Node): xml.NodeSeq = n match {
          case <classical>{ex @ _*}</classical> => <queue tooOption="None">{ex}</queue>
          case elem: xml.Elem                   => elem.copy(child = elem.child.flatMap(transform))
          case _                                => n
        }
      }
      StepResult("All CFH exchange time will now be done in queue.", <proposalClass>{ClassicalToQueueTransformer.transform(ns)}</proposalClass>).successNel
  }

  override val transformers = List(gmosnAltairRemover, f2MOSNotAvailable, cfhClassicalToQueue)
}

/**
 * This converter will upgrade to 2018B.
 */
case object SemesterConverter2018ATo2018B extends SemesterConverter {
  lazy val dssiGNToAlopekeMessage: String = "DSSI Gemini North proposal has been migrated to ʻAlopeke instead."
  val dssiGNToAlopeke: TransformFunction = {
    case p @ <dssi>{ns @ _*}</dssi> if (p \\ "Dssi" \\ "site").map(_.text).exists(_.equals(Site.GN.name)) =>
      object DssiGNAlopekeTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case p @ <Dssi>{q @ _*}</Dssi> => <Alopeke id={p.attribute("id")}>{q.map(transform) +: <mode>{AlopekeMode.SPECKLE.value}</mode>}</Alopeke>
          case <name>{_}</name>          => <name>{AlopekeBlueprint(AlopekeMode.SPECKLE).name}</name>
          case elem: xml.Elem            => elem.copy(child = elem.child.flatMap(transform))
          case _                         => n
        }
      }
      StepResult(dssiGNToAlopekeMessage, <alopeke>{DssiGNAlopekeTransformer.transform(ns)}</alopeke>).successNel
  }

  lazy val phoenixSiteMessage: String = "Phoenix proposal has been assigned to Gemini South."
  val phoenixSite: TransformFunction = {
    case p @ <phoenix>{ns @ _*}</phoenix> if (p \\ "Phoenix" \\ "site").map(_.text).forall(n => !n.equals(Site.GS.name)) =>
      object PhoenixSiteTransformer extends BasicTransformer {
        val phoenixNameRegex = "(Phoenix) (.*)".r
        def transformPhoenixName(name: String) = name match {
          case phoenixNameRegex(a, b) => s"$a ${Site.GS.name} $b"
          case _                      => name
        }

        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case p @ <Phoenix>{q @ _*}</Phoenix> => <Phoenix id={p.attribute("id")}>{q.map(transform) +: <site>{Site.GS.name}</site>}</Phoenix>
          case <name>{name}</name>             => <name>{transformPhoenixName(name.text)}</name>
          case elem: xml.Elem                  => elem.copy(child = elem.child.flatMap(transform))
          case _                               => n
        }
      }
      StepResult(phoenixSiteMessage, <phoenix>{PhoenixSiteTransformer.transform(ns)}</phoenix>).successNel
  }

  override val transformers = List(dssiGNToAlopeke, phoenixSite)
}

/**
 * This converter will upgrade to 2018A
 */
case object SemesterConverter2017BTo2018A extends SemesterConverter {
  val jLoFilter = "J-lo (1.122 um)"
  val yFilter = "Y (1.020 um)"

  val removeF2YJloFilters: TransformFunction = {
    case p @ <flamingos2>{ns @ _*}</flamingos2> if (ns \ "filter").map(_.text).exists(f => f === jLoFilter || f === yFilter) =>
      object yjLoFilterTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <name>{name}</name>                                   => <name>{name.text.replaceAll(jLoFilter, "").replaceAll(yFilter, "")}</name>
          case <filter>{filter}</filter> if filter.text == jLoFilter => xml.NodeSeq.Empty
          case <filter>{filter}</filter> if filter.text == yFilter   => xml.NodeSeq.Empty
          case elem: xml.Elem                                        => elem.copy(child = elem.child.flatMap(transform))
          case _                                                     => n
        }
      }
      StepResult(s"The unavailable Flamingos2 filters $jLoFilter and $yFilter have been removed from the proposal.", <flamingos2>{yjLoFilterTransformer.transform(ns)}</flamingos2>).successNel
  }

  val renameCFH: TransformFunction = {
    case p @ <partner>cfht</partner> =>
      StepResult("Renamed CFHT partner to CFH", <partner>cfh</partner>).successNel
  }

  override val transformers = List(removeF2YJloFilters, renameCFH)
}

/**
 * This converter will upgrade to 2017B
 */
case object SemesterConverter2017ATo2017B extends SemesterConverter {
  val timeToProgTime: TransformFunction = {
    case o @ <observation>{ns @ _*}</observation> if (o \\ "time").nonEmpty =>

      // The time field from previous versions is being instead used to represent program time, and the time field
      // will now be modified to represent total time, i.e. progTime + partTime (partner calibration time).
      object TimeToProgTime extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case t @ <time>{ts @ _*}</time> => <progTime units={t.attribute("units")}>{ts}</progTime>
          case elem: xml.Elem             => elem.copy(child=elem.child.flatMap(transform))
          case _                          => n
        }
      }
      StepResult("Former observation time parameter mapped to program time",
        // Too many attributes.
        <observation band={o.attribute("band")} enabled={o.attribute("enabled")} target={o.attribute("target")} condition={o.attribute("condition")} blueprint={o.attribute("blueprint")}>{TimeToProgTime.transform(ns)}</observation>).successNel
  }
  override val transformers = List(timeToProgTime)
}

/**
 * This converter will upgrade to 2017A
 */
case object SemesterConverter2016BTo2017A extends SemesterConverter {
  // Set overrideAffiliate as true by default. It will automatically set to false if current FT affiliate is undefined
  // or the same as the PI institution affiliate.
  val overrideAffiliate:TransformFunction = {
    case m @ <meta>{ns @ _*}</meta> if (m \ "@overrideAffiliate").isEmpty =>
      StepResult("Affiliate override flag is missing", <meta overrideAffiliate="true">{ns}</meta>).successNel
  }
  override val transformers = List(overrideAffiliate)
}

/**
 * This converter will upgrade to 2016B
 */
case object SemesterConverter2016ATo2016B extends SemesterConverter {
  val oldKLongFilter    = "K-long (2.00 um)"
  val removedFilter     = <filter>{oldKLongFilter}</filter>
  val replacementFilter = "K-long (2.20 um)"

  val replaceKLongFilter: TransformFunction = {
      case p @ <flamingos2>{ns @ _*}</flamingos2> if (p \\ "filter").theSeq.contains(removedFilter) =>
        val defaultFilter     = <filter>{replacementFilter}</filter>

        object KLongFilterTransformer extends BasicTransformer {
          override def transform(n: xml.Node): xml.NodeSeq = {
            n match {
              case <name>{name}</name>                                        => <name>{name.text.replace(oldKLongFilter, replacementFilter)}</name>
              case <filter>{filter}</filter> if filter.text == oldKLongFilter => defaultFilter
              case elem: xml.Elem                                             => elem.copy(child=elem.child.flatMap(transform))
              case _                                                          => n
            }
          }
        }
        StepResult("The Flamingos2 filter K-long (2.00 um) has been converted to K-long (2.20 um).", <flamingos2>{KLongFilterTransformer.transform(ns)}</flamingos2>).successNel
    }

  val removedModes = List(GpiObservingMode.HLiwa, GpiObservingMode.HStar).map(_.value())

  val gpiModes: TransformFunction = {
    case p @ <gpi>{ns @ _*}</gpi> if removedModes.contains((p \\ "observingMode").text) =>
      object GpiObsModeTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
            case <name>{name}</name>                   => <name>{name.text.replace((p \\ "observingMode").text, GpiObservingMode.HDirect.value)}</name>
            case <observingMode>{name}</observingMode> => <observingMode>{GpiObservingMode.HDirect.value}</observingMode>
            case elem: xml.Elem                        => elem.copy(child = elem.child.flatMap(transform))
            case _                                     => n
          }
      }
      StepResult(s"GPI proposal with observing mode ${(p \\ "observingMode").text} has been assigned to the ${GpiObservingMode.HDirect.value} mode.", <gpi>{GpiObsModeTransformer.transform(ns)}</gpi>).successNel
  }
  override val transformers = List(replaceKLongFilter, gpiModes)
}

/**
 * This converter will upgrade to 2016A
 */
case object SemesterConverter2015BTo2016A extends SemesterConverter {
  val removedFilters = List(<filter>J-continuum (1.122 um)</filter>, <filter>Jcont (1.065 um)</filter>, <filter>CO 3-1 (bh) (2.323 um)</filter>)
  val removeUnusedNIRIFilters: TransformFunction = {
     case p @ <niri>{ns @ _*}</niri> if (p \ "filter").theSeq.exists(removedFilters.contains) =>
       val filtersToRemove = (p \ "filter").theSeq.filter(removedFilters.contains)
       object SlitTransformer extends BasicTransformer {
         // Regex to match the name, filters are included with a + sign between them
         val nameRegEx = s"(.*)[${removedFilters.map(i => s"(${i.text.replace("-", "\\-")}\\+?)").mkString("|")}](.*)"
         // Individual filter regex, note that ( and ) need to be escaped
         val filterRegexes = removedFilters.map { f =>
           s"""${f.text.replace("(", "\\(").replace (")", "\\)")}\\+?""".r
         }

         override def transform(n: xml.Node): xml.NodeSeq = n match {
           case <name>{name}</name> if name.text.matches(nameRegEx) =>
             val replacedText = filterRegexes.foldLeft(name.text) { (t, f) =>
               f.replaceAllIn(t, "")
             }
             <name>{replacedText}</name>
           case f @ <filter>{_}</filter> if removedFilters.contains(f) => xml.NodeSeq.Empty
           case elem: xml.Elem                                         => elem.copy(child = elem.child.flatMap(transform))
           case _                                                      => n
         }
       }
       // Remove unavailable filters
       val removedFiltersText = if (filtersToRemove.length == 1) s"filter ${filtersToRemove.map(_.text).head} has been removed" else s"filters ${filtersToRemove.map(_.text).mkString(", ")} have been removed."
       StepResult(s"The unavailable NIRI $removedFiltersText", <niri id={p.attribute("id")}>{SlitTransformer.transform(ns)}</niri>).successNel
    }

  override val transformers = List(removeUnusedNIRIFilters)
}

/**
 * This converter will upgrade to 2015B
 */
case object SemesterConverter2015ATo2015B extends SemesterConverter {
  val gracesReadModeTransformer: TransformFunction = {
    case p @ <graces>{ns @ _*}</graces> if (p \\ "readMode").isEmpty =>
      StepResult(s"Graces missing Read mode assigned to Fast.", <graces>{ns}<readMode>Fast (Gain=1.6e/ADU, Read noise=4.7e)</readMode></graces>).successNel
  }

  val gracesFiberTransformer: TransformFunction = {
    case p @ <graces>{ns @ _*}</graces> if (p \\ "fiberMode").nonEmpty =>
      val fiberMode = (p \\ "fiberMode").text
      val gracesRegex = "(Graces )?(.*)".r
      def transformFiberMode(name: String) = name match {
        case gracesRegex(_, "2 fibers (target + sky, R~30000)") => "2 fibers (target+sky, R~40k)"
        case gracesRegex(_, "1 fiber (target, R~50000)")        => "1 fiber (target only, R~67.5k)"
        case _                                                  => name
      }

      object GracesFiberMode extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <fiberMode>{name}</fiberMode> => <fiberMode>{transformFiberMode(name.text)}</fiberMode>
          case <name>{name}</name>           => <name>Graces {transformFiberMode(name.text)}</name>
          case elem: xml.Elem                => elem.copy(child = elem.child.flatMap(transform))
          case _                             => n
        }
      }

      StepResult(s"Graces Fiber mode $fiberMode updated to ${transformFiberMode(fiberMode)}.", <graces>{GracesFiberMode.transform(ns)}</graces>).successNel
  }

  override val transformers = List(gracesFiberTransformer, gracesReadModeTransformer)
}

/**
 * This converter is to convert to 2015A model
 */
case object SemesterConverter2014BTo2015A extends SemesterConverter {
  val gsSubmission:TransformFunction = {
    case <ngo>{ns @ _*}</ngo> if (ns \\ "partner").text == "gs" => StepResult("Gemini Staff is no longer a valid partner and this time request has been removed", Nil).successNel
  }
  override val transformers = List(gsSubmission)
}

/**
  * This converter only changes the schema version but retains the semester
  */
case object SchemaVersionConverter extends SemesterConverter {
  val current = Semester.current
  val schemaVersionTransformToCurrent:TransformFunction = {
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").nonEmpty =>
      StepResult(s"Updated schema version to ${Proposal.currentSchemaVersion}", <proposal tacCategory={(p \ "@tacCategory").text} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case <proposal>{ns @ _*}</proposal>                                      =>
      StepResult(s"Updated schema version to ${Proposal.currentSchemaVersion}", <proposal schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
  }

  override val transformers = List(schemaVersionTransformToCurrent)
}

/**
 * This converter is to current, as a minimum you need to convert a proposal to be the current version and semester
 */
case object SemesterConverterToCurrent extends SemesterConverter {
  val current = Semester.current
  val semesterTransformToCurrent:TransformFunction = {
    case s @ <semester/> =>
      StepResult(s"Updated semester to ${current.display}", <semester year={current.year.toString} half={current.half.toString}/>).successNel
  }
  val schemaVersionTransformToCurrent:TransformFunction = {
    case p @ <proposal>{ns @ _*}</proposal> if (p \ "@tacCategory").nonEmpty =>
      StepResult(s"Updated schema version to ${Proposal.currentSchemaVersion}", <proposal tacCategory={(p \ "@tacCategory").text} schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
    case <proposal>{ns @ _*}</proposal>                                      =>
      StepResult(s"Updated schema version to ${Proposal.currentSchemaVersion}", <proposal schemaVersion={Proposal.currentSchemaVersion}>{ns}</proposal>).successNel
  }

  override val transformers = List(schemaVersionTransformToCurrent, semesterTransformToCurrent)
}

case object SemesterConverter2014BTo2014BSV extends SemesterConverter {
  val addAltairToGmosN: TransformFunction = {
    case p @ <gmosN>{ns @ _*}</gmosN> if (p \\ "altair").isEmpty =>
      val noAltair = <altair><none/></altair>

      val regularName    = """GMOS-N (IFU|Imaging|LongSlit|MOS) (.*)""".r
      val nodShuffleName = """GMOS-N (LongSlit|MOS) N\+S (.*)""".r

      def transformName(name: String) = name match {
        case nodShuffleName(gmosType, rest) => s"GMOS-N $gmosType N+S None $rest"
        case regularName(gmosType, rest)    => s"GMOS-N $gmosType None $rest"
        case _                              => name
      }
      object GmosNNoAltair extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <name>{name}</name>                           => noAltair +: <name>{transformName(name.text)}</name>
          case elem: xml.Elem                                => elem.copy(child = elem.child.flatMap(transform))
          case _                                             => n
        }
      }

      StepResult("GMOS-N observations without an Altair setting have had Adaptive Optics set to None.", <gmosN>{GmosNNoAltair.transform(ns)}</gmosN>).successNel
  }

  override val transformers = List(addAltairToGmosN)
}

case object SemesterConverter2014ATo2014B extends SemesterConverter {
  override val transformers = Nil
}

/**
 * Converter from 2013B to 2014A
 */
case object SemesterConverter2013BTo2014A extends SemesterConverter {
  val (trecsRemoved, _) = removeBlueprint("trecs", "T-ReCS")
  val (michelleRemoved, _) = removeBlueprint("michelle", "Michelle")
  val (niciRemoved, _) = removeBlueprint("nici", "NICI")
  val (oldSlit, newSlit) = ("0.25 arcsec slit", "0.5 arcsec slit")
  val updateGmosNSlit: TransformFunction = {
     case p @ <gmosN>{ns @ _*}</gmosN> if (p \ "longslit" \ "fpu").text == oldSlit =>
       object SlitTransformer extends BasicTransformer {
         val nameRegEx = s"(.*)$oldSlit(.*)"

         override def transform(n: xml.Node): xml.NodeSeq = n match {
           case <name>{name}</name> if name.text.matches(nameRegEx) => <name>{name.text.replace(oldSlit, newSlit)}</name>
           case <fpu>{_}</fpu>                                      => <fpu>{newSlit}</fpu>
           case elem: xml.Elem                                      => elem.copy(child=elem.child.flatMap(transform))
           case _                                                   => n
         }
       }
       // Convert the 0.25 longslit to 0.5
       StepResult("The unavailable GMOS-N 0.25\" slit has been converted to the 0.5\" slit.", <gmosN>{SlitTransformer.transform(ns)}</gmosN>).successNel
    }
  val addGnirsCentralWavelength: TransformFunction = {
    case p @ <gnirs>{ns @ _*}</gnirs> if (p \ "spectroscopy").nonEmpty && (p \\ "centralWavelength").isEmpty =>
      val centralWavelengthDefault = <centralWavelength>&lt; 2.5um</centralWavelength>
      object CentralWavelengthTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <name>{name}</name>                           => <name>{name + " < 2.5um"}</name>
          case s @ <spectroscopy>{nodes @ _*}</spectroscopy> => <spectroscopy id={s.attribute("id")}>{transform(nodes) ++ centralWavelengthDefault}</spectroscopy>
          case elem: xml.Elem                                => elem.copy(child=elem.child.flatMap(transform))
          case _                                             => n
        }
      }

      StepResult("GNIRS observation doesn't have a central wavelength range, assigning to '< 2.5um'", <gnirs>{CentralWavelengthTransformer.transform(ns)}</gnirs>).successNel
  }
  val removeF2NarrowBandFilter: TransformFunction = {
    case p @ <flamingos2>{ns @ _*}</flamingos2> if (p \ "imaging").nonEmpty =>
      val narrowBandFilterRegex = """F10\d\d \(1.0\d\d um\)"""
      val replacementFilter     = "Y (1.020 um)"
      val defaultFilter         = <filter>{replacementFilter}</filter>

      object NarrowbandFilterTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <name>{name}</name>                                                     => <name>{name.text.replaceAll(narrowBandFilterRegex, replacementFilter)}</name>
          case <filter>{filter}</filter> if filter.text.matches(narrowBandFilterRegex) => defaultFilter
          case elem: xml.Elem                                                          => elem.copy(child=elem.child.flatMap(transform))
          case _                                                                       => n
        }
      }
      StepResult("The unavailable Flamingos2 filters F1056 (1.056 um)/F1063 (1.063 um) have been converted to Y (1.020 um).", <flamingos2>{NarrowbandFilterTransformer.transform(ns)}</flamingos2>).successNel
  }
  def f23KYFilterCondition(p: xml.Node):Boolean = ((p \ "longslit").nonEmpty || (p \ "mos").nonEmpty) && (p \\ "disperser").text == "R3K" && (p \\ "filter").text == "Y (1.020 um)"
  val replaceF2R3KYFilter: TransformFunction = {
    case p @ <flamingos2>{ns @ _*}</flamingos2> if f23KYFilterCondition(p) =>
      val yFilter           = """Y \(1.020 um\)"""
      val replacementFilter = "J-lo (1.122 um)"
      val defaultFilter     = <filter>{replacementFilter}</filter>

      object F23KYFilterTransformer extends BasicTransformer {
        override def transform(n: xml.Node): xml.NodeSeq = n match {
          case <name>{name}</name>                                       => <name>{name.text.replaceAll(yFilter, replacementFilter)}</name>
          case <filter>{filter}</filter> if filter.text.matches(yFilter) => defaultFilter
          case elem: xml.Elem                                            => elem.copy(child=elem.child.flatMap(transform))
          case _                                                         => n
        }
      }
      StepResult("The unavailable Flamingos2 configuration R3K + Y has been converted to R3K + J-lo.", <flamingos2>{F23KYFilterTransformer.transform(ns)}</flamingos2>).successNel
  }
  object GMOSAddFpuTransformer extends BasicTransformer {
    val slitName   = "1.0 arcsec slit"
    val fpuDefault = <fpu>{slitName}</fpu>
    override def transform(n: xml.Node): xml.NodeSeq = n match {
      case <name>{name}</name>         => <name>{name.text.replaceAll("MOS", s"MOS $slitName")}</name>
      case s @ <mos>{nodes @ _*}</mos> => <mos id={s.attribute("id")}>{transform(nodes) ++ fpuDefault}</mos>
      case elem: xml.Elem              => elem.copy(child=elem.child.flatMap(transform))
      case _                           => n
    }
  }
  def gmosMOSSpectroscopyCondition(p: xml.Node):Boolean = (p \ "regime").text == "optical" && (p \ "mos").nonEmpty
  val addFpuToGmosMOSSpectroscopy: TransformFunction = {
    case p @ <gmosS>{ns @ _*}</gmosS> if gmosMOSSpectroscopyCondition(p) =>
      StepResult("A default 1\" slit has been added to the GMOS-S MOS observation.", <gmosS>{GMOSAddFpuTransformer.transform(ns)}</gmosS>).successNel
    case p @ <gmosN>{ns @ _*}</gmosN> if gmosMOSSpectroscopyCondition(p) =>
      StepResult("A default 1\" slit has been added to the GMOS-N MOS observation.", <gmosN>{GMOSAddFpuTransformer.transform(ns)}</gmosN>).successNel
  }
  override val transformers = List(trecsRemoved, michelleRemoved, niciRemoved, updateGmosNSlit, addGnirsCentralWavelength, removeF2NarrowBandFilter, replaceF2R3KYFilter, addFpuToGmosMOSSpectroscopy)
}

/**
 * Converter from 2013A to 2013B
 *
 * On 2013B there no breaking changes in the model
 */
case object SemesterConverter2013ATo2013B extends SemesterConverter {
  override val transformers = Nil
}

/**
 * Converter from 2012B to 2013A
 *
 * See the changelog
 */
case object SemesterConverter2012BTo2013A extends SemesterConverter {

  val band3OptionChosen:TransformFunction = {
    case m @ <meta>{ns @ _*}</meta> if (m \ "@band3optionChosen").isEmpty => StepResult("Band3 Option is missing, set as false", <meta band3optionChosen="false">{ns}</meta>).successNel
  }
  val herbigHaroObjects:TransformFunction = {
    case Text("Herbig-Haro stars") => StepResult("Keyword 'Herbig-Haro stars' renamed to 'Herbig-Haro objects'", Text("Herbig-Haro objects")).successNel
  }
  val ukSubmission:TransformFunction = {
    case <ngo>{ns @ _*}</ngo> if (ns \\ "partner").text == "uk" => StepResult("NGO acceptance from the United Kingdom has been removed", Nil).successNel
  }
  val ukNgoAuthority:TransformFunction = {
    case <ngoauthority>uk</ngoauthority> => StepResult("The United Kingdom was marked as ITAC NGO authority and has been removed", Nil).successNel
  }
  override val transformers = List(band3OptionChosen, herbigHaroObjects, ukSubmission, ukNgoAuthority)
}
