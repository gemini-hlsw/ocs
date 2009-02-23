/*
 * JmsEpicsManager.h
 *
 *  Created on: Feb 18, 2009
 *      Author: anunez
 */

#ifndef JMSEPICSMANAGER_H_
#define JMSEPICSMANAGER_H_

#include "EpicsManager.h"

#include <cms/Session.h>
#include <cms/Destination.h>
#include <cms/MessageProducer.h>

#include <util/JmsSmartPointers.h>
#include <util/giapiMaps.h>

#include <log4cxx/logger.h>

#include <set>

using namespace cms;

namespace giapi {

class JmsEpicsManager: public EpicsManager {
public:

	int subscribeEpicsStatus(const std::string &name,
			pEpicsStatusHandler handler) throw (GiapiException);

	int unsubscribeEpicsStatus(const std::string &name) throw (GiapiException);

	std::set<std::string> getValidEpicsChannels(long timeot)
			throw (CommunicationException, TimeoutException);

	static pEpicsManager create() throw (CommunicationException);

	JmsEpicsManager() throw (CommunicationException);
	virtual ~JmsEpicsManager();


private:

	void init() throw (CommunicationException);
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

	/**
	 * A set of valid epics channels; obtained from the GMP
	 */

	std::set<std::string> _epicsChannels;

	/**
	 * The JMS Session associated to this producer.
	 */
	pSession _session;

	/**
	 * The virtual channel to where this producer will send messages to
	 */
	pDestination _destination;

	/**
	 * The message producer in charge of sending requests down to
	 * the GMP. Runs on its own session
	 */
	pMessageProducer _producer;

	/**
	 * Close open resources and destroy connections
	 */
	void cleanup();

};

}

#endif /* JMSEPICSMANAGER_H_ */
