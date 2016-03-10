#include "ConnectionManager.h"
#include <src/util/PropertiesUtil.h>
#include <src/util/StringUtil.h>

#include <activemq/core/ActiveMQConnectionFactory.h>
#include <decaf/util/concurrent/CountDownLatch.h>
#include <activemq/library/ActiveMQCPP.h>
#include <decaf/io/IOException.h>

namespace gmp {
log4cxx::LoggerPtr ConnectionManager::logger(log4cxx::Logger::getLogger("giapi.gmp.ConnectionManager"));

//Time to wait (in seconds) to attempt a new connection in case the connection is lost
int ConnectionManager::RETRY_TIMEOUT = 10;

pConnectionManager ConnectionManager::INSTANCE(new ConnectionManager());

ConnectionManager::ConnectionManager() {
	//Initialize the ActiveMQ Library before it is used
	activemq::library::ActiveMQCPP::initializeLibrary();
}

ConnectionManager::~ConnectionManager() {

	LOG4CXX_DEBUG(logger, "Destroying connection manager");
	try {
		if (_connection.get() != 0) {
			_connection->stop();
		}
	}catch (CMSException &e) {
		LOG4CXX_WARN(logger, "Problem closing JMS Connection");
	}
	//release all the references to the objects stored
	_errorHandlersFunctions.clear();
	_errorHandlerObjects.clear();
	//TODO: shut down the ActiveMQ library (with CMS3.1.1 this
	//call is throwing an IOException
	//activemq::library::ActiveMQCPP::shutdownLibrary();
	LOG4CXX_DEBUG(logger, "Connection manager destroyed");

}

void ConnectionManager::registerOperation(giapi_error_handler op) {
	_errorHandlersFunctions.insert(op);
}

void ConnectionManager::registerHandler(pGiapiErrorHandler handler) {
	_errorHandlerObjects.insert(handler);
}


void ConnectionManager::startup() throw (GmpException) {

    std::string hostname = giapi::util::PropertiesUtil::Instance().getProperty("gmp.hostname");
    if(giapi::util::StringUtil::isEmpty(hostname)){
        hostname = std::string("localhost");
    }
    std::string brokerURI =
        "failover:(tcp://"+hostname+":61616"
        "?wireFormat=openwire"
//        "&transport.useInactivityMonitor=false"
//        "&connection.alwaysSyncSend=true"
        "&connection.useAsyncSend=true"
//        "&transport.commandTracingEnabled=true"
//        "&transport.tcpTracingEnabled=true"
//        "&wireFormat.tightEncodingEnabled=true"
        ")";

	try {
		std::auto_ptr<ConnectionFactory> connectionFactory(
				ConnectionFactory::createCMSConnectionFactory( brokerURI ));

		// Create a Connection
		_connection.reset(connectionFactory->createConnection());

		_connection->start();

		_connection->setExceptionListener(this);

	} catch (CMSException& e) {
		throw GmpException("Problem connecting to GMP. " + e.getMessage());
	}
}

pConnectionManager ConnectionManager::Instance() throw (GmpException) {
	//if not connected, try to reconnect:
	if (INSTANCE->_connection.get() == 0) {
		INSTANCE->startup();
	}
	return INSTANCE;
}

void ConnectionManager::onException(const CMSException & ex) {
	LOG4CXX_ERROR(logger, "Communication Exception occurred ");
	ex.printStackTrace();
	if (_connection.get() != 0) {
		_connection->stop();
	}
	//reset the connection, so next time it will try to reconnect...
	_connection.reset(static_cast<Connection *>(0));
	//start a reconnection loop...
	decaf::util::concurrent::CountDownLatch lock(1);
	LOG4CXX_INFO(logger, "Waiting " << RETRY_TIMEOUT << " to attempt reconnection...");
	lock.await(RETRY_TIMEOUT * 1000);

	LOG4CXX_INFO(logger, "Attempting reconnection...");

	bool connected = false;
	while (!connected) {
		try {
			startup();
			connected = true;
		} catch (GmpException &e) {
			LOG4CXX_INFO(logger, "Problem attempting reconnection.. " << e.getMessage());
			LOG4CXX_INFO(logger, "Waiting " << RETRY_TIMEOUT << " seconds before attempting reconnection");
			lock.await(RETRY_TIMEOUT * 1000);
		}
	}

	LOG4CXX_INFO(logger, "Connection recovered. Invoking user provided error handlers");

	//functions first...
	std::set<giapi_error_handler>::const_iterator it = _errorHandlersFunctions.begin();

	while (it != _errorHandlersFunctions.end()) {
		//invoke this handler
		(*it)();
		it++;
	}

	//now the objects...
	std::set<pGiapiErrorHandler>::const_iterator itObject =
			_errorHandlerObjects.begin();

	while (itObject != _errorHandlerObjects.end()) {
		//invoke this handler
		pGiapiErrorHandler handler = *itObject;
		handler->onError();
		itObject++;
	}

}

pSession ConnectionManager::createSession() throw (CMSException ) {

	pSession session(_connection->createSession(Session::AUTO_ACKNOWLEDGE));
	return session;
}

}
