#include "SequenceCommandConsumer.h"

#include <activemq/core/ActiveMQConnectionFactory.h>
#include <cms/MapMessage.h>

#include <giapi/Configuration.h>
#include <ConfigurationFactory.h>

#include "ConnectionManager.h"

#include "JmsUtil.h"
#include "GMPKeys.h"

using namespace gmp;

using namespace decaf::lang;

namespace gmp {
log4cxx::LoggerPtr SequenceCommandConsumer::logger(log4cxx::Logger::getLogger("jms.MessageConsumer"));

SequenceCommandConsumer::SequenceCommandConsumer(command::SequenceCommand id,
		command::ActivitySet activities, pSequenceCommandHandler handler) :
	latch(1) {

	_handler = handler;

	try {

		ConnectionManager& manager = ConnectionManager::Instance();

		//create an auto-acknowledged session
		_session = manager.createSession();

		//Store the sequence command this processor is associated with
		_sequenceCommand = id;

		// Create the Topic destination 
		_destination = _session->createTopic( JmsUtil::getTopic(id) );

		LOG4CXX_DEBUG(logger, "Starting consumer for topic " << JmsUtil::getTopic(id));
		// Create a MessageConsumer from the Session to the Topic or Queue
		_consumer = _session->createConsumer( _destination );

		_consumer->setMessageListener( this );
		std::cout.flush();
		std::cerr.flush();

		// Indicate we are ready to consume sequence commands
		latch.countDown();

	} catch (CMSException& e) {

		// In case of exception, waiting clients should be unlocked
		latch.countDown();
		//clean anyresources that might have been allocated
		cleanup();
		e.printStackTrace();
	}
}

SequenceCommandConsumer::~SequenceCommandConsumer() {
	LOG4CXX_DEBUG(logger, "Destroying Sequence Command Consumer for " << JmsUtil::getTopic(_sequenceCommand));
	cleanup();
}

pSequenceCommandConsumer SequenceCommandConsumer::create(
		command::SequenceCommand id, command::ActivitySet activities,
		pSequenceCommandHandler handler) {

	pSequenceCommandConsumer consumer(new SequenceCommandConsumer(id,
			activities, handler));
	return consumer;
}

void SequenceCommandConsumer::onMessage(const Message* message) {

	try {
		const MapMessage* mapMessage =
		dynamic_cast< const MapMessage* >( message );

		std::vector< std::string > names = mapMessage->getMapNames();

		//get the Action Id
		int actionId = mapMessage->getIntProperty(GMPKeys::GMP_ACTIONID_PROP);
		//get the activity Id;
		command::Activity activity = JmsUtil::getActivity(mapMessage->getStringProperty(GMPKeys::GMP_ACTIVITY_PROP));

		LOG4CXX_DEBUG(logger, "Received Sequence command: " << JmsUtil::getTopic(_sequenceCommand) << " Activity : " << mapMessage->getStringProperty(GMPKeys::GMP_ACTIVITY_PROP) );

		//build a configuration object
		pConfiguration config = ConfigurationFactory::getConfiguration();

		for (std::vector< std:: string>::iterator i = names.begin(); i != names.end(); i++) {
			config->setValue((*i).c_str(), (mapMessage->getString(*i)).c_str());
		}

		pHandlerResponse response = _handler->handle(actionId, _sequenceCommand, activity, config);

		LOG4CXX_DEBUG(logger, "Replying to sequence command: " << JmsUtil::getHandlerResponse(response));

		const Destination* destination = message->getCMSReplyTo();

		if (destination == NULL) {
			LOG4CXX_ERROR(logger, "Invalid destination received. Can't reply to request");
			return;
		}

		MessageProducer * producer = _session->createProducer(destination);

		Message* reply = _buildReply(response);

		producer->send(reply);

		//delete allocated objects
		delete reply;

		//TODO: If I destroy this destination, the program exits.... :/
		//Probably is destroyed as part of destroying the message, handled directly by the JMS provider. 
		//Confirm!
		//delete destination;

		//Destroy the producer used to reply 
		producer->close();
		delete producer;

	} catch (CMSException& e) {
		e.printStackTrace();
	}
}

Message * SequenceCommandConsumer::_buildReply(pHandlerResponse response) {

	MapMessage *msg = _session->createMapMessage();
	msg->setString(GMPKeys::GMP_HANDLER_RESPONSE_KEY,
			JmsUtil::getHandlerResponse(response));

	//If this is an error, then put the error message in the response 
	if (response->getResponse() == HandlerResponse::ERROR) {
		if (response->getMessage() != NULL) {
			msg->setString(GMPKeys::GMP_HANDLER_RESPONSE_ERROR_KEY,
					response->getMessage());
		}
	}
	return msg;
}

void SequenceCommandConsumer::cleanup() {
	//*************************************************
	// Always close destination, consumers and producers before
	// you destroy their sessions and connection.
	//*************************************************

	// Destroy resources.
	try {
		if( _destination != NULL ) delete _destination;
	} catch (CMSException& e) {e.printStackTrace();}
	_destination = NULL;

	try {
		if( _consumer != NULL ) delete _consumer;
	} catch (CMSException& e) {e.printStackTrace();}
	_consumer = NULL;

	// Close open resources.
	try {
		if( _session != NULL ) _session->close();
	} catch (CMSException& e) {e.printStackTrace();}

	// Now Destroy them
	try {
		if( _session != NULL ) delete _session;
	} catch (CMSException& e) {e.printStackTrace();}
	_session = NULL;
}

}
