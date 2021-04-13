// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.{Site, VisitorBlueprint}
import edu.gemini.phase2.template.factory.impl.visitor.VisitorInst
import edu.gemini.pot.sp.{ISPProgram, SPComponentType}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.spModel.rich.pot.sp._
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike

class VisitorBlueprintSpec extends TemplateSpec("VISITOR_BP.xml") with SpecificationLike with ScalaCheck {

  implicit val ArbitraryVisitorBlueprint: Arbitrary[VisitorBlueprint] =
    Arbitrary {
      for {
        s <- Gen.oneOf(Site.GN, Site.GS)
        n <- Gen.oneOf(VisitorInst.All.map(_.name))
      } yield VisitorBlueprint(s, n)
    }

  def visitorInstrumentComponents(sp: ISPProgram): List[VisitorInstrument] =
    templateObservations(sp).flatMap { obs =>
      obs.findDescendant(_.getDataObject.getType == SPComponentType.INSTRUMENT_VISITOR).toList
    }.map(_.getDataObject.asInstanceOf[VisitorInstrument])


  "Visitor" should {

    "include all notes" in {
      forAll { (b: VisitorBlueprint) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          val notes = VisitorInst.findByName(b.customName).toList.flatMap(_.noteTitles)
          groups(sp).forall(tg => notes.forall(existsNote(tg, _)))
        }
      }
    }

    "set the position angle" in {
      forAll { (b: VisitorBlueprint) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          visitorInstrumentComponents(sp).forall { vi =>
            vi.getPosAngleDegrees == VisitorInst.findByName(b.customName).map(_.positionAngle.toDegrees).getOrElse(0.0)
          }
        }
      }
    }

    "set the wavelength" in {
      forAll { (b: VisitorBlueprint) =>
        expand(proposal(b, Nil, MagnitudeBand.R)) { (_, sp) =>
          visitorInstrumentComponents(sp).forall { vi =>
            vi.getWavelength == VisitorInst.findByName(b.customName).map(_.wavelength.toMicrons).getOrElse(0.0)
          }
        }
      }
    }
  }

}
