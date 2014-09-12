package edu.gemini.p2checker.util

import edu.gemini.p2checker.api.{ObservationElements, Problem}
import edu.gemini.p2checker.api.Problem.Type.{ERROR, WARNING}
import edu.gemini.spModel.config2.{ItemKey, Config}
import edu.gemini.spModel.core.ProgramId

/**
 * Created with IntelliJ IDEA.
 * User: sraaphor
 * Date: 4/10/14
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */

// REL-1231: MDF mask name check
object MdfConfigRule {
  private val Prefix     = "MDFRule_"
  private val FormatRule = "MDFConfigFormatRule"
  private val MatchRule  = "MDFConfigMatchRule"

  val MissingError =                    "The name of the Custom Mask must be specified"
  def FormatError(maskName: String)  = s"The Custom Mask MDF '$maskName' has the incorrect format"
  def MatchWarning(maskName: String) = s"The Custom Mask MDF '$maskName' does not match the current program"

  val MaskPattern = """^G(N|S)(\d{4})(A|B)([A-Z]+)(\d\d\d)-\d\d$""".r

  // Each instrument has its own FPUMode type, with its own value representing a custom mask, and we must
  // pass these in.
  def checkMaskName[FPUMode](customMaskSetting: FPUMode, config: Config, step: Int, elems: ObservationElements, state: Object): Option[Problem] = {
    def lookup[T](k: String): Option[T] =
      Option(config.getItemValue(new ItemKey(k)).asInstanceOf[T])

    // Determine if we are dealing with a custom mask.
    if (lookup[FPUMode]("instrument:fpuMode").forall(_ != customMaskSetting)) None
    else {
      val node         = SequenceRule.getInstrumentOrSequenceNode(step, elems)
      def missingError                   = new Problem(ERROR,   Prefix + FormatRule, MissingError,  node)
      def formatError(maskName: String)  = new Problem(ERROR,   Prefix + FormatRule, FormatError(maskName),  node)
      def matchWarning(maskName: String) = new Problem(WARNING, Prefix + MatchRule,  MatchWarning(maskName), node)

      // If the custom mask is empty, issue a warning.
      lookup[String]("instrument:fpuCustomMask").filterNot(_ == "").fold(Option(missingError)) { maskName =>

        Option(node.getProgramID).flatMap { pid =>
          // Now from the filter name, find the program ID to which it would correspond, and parse it.
          maskName match {
            case MaskPattern(site, year, ab, pType, pNum) =>
              val idFromMask = s"G$site-$year$ab-$pType-${pNum.toInt}"
              val standardId = ProgramId.parseStandardId(idFromMask)
              standardId.fold(Option(formatError(maskName))) { p =>
                if (p.toSp == pid) None
                else Some(matchWarning(maskName))
              }
            case _                                           =>
              Option(formatError(maskName))
          }
        }
      }
    }
  }
}