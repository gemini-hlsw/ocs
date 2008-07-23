#ifndef JMSMESSAGECONSUMER_H_
#define JMSMESSAGECONSUMER_H_

#include <activemq/util/Config.h>
#include <decaf/util/concurrent/CountDownLatch.h>

#include <cms/MessageListener.h>
#include <cms/Session.h>

#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/HandlerResponse.h>

#include <log4cxx/logger.h>

using namespace giapi;

namespace gmp {

using namespace cms;
using namespace decaf::util::concurrent;

class SequenceCommandConsumer : public MessageListener {

private:
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

	CountDownLatch latch;

	Session* _session;
	Destination* _destination;
	MessageConsumer* _consumer;

	pSequenceCommandHandler _handler;

	command::SequenceCommand _sequenceCommand;

	Message * _buildReply(pHandlerResponse response);
	
	void cleanup();

public:
	SequenceCommandConsumer(command::SequenceCommand id,
			command::ActivitySet activities, pSequenceCommandHandler handler);
	virtual ~SequenceCommandConsumer();
	// Called when a message associated to the specific topic is received
	virtual void onMessage(const Message* message);

};

}

#endif /*MESSAGECONSUMER_H_*/
