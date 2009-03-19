/*
 * JmsEpicsManager.h
 *
 *  Created on: Feb 18, 2009
 *      Author: anunez
 */

#ifndef JMSEPICSMANAGER_H_
#define JMSEPICSMANAGER_H_

#include "EpicsManager.h"
#include "EpicsConfiguration.h"

#include <cms/Session.h>
#include <cms/Destination.h>
#include <cms/MessageProducer.h>

#include <util/JmsSmartPointers.h>
#include <util/giapiMaps.h>


#include <log4cxx/logger.h>

using namespace cms;

namespace giapi {

/**
 * An EpicsManager that uses JMS as the underlying
 * communication mechanism
 */
class JmsEpicsManager: public EpicsManager {
public:

	int subscribeEpicsStatus(const std::string &name,
			pEpicsStatusHandler handler) throw (GiapiException);

	int unsubscribeEpicsStatus(const std::string &name) throw (GiapiException);

	static pEpicsManager create() throw (CommunicationException);

	JmsEpicsManager() throw (CommunicationException);
	virtual ~JmsEpicsManager();


private:

	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

	/**
	 * The JMS Session associated to this producer.
	 */
	pSession _session;
		
	/**
	 * The Epics Configuration object
	 */
	pEpicsConfiguration _epicsConfiguration;

	/**
	 * Close open resources and destroy connections
	 */
	void cleanup();

};

}

#endif /* JMSEPICSMANAGER_H_ */
