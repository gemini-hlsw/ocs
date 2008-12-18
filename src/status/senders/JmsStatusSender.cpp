#include "JmsStatusSender.h"
#include <giapi/giapi.h>


namespace giapi {

log4cxx::LoggerPtr JmsStatusSender::logger(log4cxx::Logger::getLogger("giapi.JmsStatusSender"));

JmsStatusSender::JmsStatusSender() {
	LOG4CXX_DEBUG(logger, "Constructing JMS Status sender")

}

JmsStatusSender::~JmsStatusSender() {
	LOG4CXX_DEBUG(logger, "Destroying JMS Status sender")
}

int JmsStatusSender::postStatus(StatusItem * statusItem) const throw (PostException) {
	LOG4CXX_DEBUG(logger, "Post Status Item " << *statusItem);
	return giapi::status::OK;
}



}
