// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.p2checker.rules

import edu.gemini.p2checker.rules.flamingos2.Flamingos2Rule
import edu.gemini.pot.sp.{ISPFactory, ISPObservation, ISPProgram, SPComponentType}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser
import edu.gemini.pot.sp.SPComponentType.INSTRUMENT_FLAMINGOS2
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.seqcomp.SeqRepeatObserve

final class Flamingos2RuleSpec extends RuleSpec {

  val ruleSet = new Flamingos2Rule()

  "Disperser in acquisition rule" should {

    val AcqErrId = "Flamingos2Rule_ACQUISITION_RULE"

    def addObserve(c: ObsClass, p: ISPProgram, o: ISPObservation, f: ISPFactory): Unit = {
      val sc   = f.createSeqComponent(p, SPComponentType.OBSERVER_OBSERVE, null)
      val dobj = sc.getDataObject.asInstanceOf[SeqRepeatObserve]
      dobj.setObsClass(c)
      sc.setDataObject(dobj)
      o.getSeqComponent.addSeqComponent(sc)
    }

    "give no warning when disperser is none for ACQ" in {
      expectNoneOf(AcqErrId) {
        advancedSetup[Flamingos2](INSTRUMENT_FLAMINGOS2) { (p, o, d, f) =>
          d.setDisperser(Disperser.NONE)
          addObserve(ObsClass.ACQ, p, o, f)
        }
      }
    }

    "not warn when disperser is NONE for ACQ_CAL" in {
      expectNoneOf(AcqErrId) {
        advancedSetup[Flamingos2](INSTRUMENT_FLAMINGOS2) { (p, o, d, f) =>
          d.setDisperser(Disperser.NONE)
          addObserve(ObsClass.ACQ_CAL, p, o, f)
        }
      }
    }

    "warn when disperser is not NONE for ACQ" in {
      expectAllOf(AcqErrId) {
        advancedSetup[Flamingos2](INSTRUMENT_FLAMINGOS2) { (p, o, d, f) =>
          d.setDisperser(Disperser.R3000)
          addObserve(ObsClass.ACQ, p, o, f)
        }
      }
    }

    "warn when disperser is not NONE for ACQ_CAL" in {
      expectAllOf(AcqErrId) {
        advancedSetup[Flamingos2](INSTRUMENT_FLAMINGOS2) { (p, o, d, f) =>
          d.setDisperser(Disperser.R3000)
          addObserve(ObsClass.ACQ_CAL, p, o, f)
        }
      }
    }

    "not warn for science observes" in {
      expectNoneOf(AcqErrId) {
        advancedSetup[Flamingos2](INSTRUMENT_FLAMINGOS2) { (p, o, d, f) =>
          d.setDisperser(Disperser.R3000)
          addObserve(ObsClass.SCIENCE, p, o, f)
        }
      }
    }
  }

}
