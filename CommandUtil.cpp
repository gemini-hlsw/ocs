#include <giapi/CommandUtil.h>
#include "LogCommandUtil.h"

namespace giapi {

CommandUtil::CommandUtil() {
}

CommandUtil::~CommandUtil() {
}

int CommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::ActivitySet activities, pSequenceCommandHandler handler) {
	return LogCommandUtil::Instance().subscribeSequenceCommand(id, activities, handler);

}

int CommandUtil::subscribeApply(const char* prefix,
		command::ActivitySet activities, pSequenceCommandHandler handler) {
	return LogCommandUtil::Instance().subscribeApply(prefix, activities, handler);
}

int CommandUtil::postCompletionInfo(command::ActionId id,
		pHandlerResponse response) {
	return LogCommandUtil::Instance().postCompletionInfo(id, response);
}

}
