#include "ConnectionManager.h"

#include <activemq/core/ActiveMQConnectionFactory.h>

namespace gmp {
log4cxx::LoggerPtr ConnectionManager::logger(log4cxx::Logger::getLogger("giapi.gmp.ConnectionManager"));

std::auto_ptr<ConnectionManager>
		ConnectionManager::INSTANCE(new ConnectionManager());

ConnectionManager::ConnectionManager() {

	ConnectionFactory* connectionFactory= NULL;

	std::string brokerURI = "tcp://127.0.0.1:61616"
		"?wireFormat=openwire"
		"&transport.useAsyncSend=true";
	//        "&transport.commandTracingEnabled=true"
	//        "&transport.tcpTracingEnabled=true";
	//        "&wireFormat.tightEncodingEnabled=true";

	try {
		// Create a ConnectionFactory
		connectionFactory =
		ConnectionFactory::createCMSConnectionFactory( brokerURI );

		// Create a Connection
		_connection = std::tr1::shared_ptr<Connection>(connectionFactory->createConnection());

		delete connectionFactory;
		connectionFactory = NULL;

		_connection->start();

		_connection->setExceptionListener(this);

	} catch (CMSException& e) {
		if (connectionFactory != NULL) {
			delete connectionFactory;
			connectionFactory = NULL;
		}

		//TODO: Sleep for a while and try to reconnect. 
		e.printStackTrace();
	}
}

ConnectionManager::~ConnectionManager() {

	LOG4CXX_DEBUG(logger, "Destroying connection manager");

	//TODO: We need to close sessions, destinations, producers and consumers.

	try {
		_connection->close();
	} catch (CMSException& e) {
		LOG4CXX_WARN(logger, "Problem closing connection");
		e.printStackTrace();
	}

}

ConnectionManager& ConnectionManager::Instance() {
	return *INSTANCE;
}

void ConnectionManager::onException(const CMSException & ex AMQCPP_UNUSED) {
	LOG4CXX_ERROR(logger, "CMS Exception occured ");
	ex.printStackTrace();
	//TODO: Sleep for a while and try to reconnect
	
}

Session* ConnectionManager::createSession() throw (CMSException ) {
	return _connection->createSession(Session::AUTO_ACKNOWLEDGE);
}

}
