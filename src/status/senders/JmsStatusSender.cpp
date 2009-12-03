#include "JmsStatusSender.h"
#include <giapi/giapi.h>

#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>
#include <status/senders/jms-writer/StatusSerializerVisitor.h>
using namespace gmp;

namespace giapi {

log4cxx::LoggerPtr JmsStatusSender::logger(log4cxx::Logger::getLogger(
		"giapi.JmsStatusSender"));

JmsStatusSender::JmsStatusSender() throw (CommunicationException) {
	LOG4CXX_DEBUG(logger, "Constructing JMS Status sender");
	try {
		ConnectionManager& manager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = pSession(manager.createSession());

		//We will use a queue to send requests to the GMP
		_destination = pDestination(_session->createTopic(
				GMPKeys::GMP_STATUS_DESTINATION));
		//Instantiate the message producer for this destination
		_producer = pMessageProducer(_session->createProducer(
				_destination.get()));
	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing Status Sender :"
				+ e.getMessage());
	}

}

JmsStatusSender::~JmsStatusSender() {
	LOG4CXX_DEBUG(logger, "Destroying JMS Status sender");
	cleanup();
}

int JmsStatusSender::postStatus(StatusItem * statusItem) const
		throw (PostException) {
	LOG4CXX_DEBUG(logger, "Post Status Item " << *statusItem);

	BytesMessage *msg = NULL;

	try {
		//create a bytes message
		msg = _session->createBytesMessage();

		//ask the appropriate visitor to complete the message
		StatusSerializerVisitor serializer(msg);
		statusItem->accept(serializer);

		//and dispatch the message
		_producer->send(msg);

	} catch (CMSException &ex) {
		LOG4CXX_WARN(logger, "Problem posting status: " + ex.getMessage());
		if (msg != NULL)
			delete msg;
		throw PostException("Problem posting status : " + ex.getMessage());
	}

	//if we are here, everything went okay. Destroy the reply and return OK
	if (msg != NULL) delete msg;
	return giapi::status::OK;
}

void JmsStatusSender::cleanup() {
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
