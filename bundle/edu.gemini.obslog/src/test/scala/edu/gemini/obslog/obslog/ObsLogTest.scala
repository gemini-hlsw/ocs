package edu.gemini.obslog.obslog

import edu.gemini.obslog.actions.FetchObservationAction
import edu.gemini.obslog.config.OlDefaultConfigurationProducer
import edu.gemini.obslog.database.pot.OlPOTLocalPersistenceManager
import edu.gemini.pot.client.SPDB
import edu.gemini.pot.spdb.DBLocalDatabase
import org.junit.Test

class ObsLogTest {
  @Test def testLog(): Unit = {
    val db = DBLocalDatabase.createTransient()
    SPDB.init(db)
    val prog = ???
    db.put(prog)
    val id = "GN-2015A-DD-10-127"
    val configProducer = new OlDefaultConfigurationProducer()
    val persistenceManager = new OlPOTLocalPersistenceManager()
    val action = new FetchObservationAction
    action.setObservationID(id)
    action.setObsLogConfigurationProducer(configProducer)
    action.setPersistenceManager(persistenceManager)
    action.execute()
    val log = action.getObservingLog
    log.dump()
  }
}
