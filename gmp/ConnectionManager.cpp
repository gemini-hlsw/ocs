#include "ConnectionManager.h"

#include <activemq/core/ActiveMQConnectionFactory.h>

namespace gmp {
log4cxx::LoggerPtr ConnectionManager::logger(log4cxx::Logger::getLogger("giapi.gmp.ConnectionManager"));

std::auto_ptr<ConnectionManager>
		ConnectionManager::INSTANCE(new ConnectionManager());

ConnectionManager::ConnectionManager() {

}

ConnectionManager::~ConnectionManager() {

	LOG4CXX_DEBUG(logger, "Destroying connection manager");
	//TODO: We need to close sessions, destinations, producers and consumers.
	try {
		if (_connection.get() != 0) {
			_connection->close();
		}
	} catch (CMSException& e) {
		LOG4CXX_WARN(logger, "Problem closing connection");
		e.printStackTrace();
	}

}

void ConnectionManager::startup()  {

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
		_connection = pConnection(connectionFactory->createConnection());

		delete connectionFactory;
		connectionFactory = NULL;

		_connection->start();

		_connection->setExceptionListener(this);

	} catch (CMSException& e) {
		if (connectionFactory != NULL) {
			delete connectionFactory;
			connectionFactory = NULL;
		}
		//TODO: Sleep for a while and try to reconnect, maybe?.
		//For now, just print a stack trace and exit the program
		e.printStackTrace();
		exit(1);
	}
}

ConnectionManager& ConnectionManager::Instance() {
	//if not connected, try to reconnect:
	if (INSTANCE->_connection.get() == 0) {
		INSTANCE->startup();
	}
	return *INSTANCE;
}

void ConnectionManager::onException(const CMSException & ex) {
	LOG4CXX_ERROR(logger, "Communication Exception occured ");
	ex.printStackTrace();
	//TODO: Sleep for a while and try to reconnect. For now just exit
	exit(1);
}

pSession ConnectionManager::createSession() throw (CMSException ) {
	
	pSession session(static_cast<Session*>(0));
	try {
		session.reset(_connection->createSession(Session::AUTO_ACKNOWLEDGE));
	} catch (CMSException &e) {
		throw; //just retrow the exception
	}
	return session;
}

}
