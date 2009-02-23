/*
 * JmsEpicsManager.cpp
 *
 *  Created on: Feb 18, 2009
 *      Author: anunez
 */

#include <gemini/JmsEpicsManager.h>

#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>

using namespace gmp;

namespace giapi {

log4cxx::LoggerPtr JmsEpicsManager::logger(log4cxx::Logger::getLogger(
		"giapi::gemini::JmsEpicsManager"));

JmsEpicsManager::JmsEpicsManager() throw (CommunicationException) {
	try {
		ConnectionManager& manager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = pSession(manager.createSession());

		//We will use a queue to send requests to the GMP
		_destination = pDestination(_session->createQueue(
				GMPKeys::GMP_GEMINI_EPICS_REQUEST_DESTINATION));
		//Instantiate the message producer for this destination
		_producer = pMessageProducer(_session->createProducer(
				_destination.get()));

		//Initialize the valid epics status channels
		init();

	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing request producer :"
				+ e.getMessage());
	}
}

void JmsEpicsManager::cleanup() {
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

JmsEpicsManager::~JmsEpicsManager() {
	// TODO Auto-generated destructor stub
}

void JmsEpicsManager::init() throw (CommunicationException) {
	//get the valid epics channels
	try {
		_epicsChannels = getValidEpicsChannels(1000);
	} catch (TimeoutException &ex) {
		LOG4CXX_WARN(logger, "Can not get list of valid Epics channels from the GMP. Will attempt later.");
		_epicsChannels.clear();
	}
}

std::set<std::string> JmsEpicsManager::getValidEpicsChannels(long timeout)
		throw (CommunicationException, TimeoutException) {

	Message *request = NULL;

	std::set<std::string> channels;

	try {
		request = _session->createMessage();
		request->setBooleanProperty(GMPKeys::GMP_GEMINI_EPICS_CHANNEL_PROPERTY,
				true);
		//create temporary objects to get the answer
		TemporaryQueue* tmpQueue = _session->createTemporaryQueue();
		MessageConsumer * tmpConsumer = _session->createConsumer(tmpQueue);

		request->setCMSReplyTo(tmpQueue);

		//send the reply
		_producer->send(request);
		//and wait for the response.
		Message *reply = (timeout > 0) ? tmpConsumer->receive(timeout)
				: tmpConsumer->receive();

		tmpConsumer->close();
		delete tmpConsumer;

		tmpQueue->destroy();
		delete tmpQueue;

		if (request != NULL)
			delete request;

		if (reply != NULL) {
			MapMessage *mm = (MapMessage *) reply;
			//get the values and store them in the map
			std::vector<std::string> mapNames = mm->getMapNames();
			for (std::vector<std::string>::iterator it = mapNames.begin(); it
					!= mapNames.end(); it++) {
				channels.insert(*it);
			}
		} else { //timeout.
			throw TimeoutException(
					"Time out trying to get valid Epics Channel names ");
		}
	} catch (CMSException &e) {
		LOG4CXX_WARN(logger, "Problem getting valid epics channel :" + e.getMessage());
		if (request != NULL)
			delete request;
		throw PostException("Problem getting valid epics channel : "
				+ e.getMessage());
	}

	return channels;
}

pEpicsManager JmsEpicsManager::create() throw (CommunicationException) {
	pEpicsManager mgr(new JmsEpicsManager());
	return mgr;
}

int JmsEpicsManager::subscribeEpicsStatus(const std::string & name,
		pEpicsStatusHandler handler) throw (GiapiException) {

	if (_epicsChannels.empty()) { //not initialized
		//attempt to initialize it
		init();
	}

	if ( _epicsChannels.find(name) != _epicsChannels.end() ) {
		//epics channel found
		return status::OK;
	} else {
		//not found
		return status::ERROR;
	}
}

int JmsEpicsManager::unsubscribeEpicsStatus(const std::string & name)
		throw (GiapiException) {
	return 0;
}

}
