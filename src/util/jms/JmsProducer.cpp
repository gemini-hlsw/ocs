/*
 * JmsProducer.cpp
 *
 *  Created on: Jun 10, 2009
 *      Author: anunez
 */

#include <util/jms/JmsProducer.h>
#include <gmp/ConnectionManager.h>


namespace giapi {
namespace util {
namespace jms {

log4cxx::LoggerPtr JmsProducer::logger(log4cxx::Logger::getLogger(
		"giapi.JmsProducer"));


JmsProducer::JmsProducer(const std::string& queueName) throw (CommunicationException){

	try {
		_connectionManager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

		//We will use a topic to send data
		_destination = pDestination(_session->createTopic(
				queueName));
		//Instantiate the message producer for this destination
		_producer = pMessageProducer(_session->createProducer(
				_destination.get()));
	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing request producer :"
				+ e.getMessage());
	}
}


//pJmsProducer JmsProducer::create(const std::string &name) throw (CommunicationException) {
//	pJmsProducer producer(new JmsProducer(name));
//	return producer;
//}

JmsProducer::~JmsProducer() {
	LOG4CXX_DEBUG(logger, "Destroying Generic JMS Producer");
	cleanup();
}


void JmsProducer::cleanup() {
	// Close open resources.
	try {
		if (_producer.get() != 0)
			_producer->close();
	} catch (CMSException& e) {
		e.printStackTrace();
	}

	try {
		if (_session.get() != 0)
			_session->close();
	} catch (CMSException& e) {
		e.printStackTrace();
	}
}

}

}
}
