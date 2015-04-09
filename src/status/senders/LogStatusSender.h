#ifndef LOGSTATUSSENDER_H_
#define LOGSTATUSSENDER_H_

#include <cstdarg>
#include <log4cxx/logger.h>
#include <giapi/giapiexcept.h>

#include <status/senders/AbstractStatusSender.h>
#include <status/StatusItem.h>

namespace giapi {
/**
 * A Status Sender that logs the post commands
 */
class LogStatusSender : public AbstractStatusSender {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	LogStatusSender();
	virtual ~LogStatusSender();

protected:
	virtual int postStatus(pStatusItem item) const throw (PostException);
};

}

#endif /*LOGSTATUSSENDER_H_*/
