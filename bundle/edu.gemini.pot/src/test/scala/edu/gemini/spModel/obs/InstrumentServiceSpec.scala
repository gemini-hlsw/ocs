package edu.gemini.spModel.obs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

object InstrumentServiceSpec extends Specification {

  private val Key = new SPNodeKey
  private val Pid = SPProgramID.toProgramID("GS-2019A-Q-1")

  private def withTestObservation[A](f: (ISPObservation, IDBDatabaseService) => MatchResult[A]): MatchResult[A] = {

    val odb = DBLocalDatabase.createTransient

    try {
      val p = odb.getFactory.createProgram(Key, Pid)
      odb.put(p)

      val o = odb.getFactory.createObservation(p, Instrument.none, null)
      p.addObservation(o)

      f(o, odb)

    } finally {
      odb.getDBAdmin.shutdown
    }

  }

  private def addInstrument(i: Instrument, o: ISPObservation, odb: IDBDatabaseService): Unit = {

    val oc = odb.getFactory.createObsComponent(o.getProgram, i.componentType, null)
    o.addObsComponent(oc)

  }

  private def replaceInstrument(oi: Option[Instrument], o: ISPObservation, odb: IDBDatabaseService): Unit = {

    val c0 = o.getObsComponents.asScala.filterNot(_.getType.broadType == INSTRUMENT).toList

    val c1 = oi.fold(c0) { i =>
      odb.getFactory.createObsComponent(o.getProgram, i.componentType, null) :: c0
    }

    o.setObsComponents(c1.asJava)

  }


  "InstrumentService" should {

    "find nothing if there is no instrument" in {
      withTestObservation { (o, odb) =>
        InstrumentService.lookupInstrument(o) shouldEqual Instrument.none
      }
    }

    "find the correct instrument when available" in {
      withTestObservation { (o, odb) =>
        addInstrument(Instrument.GmosSouth, o, odb)
        InstrumentService.lookupInstrument(o) shouldEqual Instrument.GmosSouth.some
      }
    }

    "track instrument changes" in {
      withTestObservation { (o, odb) =>
        addInstrument(Instrument.GmosSouth, o, odb)
        val i0 = InstrumentService.lookupInstrument(o)
        replaceInstrument(Some(Instrument.Flamingos2), o, odb)
        val i1 = InstrumentService.lookupInstrument(o)

        (i0 shouldEqual Instrument.GmosSouth.some) and (i1 shouldEqual Instrument.Flamingos2.some)
      }
    }

  }

  "SPObsCache" should {

    "find nothing after initialization" in {
      withTestObservation { (o, odb) =>
        SPObsCache.setObsCache(o, new SPObsCache)
        SPObsCache.getObsCache(o).getInstrument shouldEqual Instrument.none
      }
    }

    "find the instrument after InstrumentService stores it" in {
      withTestObservation { (o, odb) =>
        addInstrument(Instrument.GmosSouth, o, odb)
        InstrumentService.lookupInstrument(o)
        SPObsCache.getObsCache(o).getInstrument shouldEqual Instrument.GmosSouth.some
      }
    }

    def updateTest(mod: (ISPObservation, IDBDatabaseService) => Unit): MatchResult[Any] =
      withTestObservation { (o, odb) =>
        addInstrument(Instrument.GmosSouth, o, odb)
        InstrumentService.lookupInstrument(o)

        val i0 = SPObsCache.getObsCache(o).getInstrument

        mod(o, odb)

        val i1 = SPObsCache.getObsCache(o).getInstrument
        val i2 = InstrumentService.lookupInstrument(o)
        val i3 = SPObsCache.getObsCache(o).getInstrument

        (i0 shouldEqual Instrument.GmosSouth.some)   and
          (i1 shouldEqual Instrument.none)           and
          (i2 shouldEqual Instrument.GmosSouth.some) and
          (i3 shouldEqual Instrument.GmosSouth.some)
      }

    "be reset if an observation property changes" in {
      updateTest { (o, odb) =>
        val d = o.getDataObject
        d.setTitle("Foo")
        o.setDataObject(d)
      }
    }

    "be reset if a child observation component property changes" in {
      updateTest { (o, odb) =>
        val oc = o.getObsComponents.asScala.head
        val d  = oc.getDataObject
        d.setTitle("Foo")
        oc.setDataObject(d)
      }
    }

    "be reset if the observation structure changes" in {
      updateTest { (o, odb) =>
        val sc = odb.getFactory.createSeqComponent(o.getProgram, SPComponentType.OBSERVER_OBSERVE, null)
        o.getSeqComponent.addSeqComponent(sc)
      }
    }
  }

}
