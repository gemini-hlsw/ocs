package edu.gemini.phase2.skeleton.factory

import edu.gemini.model.p1.immutable.PhoenixBlueprint
import edu.gemini.model.p1.mutable.{PhoenixFilter, PhoenixFocalPlaneUnit}
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.phoenix.InstPhoenix
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.util.SPTreeUtil

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

object SpPhoenixTemplateSpec extends TemplateSpec("PHOENIX_BP.xml") with Specification {

  def test(fpu: PhoenixFocalPlaneUnit, filter: PhoenixFilter) =
    expand(proposal(PhoenixBlueprint(fpu, filter), List(1), MagnitudeBand.R)) { (p, sp) =>
      s"Phoenic Blueprint Expansion $fpu $filter " >> {

        def group = groups(sp).head
        def obs   = group.getAllObservations.asScala
        def insts = obs.map(SPTreeUtil.findInstrument(_).getDataObject.asInstanceOf[InstPhoenix])
        def sci   = obs.find(_.getDataObject.asInstanceOf[SPObservation].getLibraryId == "1").get
        def scii  = SPTreeUtil.findInstrument(sci).getDataObject.asInstanceOf[InstPhoenix]
        val (exp, coadds) = filter.name.head match {
          case 'J' | 'H' | 'K' => (900.0, 1)
          case 'L'             => (120.0, 3)
          case 'M'             => ( 30.0, 4)
        }

        "There should be exactly one template group." in {
          groups(sp).size must_== 1
        }

        "Group should have human-readable name" in {
          groups(sp).forall(_.getDataObject.getTitle startsWith "Phoenix")
        }

        "It should contain all four observations." in {
          libs(group) == Set(1, 2, 3, 4)
        }

        "It should contain the how-to note." in {
          existsNote(group, "How to use the observations in this folder")
        }

        "It should contain the calibration note." in {
          existsNote(group, "Darks, Flats, and Arcs")
        }

        s"All obs should have FPU $fpu" in {
          insts.forall(_.getMask.name must_== fpu.name)
        }

        s"All obs should have filter $filter" in {
          insts.forall(_.getFilter.name must_== filter.name)
        }

        s"Science obs should have exposure $exp" in {
          scii.getExposureTime must_== exp
        }

        s"Science obs should have coadds $coadds" in {
          scii.getCoadds must_== coadds
        }

      }
    }

  /*
   * Test with every filter because exposure and coadds depend on it. We'll just pick an FPU
   * because it doesn't enter into any of the configuration decisions.
   */
  PhoenixFilter.values.foreach(test(PhoenixFocalPlaneUnit.MASK_2, _))

}
