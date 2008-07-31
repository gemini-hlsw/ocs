#ifndef CONNECTIONMANAGER_H_
#define CONNECTIONMANAGER_H_

#include <log4cxx/logger.h>
#include <tr1/memory>

#include <cms/Connection.h>
#include <cms/Session.h>
#include <cms/ExceptionListener.h>
#include <activemq/util/Config.h>

namespace gmp {

using namespace cms;

/**
 * Connection Manager is a singleton that provides a unique communication 
 * point to the broker.
 * 
 * Using the connection manager, clients can start JMS Sessions to 
 * exchange messages with the GMP. 
 * 
 * The connection manager knows how to find the GMP and how to establish a 
 * connection to it. In addition, it handles communication exceptions 
 * to the GMP server, taking measures in case of problems.
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
	static ConnectionManager & Instance();

	/**
	 * Creates a new JMS Session for clients to interact with
	 * the GMP broker. It does not keep ownership of the newly 
	 * allocated object. It is responsibility of the callers to
	 * release and destroy the returned object
	 * 
	 * @return A new Session object from the current connection. 
	 */
	Session* createSession() throw (CMSException );

	/**
	 * Handles the exceptions that might happen with the connection
	 * to the broker
	 */
	virtual void onException(const CMSException& ex AMQCPP_UNUSED);
	
	/**
	 * Destructor. Close and delete the connection to the broker
	 */
	virtual ~ConnectionManager();

private:
	ConnectionManager();
	/**
	 * The singleton instance to the connection manager
	 */
	static std::auto_ptr<ConnectionManager> INSTANCE;

	/**
	 * The JMS Connection to the broker
	 */
	std::tr1::shared_ptr<Connection> _connection;

	/**
	 * Initialize the communication to the broker
	 */
	void startup();
};

}

#endif /*CONNECTIONMANAGER_H_*/
