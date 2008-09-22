#include "LogCommandUtil.h"

#include <gmp/JmsUtil.h>

namespace giapi {
log4cxx::LoggerPtr LogCommandUtil::logger(log4cxx::Logger::getLogger("giapi.LogCommandUtil"));

std::auto_ptr<LogCommandUtil> LogCommandUtil::INSTANCE(0);

LogCommandUtil::LogCommandUtil() {

}

LogCommandUtil::~LogCommandUtil() {
}

LogCommandUtil& LogCommandUtil::Instance() {
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new LogCommandUtil());
	}
	return *INSTANCE;
}

int LogCommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::ActivitySet activities, pSequenceCommandHandler handler) {
	LOG4CXX_INFO(logger, "Registering handler to sequence command " << gmp::JmsUtil::getTopic(id));
	return giapi::status::OK;
}

int LogCommandUtil::subscribeApply(const char* prefix,
		command::ActivitySet activities, pSequenceCommandHandler handler) {
	LOG4CXX_INFO(logger, "Registering handler to apply prefix " << prefix);
	return giapi::status::OK;
}

int LogCommandUtil::postCompletionInfo(command::ActionId id,
		pHandlerResponse response) {
	LOG4CXX_INFO(logger, "Posting completion info for action id  " << id);
	return giapi::status::OK;
}

}
