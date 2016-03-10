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
		_connectionManager = ConnectionManager::Instance();
		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

		//Instantiate the message producer
		_producer = pMessageProducer(_session->createProducer(NULL));
                _producer->setDeliveryMode(DeliveryMode::NON_PERSISTENT);
                _producer->setTimeToLive(10 * 1000);
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

int JmsStatusSender::postStatus(pStatusItem statusItem) const
		throw (PostException) {
	LOG4CXX_DEBUG(logger, "Post Status Item " << statusItem->getName());

	BytesMessage *msg = NULL;

	try {
		//create a bytes message
		msg = _session->createBytesMessage();

		//ask the appropriate visitor to complete the message
		StatusSerializerVisitor serializer(msg);
		statusItem->accept(serializer);

		//Create the destination...
		//We will use a topic to send the status to the JMS Broker
		Destination * destination = _session->createTopic(
				GMPKeys::GMP_STATUS_DESTINATION_PREFIX + statusItem->getName());
		//and dispatch the message
		_producer->send(destination, msg);

		delete destination;

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
