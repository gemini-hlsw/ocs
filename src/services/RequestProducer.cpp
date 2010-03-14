/*
 * RequestProducer.cpp
 *
 *  Created on: Dec 10, 2008
 *      Author: anunez
 */

#include <services/RequestProducer.h>
#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>

namespace giapi {

log4cxx::LoggerPtr RequestProducer::logger(log4cxx::Logger::getLogger(
		"giapi.RequestProducer"));

RequestProducer::RequestProducer() throw (CommunicationException) {
	try {
		_connectionManager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

		//We will use a queue to send requests to the GMP
		_destination = pDestination(_session->createQueue(
				GMPKeys::GMP_UTIL_REQUEST_DESTINATION));
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

RequestProducer::~RequestProducer() {
	LOG4CXX_DEBUG(logger, "Destroying Util Request Producer");
	cleanup();
}

pRequestProducer RequestProducer::create() throw (CommunicationException) {
	pRequestProducer producer(new RequestProducer());
	return producer;
}

void RequestProducer::cleanup() {
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

std::string RequestProducer::getProperty(const std::string &key, long timeout)
		throw (CommunicationException, TimeoutException) {

	//prepare a message to the GMP, requesting the property

	MapMessage * request = NULL;
	std::string answer;
	try {
		request = _session->createMapMessage();
		//Request Type is stored as a property
		request->setIntProperty(GMPKeys::GMP_UTIL_REQUEST_TYPE,
				GMPKeys::GMP_UTIL_REQUEST_PROPERTY);
		request->setString(GMPKeys::GMP_UTIL_PROPERTY, key);

		//create temporary objects to get the answer
		TemporaryQueue* tmpQueue = _session->createTemporaryQueue();
		MessageConsumer * tmpConsumer = _session->createConsumer(tmpQueue);

		request->setCMSReplyTo(tmpQueue);

		//send the reply
		_producer->send(request);
		//destroy the request, not needed anymore
		delete request;

		//and wait for the response.
		Message *reply =
			(timeout > 0) ? tmpConsumer->receive(timeout) :
				tmpConsumer->receive();

		tmpConsumer->close();
		delete tmpConsumer;

		tmpQueue->destroy();
		delete tmpQueue;

		if (reply != NULL) {
			TextMessage *mm = (TextMessage *)reply;
			answer = mm->getText();
		} else { //timeout. Throw an exception
			throw TimeoutException("Time out while waiting for property " + key);
		}
	} catch (CMSException &e) {
		LOG4CXX_WARN(logger, "Problem sending utility request: " + e.getMessage());
		if (request != NULL)
			delete request;
		throw PostException("Problem sending utility request : "
				+ e.getMessage());
	}

	return answer;
}

}
