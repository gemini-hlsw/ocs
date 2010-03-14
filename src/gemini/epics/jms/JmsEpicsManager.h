/*
 * JmsEpicsManager.h
 *
 *  Created on: Feb 18, 2009
 *      Author: anunez
 */

#ifndef JMSEPICSMANAGER_H_
#define JMSEPICSMANAGER_H_

#include <gemini/epics/EpicsManager.h>
#include <gemini/epics/EpicsConfiguration.h>
#include <gemini/epics/jms/EpicsConsumer.h>

#include <cms/Session.h>
#include <cms/Destination.h>
#include <cms/MessageProducer.h>

#include <util/JmsSmartPointers.h>
#include <util/giapiMaps.h>
#include <gmp/ConnectionManager.h>

#include <log4cxx/logger.h>

using namespace cms;
using namespace gmp;

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
	 * The connection manager
	 */
	pConnectionManager _connectionManager;

	/**
	 * The Epics Configuration object
	 */
	pEpicsConfiguration _epicsConfiguration;


	/**
	 * Type definition for the hash_table that will map EPICS channel names to
	 * the consumer that is receiving the updates
	 */
	typedef hash_map<std::string, pEpicsConsumer> EpicsConsumersMap;

	EpicsConsumersMap _epicsConsumersMap;


	/**
	 * Close open resources and destroy connections
	 */
	void cleanup();

};

}

#endif /* JMSEPICSMANAGER_H_ */
