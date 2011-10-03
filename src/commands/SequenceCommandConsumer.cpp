#include "SequenceCommandConsumer.h"

#include <activemq/core/ActiveMQConnectionFactory.h>
#include <cms/MapMessage.h>

#include <giapi/Configuration.h>
#include <gmp/ConnectionManager.h>
#include <gmp/JmsUtil.h>
#include <gmp/GMPKeys.h>


#include "ConfigurationFactory.h"



using namespace decaf::lang;


namespace giapi {
log4cxx::LoggerPtr SequenceCommandConsumer::logger(log4cxx::Logger::getLogger("giapi::SequenceCommandConsumer"));

SequenceCommandConsumer::SequenceCommandConsumer(command::SequenceCommand id,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {
	_sequenceCommand = id;
	init( JmsUtil::getTopic(id), activities, handler );

}

SequenceCommandConsumer::SequenceCommandConsumer(const std::string & prefix,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {
    _sequenceCommand = giapi::command::APPLY;
	init( JmsUtil::getTopic(prefix), activities, handler );

}

void SequenceCommandConsumer::init(
		const std::string & topic,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {

	_handler = handler;

	try {

		_connectionManager = ConnectionManager::Instance();

		//create an auto-acknowledged session
		_session = _connectionManager->createSession();

		// Create the Topic destination
		_destination = pDestination(_session->createTopic( topic ));

		const std::string selector = buildSelector(activities);

		LOG4CXX_DEBUG(logger, "Starting consumer for topic " << topic << "/" <<  selector);
		// Create a MessageConsumer from the Session to the Topic or Queue
		_consumer = pMessageConsumer(_session->createConsumer( _destination.get(), selector ));

		_consumer->setMessageListener( this );
	} catch (CMSException& e) {
		//clean any resources that might have been allocated
		cleanup();
		throw CommunicationException("Trouble initializing sequence command producer: " + e.getMessage());
	}


}


SequenceCommandConsumer::~SequenceCommandConsumer() throw (){
	LOG4CXX_DEBUG(logger, "Destroying Sequence Command Consumer " << _sequenceCommand);
	cleanup();
}

pSequenceCommandConsumer SequenceCommandConsumer::create(
		command::SequenceCommand id, command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {

	pSequenceCommandConsumer consumer(new SequenceCommandConsumer(id,
			activities, handler));
	return consumer;
}


pSequenceCommandConsumer SequenceCommandConsumer::create(
		const std::string &prefix, command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {

	pSequenceCommandConsumer consumer(new SequenceCommandConsumer(prefix,
			activities, handler));
	return consumer;
}




void SequenceCommandConsumer::onMessage(const Message* message) throw (){

	try {
		const MapMessage* mapMessage =
		dynamic_cast< const MapMessage* >( message );

		std::vector< std::string > names = mapMessage->getMapNames();

		//get the Action Id
		int actionId = mapMessage->getIntProperty(GMPKeys::GMP_ACTIONID_PROP);
		//get the activity Id;
		command::Activity activity = JmsUtil::getActivity(mapMessage->getStringProperty(GMPKeys::GMP_ACTIVITY_PROP));

		LOG4CXX_DEBUG(logger, "Received Sequence command (" << actionId << "): " << JmsUtil::getTopic(_sequenceCommand) << " Activity : " << mapMessage->getStringProperty(GMPKeys::GMP_ACTIVITY_PROP) );

		//build a configuration object
		pConfiguration config = ConfigurationFactory::getConfiguration();

		for (std::vector< std:: string>::iterator i = names.begin(); i != names.end(); i++) {
			config->setValue((*i), (mapMessage->getString(*i)));
		}

		pHandlerResponse response = _handler->handle(actionId, _sequenceCommand, activity, config);

		LOG4CXX_DEBUG(logger, "Replying to sequence command:(" << actionId << "): " << JmsUtil::getHandlerResponse(response));

		const Destination* destination = message->getCMSReplyTo();

		if (destination == NULL) {
			LOG4CXX_ERROR(logger, "Invalid destination received. Can't reply to request");
			return;
		}

		pMessageProducer producer = pMessageProducer(_session->createProducer(destination));

		MapMessage *reply = _session->createMapMessage();

		JmsUtil::makeHandlerResponseMsg(reply, response);

		producer->send(reply);

		//delete allocated objects
		delete reply;

		//TODO: If I destroy this destination, the program exits.... :/
		//Probably is destroyed as part of destroying the message, handled directly by the JMS provider.
		//Confirm!
		//delete destination;
		//Close the producer used to reply
		producer->close();

	} catch (CMSException& e) {
		e.printStackTrace();
	}
}


std::string SequenceCommandConsumer::buildSelector(command::ActivitySet activities) {
	std::string selector = GMPKeys::GMP_ACTIVITY_PROP + " IN ('";

	switch (activities) {
		case command::SET_PRESET:
			 selector += GMPKeys::GMP_ACTIVITY_PRESET + "'";
			 break;
		case command::SET_PRESET_START:
			selector += GMPKeys::GMP_ACTIVITY_PRESET + "','"
					+ GMPKeys::GMP_ACTIVITY_START + "','"
					+ GMPKeys::GMP_ACTIVITY_PRESET_START + "'";
			break;
		case command::SET_CANCEL:
			selector += GMPKeys::GMP_ACTIVITY_CANCEL + "'";
			break;
		case command::SET_PRESET_CANCEL:
			selector += GMPKeys::GMP_ACTIVITY_PRESET + "','"
					+ GMPKeys::GMP_ACTIVITY_CANCEL +"'";
			break;
		case command::SET_START_CANCEL:
			selector += GMPKeys::GMP_ACTIVITY_START + "','"
					+ GMPKeys::GMP_ACTIVITY_CANCEL +"'";
			break;
		case command::SET_PRESET_START_CANCEL:
			selector += GMPKeys::GMP_ACTIVITY_PRESET + "','"
					+ GMPKeys::GMP_ACTIVITY_START + "','"
					+ GMPKeys::GMP_ACTIVITY_PRESET_START + "','"
					+ GMPKeys::GMP_ACTIVITY_CANCEL + "'";
			break;
		default:
			break;
	}

	selector += ")";
	return selector;
}

void SequenceCommandConsumer::cleanup() {
	//*************************************************
	// Always close destination, consumers and producers before
	// you destroy their sessions and connection.
	//*************************************************

	// Close open resources.
	try {
		if( _consumer.get() != 0 ) _consumer->close();
	} catch (CMSException& e) {e.printStackTrace();}

	try {
		if( _session.get() != 0 ) _session->close();
	} catch (CMSException& e) {e.printStackTrace();}

	//destruction of the objects is automatic since we are using smart pointers
}

}
