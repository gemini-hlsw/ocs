package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.GnirsBlueprintSpectroscopy
import edu.gemini.model.p1.mutable.{GnirsCentralWavelength, GnirsCrossDisperser}
import edu.gemini.model.p1.mutable.GnirsCentralWavelength._
import edu.gemini.pot.sp.{ISPObservation, SPComponentType}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.SpecificationLike
import edu.gemini.spModel.rich.pot.sp._
import scala.collection.JavaConverters._

class REL_3376_Test extends TemplateSpec("GNIRS_BP.xml") with SpecificationLike with ScalaCheck {

  // Get the Q-offsets for the "ABBA offset sequence" offset iterator as a List[Int]
  private def qOffset(obs: ISPObservation): List[Int] =
    obs.findSeqComponentsByType(SPComponentType.ITERATOR_OFFSET)
      .map(_.getDataObject)
      .find(_.getTitle == "ABBA offset sequence")
      .getOrElse(sys.error("Offset sequence was not found."))
      .asInstanceOf[SeqRepeatOffset]
      .getPosList
      .iterator
      .asScala
      .toList
      .map(_.getYaxis.toInt)

  // Gen for non-cross-dispersed GnirsBlueprintSpectroscopy with the given GnirsCentralWavelength
  private def nonXD(λ: GnirsCentralWavelength): Gen[GnirsBlueprintSpectroscopy] =
    GnirsBlueprintSpec.ArbitraryBlueprintSpectroscopy.arbitrary.map { bp =>
      bp.copy(centralWavelength = λ, crossDisperser = GnirsCrossDisperser.NO)
    }

  "ABBA iterators" >> {

    // IF CROSS-DISPERSED == No:
    //    IF PI Central Wavelength < 2.5um:
    //       SET Q-OFFSET to -2, 4, 4, -2 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {12}          # Science
    //       SET Q-OFFSET to -4, 2, 2, -4 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {6}, {14}     # Tellurics
    //    ELSE:
    //       SET Q-OFFSET to -1, 5, 5, -1 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {6},{12},{14} # Science & Tellurics

    "For Central Wavelength < 2.5um" ! forAll(nonXD(LT_25)) { bp =>
      expand(proposal(bp, List(6.5, 10, 21, 25), MagnitudeBand.H)) { (p, sp) =>
        groups(sp).map(libsMap).forall { m =>
          qOffset(m(12)) must_== List(-2, 4, 4, -2)
          qOffset(m(6))  must_== List(-4, 2, 2, -4)
          qOffset(m(14)) must_== List(-4, 2, 2, -4)
        }
      }
    }

    "For Central Wavelength ≥ 2.5um" ! forAll(nonXD(GTE_25)) { bp =>
      expand(proposal(bp, List(6.5, 10, 21, 25), MagnitudeBand.H)) { (p, sp) =>
        groups(sp).map(libsMap).forall { m =>
          qOffset(m(6))  must_== List(-1, 5, 5, -1)
          qOffset(m(12)) must_== List(-1, 5, 5, -1)
          qOffset(m(14)) must_== List(-1, 5, 5, -1)
        }
      }
    }

    // N.B. the deletion of this mutation is verified by the checks above.
    // -- SET Q-OFFSET to -3, 3, 3, -3 respectively IN ITERATOR CALLED 'ABBA offset pattern' for {6},{12},{14} # Science & Tellurics

  }

}