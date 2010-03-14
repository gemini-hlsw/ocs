#include "LogCommandUtil.h"

#include <gmp/JmsUtil.h>

namespace giapi {
log4cxx::LoggerPtr LogCommandUtil::logger(log4cxx::Logger::getLogger("giapi.LogCommandUtil"));

pLogCommandUtil LogCommandUtil::INSTANCE(static_cast<LogCommandUtil *>(0));

LogCommandUtil::LogCommandUtil() {

}

LogCommandUtil::~LogCommandUtil() {
	LOG4CXX_DEBUG(logger, "Destroying Log Command Util Service");
}

pLogCommandUtil LogCommandUtil::Instance() {
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new LogCommandUtil());
	}
	return INSTANCE;
}

int LogCommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::ActivitySet activities, pSequenceCommandHandler handler) {
	LOG4CXX_INFO(logger, "Registering handler to sequence command " << gmp::JmsUtil::getTopic(id));
	return giapi::status::OK;
}

int LogCommandUtil::subscribeApply(const std::string & prefix,
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
