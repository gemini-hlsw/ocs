#include <giapi/CommandUtil.h>
#include "LogCommandUtil.h"
#include "JmsCommandUtil.h"

namespace giapi {

CommandUtil::CommandUtil() {
}

CommandUtil::~CommandUtil() {
}

int CommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (GiapiException){
	//Uninitialized sequence command handler
	if (handler == 0) {
		return giapi::status::ERROR;
	}
	return JmsCommandUtil::Instance()->subscribeSequenceCommand(id, activities, handler);

}

int CommandUtil::subscribeApply(const std::string & prefix,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (GiapiException){
	//Uninitialized sequence command handler
	if (handler == 0) {
		return giapi::status::ERROR;
	}
	return JmsCommandUtil::Instance()->subscribeApply(prefix, activities, handler);
}

int CommandUtil::postCompletionInfo(command::ActionId id,
		pHandlerResponse response) throw (GiapiException) {
	//Uninitialized handler response for completion info
	if (response == 0) {
		return giapi::status::ERROR;
	}
	//Invalid responses for completion info.
	//Validates the response is either COMPLETED or ERROR. If
	//ERROR, check whether we have a message or not. It is
	//an error to generate an ERROR response without an error
	//message
	switch (response->getResponse()) {
		case HandlerResponse::COMPLETED:
			break; //all right
		case HandlerResponse::ERROR:
			if (!(response->getMessage().empty()))
				break; //all right
		default:
			//in all the other cases, return error
			return giapi::status::ERROR;
	}

	return JmsCommandUtil::Instance()->postCompletionInfo(id, response);
}

}
