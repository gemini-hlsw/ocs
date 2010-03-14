#include "CompletionInfoProducer.h"
#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>
#include <gmp/JmsUtil.h>

namespace gmp {
log4cxx::LoggerPtr CompletionInfoProducer::logger(log4cxx::Logger::getLogger("gmp.CompletionInfoProducer"));

CompletionInfoProducer::CompletionInfoProducer() throw (CommunicationException) {
	try {
		_connectionManager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

		//We will use a queue to send this messages to the GMP
		_destination = pDestination(_session->createQueue(GMPKeys::GMP_COMPLETION_INFO));
		//Instantiate the message producer for this destination
		_producer = pMessageProducer(_session->createProducer(_destination.get()));
	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing completion info producer :" + e.getMessage());
	}
}

CompletionInfoProducer::~CompletionInfoProducer() {
	LOG4CXX_DEBUG(logger, "Destroying Completion Info Producer");
	cleanup();
}

pCompletionInfoProducer CompletionInfoProducer::create() throw (CommunicationException) {
	pCompletionInfoProducer producer(new CompletionInfoProducer());
	return producer;
}


void CompletionInfoProducer::cleanup() {
	// Close open resources.

	try {
		if( _producer.get() != 0 ) _producer->close();
	} catch (CMSException& e) {e.printStackTrace();}

	try {
		if( _session.get() != 0 ) _session->close();
	} catch (CMSException& e) {e.printStackTrace();}

}

int CompletionInfoProducer::postCompletionInfo(command::ActionId id,
		pHandlerResponse response) throw (PostException) {

	MapMessage * reply = NULL;

	try {
		reply = _session->createMapMessage();
		JmsUtil::makeHandlerResponseMsg(reply, response);
		//store the action id in the message to send
		reply->setIntProperty(GMPKeys::GMP_ACTIONID_PROP, id);
		//send the reply
		_producer->send(reply);
	} catch (CMSException &e) {
		LOG4CXX_WARN(logger, "Problem posting completion info: " + e.getMessage());
		if (reply != NULL) delete reply;
		throw PostException("Problem posting completion info : " + e.getMessage());
	}
	//if we are here, everything went okay. Destroy the reply and return OK
	if (reply != NULL) delete reply;
	return giapi::status::OK;
}

}
