// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.io.impl.migration.to2025B

import edu.gemini.spModel.io.PioSyntax._
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.pio.{Document, Version}
import edu.gemini.spModel.pio.xml.PioXmlFactory

object To2025B extends Migration {

  override val version = Version.`match`("2025B-1")

  val fact = new PioXmlFactory

  override val conversions: List[Document => Unit] =
    List(
      renameSequenceName
    )

  private def renameSequenceName(d: Document): Unit = {
    d.allContainers.filter(_.getSubtype == "IGRINS2").foreach { u =>
      u.setSubtype("IGRINS-2")
    }
  }

}
