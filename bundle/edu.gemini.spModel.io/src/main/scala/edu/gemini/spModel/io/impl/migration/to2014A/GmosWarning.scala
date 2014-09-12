package edu.gemini.spModel.io.impl.migration.to2014A

import edu.gemini.pot.sp.{ISPSeqComponent, SPComponentType, ISPObservation}
import edu.gemini.pot.sp.SPComponentType.{INSTRUMENT_GMOS, INSTRUMENT_GMOSSOUTH, ITERATOR_GMOS, ITERATOR_GMOSSOUTH}

import scala.collection.JavaConverters._
import java.util.logging.Logger

/**
 * Some old programs have GMOS-N instruments with GMOS-S sequences and vice
 * versa.  We want to warn when that happens so the programs can be corrected
 * manually.
 */
object GmosWarning {
  val Log = Logger.getLogger(this.getClass.getName)

  val GmosTypes = Set(INSTRUMENT_GMOS, INSTRUMENT_GMOSSOUTH)
  val OppositeIterator = Map(INSTRUMENT_GMOS -> ITERATOR_GMOSSOUTH, INSTRUMENT_GMOSSOUTH -> ITERATOR_GMOS)

  def warnIfNecessary(obs: ISPObservation): Unit = {
    def hasIterator(sc: ISPSeqComponent, iterType: SPComponentType): Boolean =
      if (sc.getType == iterType) true
      else sc.getSeqComponents.asScala.exists(hasIterator(_, iterType))

    Option(obs.getProgramID).foreach { _ =>
      obs.getObsComponents.asScala.find(oc => GmosTypes.contains(oc.getType)).map(_.getType).foreach { t =>
        Option(obs.getSeqComponent).foreach { sc =>
          if (hasIterator(sc, OppositeIterator(t))) {
            Log.severe(s"*** Observation ${obs.getObservationID} has mismatched GMOS sequence.")
          }
        }
      }
    }
  }
}
