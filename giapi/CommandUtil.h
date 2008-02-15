#ifndef COMMANDUTIL_H_
#define COMMANDUTIL_H_

#include <giapi/giapi.h>

#include <giapi/SequenceCommandHandler.h>
#include <giapi/HandlerResponse.h>

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
	 * <p/>
	 * The <code>ActivitySet</code> argument allows different handlers for 
	 * different activities. For instance, one handler can implement only 
	 * <b>preset</b> and another <b>start</b>.
	 * 
	 * @param id The <code>SequenceCommand</code> enumerated type that 
	 *        identifies the sequence command to which a handler will be 
	 *        associated. 
	 * @param activities Enumerated type representing the set of Activities 
	 *        the handler will be associated with for the given sequence
	 *        command.
	 * @param handler. Pointer to a concrete implementation of a 
	 *        SequenceCommandHandler that will be associated to the given
	 *        sequence command and actions. The most recently registered
	 *        sequence command handler for a sequence command will be used. 
	 * 
	 * @return giapi::status::OK if the subscription suceeds. Otherwise, it
	 *         returns giapi::status::ERROR.
	 *
	 * @see command::Activity 
	 */
	static int subscribeSequenceCommand(command::SequenceCommand id,
			command::ActivitySet activities, pSequenceCommandHandler handler);

	/**
	 * Associates the given handler to the configuration prefix specified and
	 * array of Activity elements. 
	 * <p/>
	 * When the system receives a configuration it will split it up according
	 * to the registered handlers and pass each the part of the configuration
	 * that is registered to handle.
	 * </p>
	 * The <code>ActivitySet</code> argument allows different handlers for 
	 * different activities. For instance, one handler can implement only 
	 * <b>preset</b> and another <b>start</b>.
	 * 
	 * @param prefix The configuration prefix this handler will take care 
	 *        of. When a configuration is received by the system, it will 
	 *        be split and the part indicated by this prefix will be  
	 *        passed to the handler.  
	 * @param activities Enumerated type representing the set of Activities 
	 *        the handler will be associated with for the given prefix.
	 * @param handler. Pointer to a concrete implementation of a 
	 *        SequenceCommandHandler that will be associated to the given
	 *        sequence command and actions. The most recently registered
	 *        sequence command handler for a sequence command will be used. 
	 * 
	 * @return giapi::status::OK if the subscription suceeds. Otherwise, it
	 *         returns giapi::status::ERROR.
	 * 
	 * @see command::Activity 
	 */
	static int subscribeApply(const char* prefix,
			command::ActivitySet activities, pSequenceCommandHandler handler);

	/**
	 * Post completion information to the GMP for actions that do not complete
	 * immediately. This case is triggered when a {@link SequenceCommandHandler}
	 * returns <b>STARTED</b> in the {@link HandlerResponse}. The system will, 
	 * when the actions complete successfully or with an error, notify the 
	 * GMP of the completion status of the actions associated with the 
	 * original <code>ActionId</code>, using this call. 
	 * 
	 * @param id the original ActionId associated to the actions for which we 
	 *        are reporting completion info. 
	 * @param response contains the completion state associated to the action
	 *        id. Valid response type are only COMPLETED and ERROR. In case 
	 *        the response is ERROR, a message should be provided. 
	 * 
	 * @return giapi::status::OK if the post suceeds. Otherwise, it
	 *         returns giapi::status::ERROR. 
	 */
	static int postCompletionInfo(command::ActionId id,
			pHandlerResponse response);

private:
	CommandUtil();
	virtual ~CommandUtil();
};
}

#endif /*COMMANDUTIL_H_*/
