// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.io.impl.migration
package to2023A

import edu.gemini.spModel.gemini.ghost.GhostAsterism.PrvMode.PrvOff
import edu.gemini.spModel.target.env.AsterismType.GhostHighResolutionTargetPlusSky
import edu.gemini.spModel.io.PioSyntax._
import edu.gemini.spModel.pio.{Container, Document, ParamSet, Pio, Version}
import edu.gemini.spModel.pio.xml.{PioXmlFactory, PioXmlUtil}

object To2023A extends Migration {

  override val version = Version.`match`("2023A-1")

  val fact = new PioXmlFactory

  override val conversions: List[Document => Unit] =
    List(
      addGhostPrvParam
    )

  private def addGhostPrvParam(d: Document): Unit =
    targetEnvs(d).foreach { ps =>
      ps.paramSet("asterism").foreach { a =>
        if (a.value("tag").contains(GhostHighResolutionTargetPlusSky.tag)) {
          Pio.addParam(fact, a, "prv", PrvOff.tag)
        }
      }
    }

}
