package edu.gemini.phase2.template.factory.impl.phoenix

import edu.gemini.spModel.gemini.phoenix.PhoenixParams

sealed trait PhoenixFilterGroup

object PhoenixFilterGroup {

  case object JHK extends PhoenixFilterGroup
  case object L   extends PhoenixFilterGroup
  case object M   extends PhoenixFilterGroup

  def forFilter(f: PhoenixParams.Filter): PhoenixFilterGroup = {
    import PhoenixParams.Filter._
    f match {
      case /* J */ J7799 | J8265 | J9232 | J9440 | J9671 |
           /* H */ H6073 | H6420 |
           /* K */ K4132 | K4220 | K4308 | K4396 | K4484 |  K4578 | K4667 | K4748 => JHK
      case /* L */ L2462 | L2734 | L2870 | L3010 | L3100 | L3290                  => L
      case /* M */ M1930 | M2030 | M2150                                          => M
    }
  }

}