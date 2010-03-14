/*
 * JmsProducer.h
 *
 *  Created on: Jun 10, 2009
 *      Author: anunez
 */

#ifndef JMSPRODUCER_H_
#define JMSPRODUCER_H_

#include <giapi/giapiexcept.h>
#include <util/JmsSmartPointers.h>
#include <gmp/ConnectionManager.h>

#include <cms/Session.h>
#include <cms/Destination.h>
#include <cms/MessageProducer.h>

#include <log4cxx/logger.h>

using namespace gmp;

namespace giapi {
namespace util {
namespace jms {



class JmsProducer {
public:
	virtual ~JmsProducer();

protected:
	/**
	 * Constructor
	 */
	JmsProducer(const std::string & queueName) throw (CommunicationException);

	/**
	 * The JMS Session associated to this producer.
	 */
	pSession _session;

	/**
	 * The message producer in charge of sending requests down to
	 * the GMP. Runs on its own session
	 */
	pMessageProducer _producer;
	
	/**
	 * The connection manager
	 */
	pConnectionManager _connectionManager;

	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

private:


	/**
	 * The virtual channel to where this producer will send messages to
	 */
	pDestination _destination;


	/**
	 * Close open resources and destroy connections
	 */
	void cleanup();

};

}
}
}

#endif /* JMSPRODUCER_H_ */
