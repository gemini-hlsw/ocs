#ifndef LOGSTATUSSENDER_H_
#define LOGSTATUSSENDER_H_

#include <log4cxx/logger.h>
#include <giapi/giapiexcept.h>

#include "StatusSender.h"
#include <status/StatusItem.h>

namespace giapi {
/**
 * A Status Sender that logs the post commands
 */
class LogStatusSender : public StatusSender {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	LogStatusSender();
	virtual ~LogStatusSender();

	virtual int postStatus() const throw (PostException);
	virtual int postStatus(const char* name) const throw (PostException);

private:
	int postStatus(StatusItem *item) const throw (PostException);;
};

}

#endif /*LOGSTATUSSENDER_H_*/
