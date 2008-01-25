#ifndef LOGSTATUSSENDER_H_
#define LOGSTATUSSENDER_H_

#include <log4cxx/logger.h>

#include "giapi/StatusSender.h"
#include "giapi/giapiexcept.h"
#include "StatusItem.h"

namespace giapi {

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
};

}

#endif /*LOGSTATUSSENDER_H_*/
