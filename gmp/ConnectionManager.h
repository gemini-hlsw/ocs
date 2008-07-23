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
	
	Session* createSession() throw ( CMSException ) ;

	/**
	 * Handles the exceptions that might happen with the connection
	 * to the broker
	 */
	virtual void onException( const CMSException& ex AMQCPP_UNUSED);

	virtual ~ConnectionManager();

private:
	ConnectionManager();
	static std::auto_ptr<ConnectionManager> INSTANCE;

	/**
	 * The JMS Connection to the broker
	 */
	std::tr1::shared_ptr<Connection> _connection;
	
	
};

}

#endif /*CONNECTIONMANAGER_H_*/
