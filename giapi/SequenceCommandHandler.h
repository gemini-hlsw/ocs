#ifndef SEQUENCECOMMANDHANDLER_H_
#define SEQUENCECOMMANDHANDLER_H_

#include <giapi/giapi.h>
#include <giapi/HandlerResponse.h>
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
			command::SequenceCommand sequenceCommand,
			command::Activity activity,
			pConfiguration config) = 0;

protected:
	/**
	 * Protected constructor. This is an abstract class that can not 
	 * be instantiated
	 */
	SequenceCommandHandler();

};

typedef std::tr1::shared_ptr<SequenceCommandHandler> pSequenceCommandHandler;

}

#endif /*SEQUENCECOMMANDHANDLER_H_*/
