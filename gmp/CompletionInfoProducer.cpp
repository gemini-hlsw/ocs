#include "CompletionInfoProducer.h"
#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>
#include <gmp/JmsUtil.h>

namespace gmp {
log4cxx::LoggerPtr CompletionInfoProducer::logger(log4cxx::Logger::getLogger("gmp.CompletionInfoProducer"));

CompletionInfoProducer::CompletionInfoProducer() {
	try {
		ConnectionManager& manager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = pSession(manager.createSession());
		
		//We will use a queue to send this messages to the GMP
		_destination = pDestination(_session->createQueue(GMPKeys::GMP_COMPLETION_INFO));
		//Instantiate the message producer for this destination
		_producer = pMessageProducer(_session->createProducer(_destination.get()));
	} catch (CMSException& e) {
		//clean anyresources that might have been allocated
		cleanup();
		e.printStackTrace();
	}
}

CompletionInfoProducer::~CompletionInfoProducer() {
	LOG4CXX_DEBUG(logger, "Destroying Completion Info Producer");
	cleanup();
}

pCompletionInfoProducer CompletionInfoProducer::create() {
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
		pHandlerResponse response) {
	
	MapMessage * reply = NULL;
	
	try {
		reply = _session->createMapMessage();
		JmsUtil::makeHandlerResponseMsg(reply, response);
		//store the action id in the message to send
		reply->setIntProperty(GMPKeys::GMP_ACTIONID_PROP, id);
		//send the reply
		_producer->send(reply);
		//delete allocated objects
	} catch (CMSException &e) {
		LOG4CXX_WARN(logger, "Problem posting completion info: " + e.getMessage());
		e.printStackTrace();
		if (reply != NULL) delete reply;
		//return error
		return giapi::status::ERROR;
	}
	//if we are here, everything went okay. Destroy the reply and return OK
	if (reply != NULL) delete reply;
	return giapi::status::OK;
}

}
