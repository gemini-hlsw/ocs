#include "JmsStatusSender.h"
#include <giapi/giapi.h>

#include <gmp/ConnectionManager.h>
#include <gmp/GMPKeys.h>

using namespace gmp;

namespace giapi {

log4cxx::LoggerPtr JmsStatusSender::logger(log4cxx::Logger::getLogger(
		"giapi.JmsStatusSender"));

JmsStatusSender::JmsStatusSender() throw (CommunicationException) {
	LOG4CXX_DEBUG(logger, "Constructing JMS Status sender")
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
	LOG4CXX_DEBUG(logger, "Destroying JMS Status sender")
	cleanup();
}

int JmsStatusSender::postStatus(StatusItem * statusItem) const
		throw (PostException) {
	LOG4CXX_DEBUG(logger, "Post Status Item " << *statusItem);

	BytesMessage *msg = NULL;

	try {
		msg = _session->createBytesMessage();

		Offsets offset = BASIC_OFFSET; // this is to differentiate different types of
		                // status when serializing them into JMS
		const std::type_info& typeInfo = statusItem->getType();

		AlarmStatusItem* alarm = dynamic_cast<AlarmStatusItem* >(statusItem);

		HealthStatusItem *health = NULL;

		if (alarm != NULL) { // the object is an alarm status item
			offset = ALARM_OFFSET;
		} else {
			//it could be a Health Status item
			health = dynamic_cast<HealthStatusItem*>(statusItem);
			if (health != NULL) {
				offset = HEALTH_OFFSET;
			}
		}


		if (typeInfo == typeid(int)) {
			msg->writeByte(offset);
			//the name now...
			msg->writeUTF(statusItem->getName());
			//and finally the value
			msg->writeInt(statusItem->getValueAsInt());
		} else if (typeInfo == typeid(double) ) {
			msg->writeByte(offset + 1);
			//the name now...
			msg->writeUTF(statusItem->getName());
			//and finally the value
			msg->writeDouble(statusItem->getValueAsDouble());
		} else if (typeInfo == typeid(const char *)) {
			msg->writeByte(offset + 3);
			//the name now...
			msg->writeUTF(statusItem->getName());
			//and finally the value
			msg->writeUTF(statusItem->getValueAsString());
		}


		//Alarm information to go here now
		if (offset == ALARM_OFFSET) {
			alarm::Cause cause = alarm->getCause();
			alarm::Severity severity = alarm->getSeverity();

			switch (severity) {
			case alarm::ALARM_OK:
				msg->writeByte(0);
				break;
			case alarm::ALARM_WARNING:
				msg->writeByte(1);
				break;
			case alarm::ALARM_FAILURE:
				msg->writeByte(2);
				break;
			}

			switch (cause) {
			case alarm::ALARM_CAUSE_OK:
				msg->writeByte(0);
				break;
			case alarm::ALARM_CAUSE_HIHI:
				msg->writeByte(1);
				break;
			case alarm::ALARM_CAUSE_HI:
				msg->writeByte(2);
				break;
			case alarm::ALARM_CAUSE_LOLO:
				msg->writeByte(3);
				break;
			case alarm::ALARM_CAUSE_LO:
				msg->writeByte(4);
				break;
			case alarm::ALARM_CAUSE_OTHER:
				msg->writeByte(5);
				break;
			}

			//check if there is any message for this alarm
			if (alarm->getMessage() != NULL) {
				msg->writeBoolean(true);
				msg->writeUTF(alarm->getMessage());
			} else {
				msg->writeBoolean(false);
			}

		}

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
