#ifndef JMSSTATUSSENDER_H_
#define JMSSTATUSSENDER_H_

#include <log4cxx/logger.h>

#include <giapi/StatusSender.h>
#include <giapi/giapiexcept.h>

namespace giapi {
/**
 * A Status Sender that uses JMS as the underlying communication
 * mechanism
 */
class JmsStatusSender : public StatusSender {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	JmsStatusSender();
	virtual ~JmsStatusSender();
	virtual int postStatus(const char* statusItem) const throw (PostException);
	virtual int postStatus() const throw (PostException);

};

}

#endif /*JMSSTATUSSENDER_H_*/
