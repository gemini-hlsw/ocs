#ifndef COMPLETIONINFOPRODUCER_H_
#define COMPLETIONINFOPRODUCER_H_

#include <giapi/giapi.h>
#include <giapi/HandlerResponse.h>

#include <cms/Session.h>
#include <cms/Destination.h>
#include <cms/MessageProducer.h>

#include <log4cxx/logger.h>
#include <tr1/memory>

using namespace giapi;
using namespace cms;

namespace gmp {

class CompletionInfoProducer;

typedef std::auto_ptr<CompletionInfoProducer> pCompletionInfoProducer;

/**
 * Produces completion information messages back to the GMP.
 */

class CompletionInfoProducer {

public:
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

	virtual ~CompletionInfoProducer();

	/**
	 * Send the completion information contained in the response back to the 
	 * GMP.
	 * 
	 * @param id the original ActionId associated to the actions for which 
	 * completion info is being reported 
	 * @param response contains the completion state associated to the action
	 * id. 
	 * @return giapi::status::OK if the post suceeds. Otherwise, it
	 *         returns giapi::status::ERROR. 
	 */
	int postCompletionInfo(command::ActionId id, pHandlerResponse response);

	static pCompletionInfoProducer create();

private:

	/** 
	 * The JMS Session associated to this producer. 
	 */
	Session* _session;

	/**
	 * The virtual channel to where this producer will send messages to
	 */
	Destination* _destination;

	/**
	 * The message producer in charge of sending completion information
	 * messages. Runs on its own session
	 */
	MessageProducer* _producer;

	/**
	 * Destroy any allocated resources and closes communication channels
	 */
	void cleanup();

	/**
	 * Private Constructor
	 */
	CompletionInfoProducer();

};

}

#endif /*COMPLETIONINFOPRODUCER_H_*/
