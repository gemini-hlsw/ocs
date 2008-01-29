#ifndef SEQUENCECOMMANDHANDLER_H_
#define SEQUENCECOMMANDHANDLER_H_

#include <giapi/giapi.h>
#include <giapi/HandlerResponse.h>
#include <giapi/Request.h>
#include <giapi/Configuration.h>

#include <tr1/memory>


namespace giapi {
/**
 * Interface for sequence command handlers
 */
class SequenceCommandHandler {
public:
	virtual ~SequenceCommandHandler();

	/**
	 * 
	 */
	virtual std::tr1::shared_ptr<HandlerResponse> handle(command::ActionId id, 
			std::tr1::shared_ptr<Request> request,
			std::tr1::shared_ptr<Configuration> config) = 0;

protected:
	/**
	 * Protected constructor. This is a pure abstract class that can not 
	 * be instantiated
	 */
	SequenceCommandHandler();

};

}

#endif /*SEQUENCECOMMANDHANDLER_H_*/
