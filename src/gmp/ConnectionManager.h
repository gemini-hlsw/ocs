#ifndef CONNECTIONMANAGER_H_
#define CONNECTIONMANAGER_H_

#include <log4cxx/logger.h>
#include <tr1/memory>
#include <set>

#include <cms/Connection.h>
#include <cms/Session.h>
#include <cms/ExceptionListener.h>
#include <activemq/util/Config.h>

#include <gmp/JmsUtil.h>
#include <giapi/giapiexcept.h>
#include <giapi/giapi.h>
#include <giapi/GiapiErrorHandler.h>

#include <util/JmsSmartPointers.h>

namespace gmp {


using namespace cms;

class ConnectionManager; 

typedef std::tr1::shared_ptr<ConnectionManager> pConnectionManager; 

/**
 * Connection Manager is a singleton that provides a unique communication
 * point to the broker.
 *
 * Using the connection manager, clients can start JMS Sessions to
 * exchange messages with the GMP.
 *
 * The connection manager knows how to find the GMP and how to establish a
 * connection to it. Details of the internal communication to the Gemini
 * Master Process are hidden from the client code.
 */

class ConnectionManager : public ExceptionListener {

	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:

	/**
	 * Get the unique instance of the Connection Manager
	 *
	 * @return The ConnectionManager singleton object
	 */
	static pConnectionManager Instance() throw (GmpException);

	/**
	 * Creates a new JMS Session for clients to interact with
	 * the GMP broker. It does not keep ownership of the newly
	 * allocated object. It is responsibility of the callers to
	 * release and destroy the returned object
	 *
	 * @return A new Session object from the current connection.
	 */
	pSession createSession() throw (CMSException );

	/**
	 * Handles the exceptions that might happen with the connection
	 * to the broker
	 */
	virtual void onException(const CMSException& ex AMQCPP_UNUSED);

	/**
	 * Destructor. Close and delete the connection to the broker
	 */
	virtual ~ConnectionManager();


	void registerOperation(giapi_error_handler  op);

	void registerHandler(pGiapiErrorHandler handler);

private:
	ConnectionManager();
	/**
	 * The singleton instance to the connection manager
	 */
	static pConnectionManager INSTANCE;

	/**
	 * The JMS Connection to the broker
	 */
	pConnection _connection;

	std::set<giapi_error_handler> _errorHandlersFunctions;

	std::set<pGiapiErrorHandler> _errorHandlerObjects;

	/**
	 * Initialize the communication to the broker
	 */
	void startup() throw (GmpException);

	/**
	 * Timeout to use to retry a connection to the GMP in
	 * case the connection fails.
	 */
	static int RETRY_TIMEOUT;
};

}

#endif /*CONNECTIONMANAGER_H_*/
