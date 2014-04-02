#include "JmsEpicsConfiguration.h"
#include <gmp/GMPKeys.h>

namespace giapi {

log4cxx::LoggerPtr JmsEpicsConfiguration::logger(log4cxx::Logger::getLogger(
		"giapi::gemini::JmsEpicsConfiguration"));


JmsEpicsConfiguration::JmsEpicsConfiguration(pSession session)  {
	_session = session;
}

JmsEpicsConfiguration::~JmsEpicsConfiguration() {

}

void JmsEpicsConfiguration::init() throw (CommunicationException, TimeoutException)  {
	//request the channles
	requestChannels(1000);
}

bool JmsEpicsConfiguration::isInitialized() const {
	return _epicsChannels.size() > 0;
}

bool JmsEpicsConfiguration::hasChannel(const std::string & channel) {
	return (_epicsChannels.find(channel) != _epicsChannels.end());
}


pEpicsConfiguration JmsEpicsConfiguration::create(pSession session) {
	pEpicsConfiguration conf(new JmsEpicsConfiguration(session));
	return conf;
}

void JmsEpicsConfiguration::requestChannels(long timeout) throw (CommunicationException, TimeoutException) {

	Message *request= NULL;

	try {
		//We will use a queue to send the request to the GMP
		pDestination dest = pDestination(_session->createQueue(
						gmp::GMPKeys::GMP_GEMINI_EPICS_REQUEST_DESTINATION));
		//Instantiate the message producer for this destination
		pMessageProducer producer = pMessageProducer(_session->createProducer(
						dest.get()));

		request = _session->createMessage();
		request->setBooleanProperty(gmp::GMPKeys::GMP_GEMINI_EPICS_CHANNEL_PROPERTY,
				true);

		//create temporary objects to get the answer
		TemporaryQueue* tmpQueue = _session->createTemporaryQueue();
		pMessageConsumer tmpConsumer = pMessageConsumer(
				_session->createConsumer(tmpQueue));

		request->setCMSReplyTo(tmpQueue);

		//send the reply
		producer->send(request);
		//close the producer
		producer->close();

		//and wait for the response.
		Message *reply = (timeout> 0) ? tmpConsumer->receive(timeout)
		: tmpConsumer->receive();

		tmpConsumer->close();

		tmpQueue->destroy();
		delete tmpQueue;
		delete request;
		if (reply != NULL) {
			MapMessage *mm = (MapMessage *) reply;
			//get the values and store them in the map
			std::vector<std::string> mapNames = mm->getMapNames();
			for (std::vector<std::string>::iterator it = mapNames.begin(); it
					!= mapNames.end(); it++) {
				_epicsChannels.insert(*it);
				LOG4CXX_DEBUG(logger, "Authorizing Epics channel: " + *it);
			}
		} else { //timeout.
			throw TimeoutException(
					"Time out trying to get valid Epics Channel names ");
		}
	} catch (CMSException &e) {
		LOG4CXX_WARN(logger, "Problem getting valid epics channel :" + e.getMessage());
		if (request != NULL) delete request;
		throw PostException("Problem getting valid epics channel : "
				+ e.getMessage());
	}
}
}

