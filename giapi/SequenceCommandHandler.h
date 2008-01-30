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
	virtual pHandlerResponse handle(command::ActionId id, 
			pRequest request,
			pConfiguration config) = 0;

protected:
	/**
	 * Protected constructor. This is a pure abstract class that can not 
	 * be instantiated
	 */
	SequenceCommandHandler();

};

typedef std::tr1::shared_ptr<SequenceCommandHandler> pSequenceCommandHandler;

}

#endif /*SEQUENCECOMMANDHANDLER_H_*/
