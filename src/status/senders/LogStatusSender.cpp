#include "LogStatusSender.h"
#include <status/StatusItem.h>
#include <status/StatusDatabase.h>
#include <giapi/giapi.h>

#include <vector>

namespace giapi {

log4cxx::LoggerPtr LogStatusSender::logger(log4cxx::Logger::getLogger("giapi.LogStatusSender"));

LogStatusSender::LogStatusSender() {
}

LogStatusSender::~LogStatusSender() {
	LOG4CXX_DEBUG(logger, "Destroying LogStatus Sender");
}

int LogStatusSender::postStatus(pStatusItem statusItem) const
		throw (PostException) {
	LOG4CXX_INFO(logger, "Post Status Item " << statusItem->getName());
	return status::OK;
}

}
