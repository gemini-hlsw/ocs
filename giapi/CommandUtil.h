#ifndef COMMANDUTIL_H_
#define COMMANDUTIL_H_

#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>

#include <tr1/memory>
/**
 * A collection of utility methods to deal with Commands. 
 */
namespace giapi {

class CommandUtil {
public:

	//TODO: All these calls will interact with the GMP. Hence
	//they might need to throw connection exceptions of some sort. 
	
	/**
	 * Associates the given handler to the sequence command specified. 
	 * 
	 * @param id The <code>SequenceCommand</code> enumerated type that 
	 *        identifies the sequence command to which a handler will be 
	 *        associated. 
	 * @param activities Array of Activity enumerated types that supports
	 *        the usage of different handlers for different activities. 
	 * @param handler. Pointer to a concrete implementation of a 
	 *        SequenceCommandHandler that will be associated to the given
	 *        sequence command and actions. The most recently registered
	 *        sequence command handler for a sequence command will be used. 
	 * 
	 * @return giapi::status::OK if the subscription suceeds. Otherwise, it
	 *         returns giapi::status::ERROR. 
	 */
	static int subscribeSequenceCommand(command::SequenceCommand id,
			command::Activity activities[],
			std::tr1::shared_ptr<SequenceCommandHandler> handler);

	/**
	 * Associates the given handler to the configuration prefix specified and
	 * array of Activity elements. 
	 * <p/>
	 * When the system receives a configuration it will split it up according
	 * to the registered handlers and pass each the part of the configuration
	 * that is registered to handle.
	 * </p>
	 * The <code>Activity</code> array allows different handlers for different
	 * activities. For instance, one handler can implement only <b>preset</b> 
	 * and another <b>start</b>.
	 * 
	 * @param id The <code>SequenceCommand</code> enumerated type that 
	 *        identifies the sequence command to which a handler will be 
	 *        associated. 
	 * @param activities Array of Activity enumerated types that supports
	 *        the usage of different handlers for different activities. 
	 * @param handler. Pointer to a concrete implementation of a 
	 *        SequenceCommandHandler that will be associated to the given
	 *        sequence command and actions. The most recently registered
	 *        sequence command handler for a sequence command will be used. 
	 * 
	 * @return giapi::status::OK if the subscription suceeds. Otherwise, it
	 *         returns giapi::status::ERROR. 
	 */
	static int subscribeApply(const char* prefix,
			command::Activity activities[],
			std::tr1::shared_ptr<SequenceCommandHandler> handler);

	/**
	 * Post completion information to the GMP for actions that do not complete
	 * immediately. This case is triggered when a {@link SequenceCommandHandler}
	 * returns <b>STARTED</b> in the {@link HandlerResponse}. The system will, 
	 * when the actions complete sucessfully or with an error, notify the 
	 * GMP of the completion status of the actions associated with the 
	 * original <code>ActionId</code>, using this call. 
	 * 
	 * @param id the original ActionId associated to the actions for which we 
	 *        are reporting completion info. 
	 * @param response contains the completion state associated to the action
	 *        id. Valid response type are only COMPLETED and ERROR. 
	 * @param errorMsg error description, in case the response is ERROR. 
	 * 
	 * @return giapi::status::OK if the subscription suceeds. Otherwise, it
	 *         returns giapi::status::ERROR. 
	 */
	static int postCompletionInfo(command::ActionId id,
			std::tr1::shared_ptr<HandlerResponse> response,
			const char * errorMsg);

private:
	CommandUtil();
	virtual ~CommandUtil();
};
}

#endif /*COMMANDUTIL_H_*/
