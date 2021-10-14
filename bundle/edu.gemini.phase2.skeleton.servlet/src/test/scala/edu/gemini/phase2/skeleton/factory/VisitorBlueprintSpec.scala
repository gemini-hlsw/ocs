// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.{AlopekeBlueprint, AlopekeMode, DssiBlueprint, GeminiBlueprintBase, IgrinsBlueprint, MaroonXBlueprint, Site, VisitorBlueprint, ZorroBlueprint, ZorroMode}
import edu.gemini.pot.sp.{ISPProgram, SPComponentType}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.visitor.{VisitorConfig, VisitorInstrument}
import edu.gemini.spModel.rich.pot.sp._
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class VisitorBlueprintSpec extends TemplateSpec("VISITOR_BP.xml") with SpecificationLike with ScalaCheck {

  val GenAlopekeBlueprint: Gen[AlopekeBlueprint] =
    Gen.oneOf(AlopekeMode.SPECKLE, AlopekeMode.WIDE_FIELD).map(m => AlopekeBlueprint(m))

  val GenDssiBlueprint: Gen[DssiBlueprint] =
    Gen.oneOf(Site.GN, Site.GS).map(s => DssiBlueprint(s))

  val GenIgrinsBlueprint: Gen[IgrinsBlueprint] =
    Gen.const(IgrinsBlueprint())

  val GenMaroonXBlueprint: Gen[MaroonXBlueprint] =
    Gen.const(MaroonXBlueprint())

  val GenZorroBlueprint: Gen[ZorroBlueprint] =
    Gen.oneOf(ZorroMode.SPECKLE, ZorroMode.WIDE_FIELD).map(m => ZorroBlueprint(m))

  implicit val GenVisitorBlueprint: Gen[VisitorBlueprint] =
    for {
      s <- Gen.oneOf(Site.GN, Site.GS)
      n <- Gen.oneOf(VisitorConfig.All.map(_.name))
    } yield VisitorBlueprint(s, n)

  implicit val ArbitraryGeminiBlueprint: Arbitrary[GeminiBlueprintBase] =
    Arbitrary{
      Gen.oneOf(
        GenAlopekeBlueprint,
        GenDssiBlueprint,
        GenIgrinsBlueprint,
        GenMaroonXBlueprint,
        GenZorroBlueprint,
        GenVisitorBlueprint
      )
    }

  def visitorInstrumentComponents(sp: ISPProgram): List[VisitorInstrument] =
    templateObservations(sp).flatMap { obs =>
      obs.findDescendant(_.getDataObject.getType == SPComponentType.INSTRUMENT_VISITOR).toList
    }.map(_.getDataObject.asInstanceOf[VisitorInstrument])


  "Visitor" should {

    "include all notes" in {
      forAll { (b: GeminiBlueprintBase) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          val notes = VisitorConfig.findByInstrument(b.instrument).toList.flatMap(_.noteTitles)
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }

    "set the position angle" in {
      forAll { (b: GeminiBlueprintBase) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          visitorInstrumentComponents(sp).forall { vi =>
            vi.getPosAngleDegrees == VisitorConfig.findByInstrument(b.instrument).map(_.positionAngle.toDegrees).getOrElse(0.0)
          }
        }
      }
    }

    "set the wavelength" in {
      forAll { (b: GeminiBlueprintBase) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          visitorInstrumentComponents(sp).forall { vi =>
            vi.getWavelength == VisitorConfig.findByInstrument(b.instrument).map(_.wavelength.toMicrons).getOrElse(0.0)
          }
        }
      }
    }
  }

}
