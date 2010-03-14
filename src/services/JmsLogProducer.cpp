#include "JmsLogProducer.h"
#include <gmp/GMPKeys.h>


using namespace gmp;

namespace giapi {

JmsLogProducer::JmsLogProducer() throw (CommunicationException) :
	JmsProducer(GMPKeys::GMP_SERVICES_LOG_DESTINATION) {
}

JmsLogProducer::~JmsLogProducer() {
}

pJmsLogProducer JmsLogProducer::create() throw (CommunicationException) {
	pJmsLogProducer producer(new JmsLogProducer());
	return producer;
}

void JmsLogProducer::postLog(log::Level level, const std::string &logMsg)
		throw (CommunicationException) {

	TextMessage * msg = NULL;
	try {
		/**
		 * We convert the level to an integer explicitly, to
		 * prevent problems if the enumeration order is altered.
		 */
		int intLevel = 1; //default is INFO
		switch(level) {
		case log::INFO:
			intLevel = 1;
			break;
		case log::WARNING:
			intLevel = 2;
			break;
		case log::SEVERE:
			intLevel = 3;
			break;
		default:
			return; //do nothing
		}
		/**
		 * Create a Text Message to send the log information
		 */
		msg = _session->createTextMessage();

		/**
		 * The level is sent as a property in the message (this allows
		 * easier filtering by clients, for instance)
		 */
		msg->setIntProperty(GMPKeys::GMP_SERVICES_LOG_LEVEL, intLevel);
		/**
		 * The log message itself is encoded in the text message
		 */
		msg->setText(logMsg);
		/**
		 * And we dispatch the message
		 */
		_producer->send(msg);

		/**
		 * Destroy the message
		 */
		delete msg;

	} catch (CMSException &e) {
		if (msg != NULL) {
			delete msg;
		}
		throw CommunicationException("Problem posting log info : " + logMsg + "(" + e.getMessage() + ")");
	}
}

}
