#ifndef SEQUENCECOMMANDHANDLER_H_
#define SEQUENCECOMMANDHANDLER_H_

#include <giapi/giapi.h>
#include <giapi/HandlerResponse.h>
#include <giapi/Configuration.h>

#include <tr1/memory>

namespace giapi {
/**
 * Interface definition for sequence command handlers.
 *
 * @see CommandUtil::subscribeApply
 * @see CommandUtil::subscribeCommand
 */
class SequenceCommandHandler {
public:

	/**
	 * Handler method for sequence commands. Invoked when
	 * commands are received by the GIAPI if the handler has been
	 * registered using the <code>subscribeSequenceCommand</code> or the
	 * <code>subscribeApplyCommand</code> methods.
	 *
	 * @param id the unique value that identifies the request or
	 *        equivalently identifies the set of actions started by the request.
	 *        The handler should retain the value unless the actions
	 *        started by the request are completed immediately. The same
	 *        action id will not be used again in any future request unless
	 *        the GMP is restarted
	 * @param sequenceCommand the sequence command that should be handled,
	 *        such as TEST or REBOOT.
	 * @param activity Activity requested by the sender, like PRESET_START or
	 *        CANCEL
	 * @param config  configuration associated to the command.
	 *
	 * @return a <code>pHandlerResponse</code> pointer, containing the result
	 *        of handling the command.
	 *
	 * @see CommandUtil::subscribeApply
	 * @see CommandUtil::subscribeCommand
	 */
	virtual pHandlerResponse handle(command::ActionId id,
			command::SequenceCommand sequenceCommand,
			command::Activity activity, pConfiguration config) = 0;

	/**
	 * Virtual destructor
	 */
	virtual ~SequenceCommandHandler() {};

protected:
	/**
	 * Protected constructor. This is an abstract class that can not
	 * be instantiated. Instrument code needs to extend this class and
	 * provide a concrete implementation of the <code>handle</code>
	 * method.
	 */
	SequenceCommandHandler() {};

};

/**
 * Definition of a smart pointer to a sequence command handler.
 */
typedef std::tr1::shared_ptr<SequenceCommandHandler> pSequenceCommandHandler;

}

#endif /*SEQUENCECOMMANDHANDLER_H_*/
