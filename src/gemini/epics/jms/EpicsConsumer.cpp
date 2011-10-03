#include "EpicsConsumer.h"
#include <gmp/ConnectionManager.h>

#include <cms/BytesMessage.h>

#include <gemini/epics/jms/JmsEpicsFactory.h>


namespace giapi {

log4cxx::LoggerPtr EpicsConsumer::logger(log4cxx::Logger::getLogger(
		"giapi::gemini::EpicsConsumer"));

EpicsConsumer::EpicsConsumer(const std::string &channelName,
		pEpicsStatusHandler handler) throw (CommunicationException) {
	_handler = handler;
	_channelName = channelName;
	try {

		_connectionManager = ConnectionManager::Instance();

		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

		std::string topic = JmsUtil::getEpicsChannelTopic(channelName);

		// Create the Topic destination
		_destination = pDestination(_session->createTopic(topic));

		LOG4CXX_DEBUG(logger, "Start receiving EPICS updates through JMS topic " << topic);
		// Create a MessageConsumer from the Session to the Topic or Queue
		_consumer = pMessageConsumer(_session->createConsumer(
				_destination.get()));

		_consumer->setMessageListener(this);
	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing EPICS consumer: "
				+ e.getMessage());
	}
}

pEpicsConsumer EpicsConsumer::create(const std::string &channelName,
		pEpicsStatusHandler handler) throw (CommunicationException) {

	pEpicsConsumer consumer(new EpicsConsumer(channelName, handler));
	return consumer;

}

EpicsConsumer::~EpicsConsumer() throw() {
	LOG4CXX_DEBUG(logger, "Destroying EPICS Consumer for channel " << _channelName);
	cleanup();
}

void EpicsConsumer::cleanup() {
	//*************************************************
	// Always close destination, consumers and producers before
	// you destroy their sessions and connection.
	//*************************************************

	// Close open resources.
	try {
		if (_consumer.get() != 0)
			_consumer->close();
	} catch (CMSException& e) {
		e.printStackTrace();
	}

	try {
		if (_session.get() != 0)
			_session->close();
	} catch (CMSException& e) {
		e.printStackTrace();
	}

	//destruction of the objects is automatic since we are using smart pointers
}

void EpicsConsumer::onMessage(const cms::Message * message) throw() {

	const BytesMessage* bytesMessage =
			dynamic_cast< const BytesMessage* >( message );

	if (bytesMessage != NULL) {
		pEpicsStatusItem item = JmsEpicsFactory::buildEpicsStatusItem(bytesMessage);
		_handler->channelChanged(item);
	}
}

}
