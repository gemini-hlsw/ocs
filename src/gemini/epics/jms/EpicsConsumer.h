#ifndef EPICSCONSUMER_H_
#define EPICSCONSUMER_H_

#include <cms/Message.h>
#include <cms/MessageListener.h>
#include <cms/Session.h>

#include <log4cxx/logger.h>

#include <tr1/memory>

#include <giapi/EpicsStatusHandler.h>
#include <giapi/giapiexcept.h>
#include <util/JmsSmartPointers.h>

#include <gmp/ConnectionManager.h>

using namespace cms;
using namespace gmp;

namespace giapi {

/**
 * Forward declaration of the EpicsConsumer class
 */
class EpicsConsumer;
/**
 * Definition of a smart pointer to an epics consumer
 */
typedef std::tr1::shared_ptr<EpicsConsumer> pEpicsConsumer;


/**
 * This class provides a JMS listener that will receive messages with
 * updated information about one EPICS channel. The updated information
 * will be passed to the registered EPICS status handler. This way,
 * client code (via the EPICS status handler) can receive notifications
 * associated to an EPICS status item update of interest.
 */
class EpicsConsumer: public MessageListener {

public:
	virtual ~EpicsConsumer() throw ();

	/**
	 * Invoked by the JMS whenever a new message is received
	 */
	virtual void onMessage(const Message* message) throw();

	/**
	 * Static factory to create consumers for different EPICS channels
	 * associated to the corresponding EPICS status handler.
	 */
	static pEpicsConsumer create(const std::string &channelName,
			pEpicsStatusHandler handler) throw (CommunicationException);

private:

	/**
	 * Constructor. Start monitoring (via JMS) the specified channel and invokes
	 * the given handler when an update is received.
	 */
	EpicsConsumer(const std::string &channelName, pEpicsStatusHandler handler)
			throw (CommunicationException);

	/**
	 * Close and destroy associated JMS resources used by this consumer
	 */

	void cleanup();
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

	/**
	 * The JMS Session associated to this consumer. Each consumer has its
	 * own session, since they might operate in different execution
	 * threads
	 */
	pSession _session;

	/**
	 * The virtual channel from where this consumer will get the messages.
	 * The destination is obtained based on the epics channel we have
	 * interest on
	 */
	pDestination _destination;

	/**
	 * The consumer instance for this object. Each consumer runs on its
	 * own JMS Session
	 */
	pMessageConsumer _consumer;

	/**
	 * The handler to be invoked whenever an EPICS update is received
	 */
	pEpicsStatusHandler _handler;
	
	/**
	 * The Connection Manager
	 */
	pConnectionManager _connectionManager;

	/**
	 * The EPICS channel name this consumer is monitoring.
	 */
	std::string _channelName;

};

}

#endif /* EPICSCONSUMER_H_ */
