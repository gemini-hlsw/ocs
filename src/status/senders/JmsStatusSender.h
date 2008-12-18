#ifndef JMSSTATUSSENDER_H_
#define JMSSTATUSSENDER_H_

#include <log4cxx/logger.h>

#include <status/senders/AbstractStatusSender.h>
#include <giapi/giapiexcept.h>

namespace giapi {
/**
 * A Status Sender that uses JMS as the underlying communication
 * mechanism
 */
class JmsStatusSender : public AbstractStatusSender {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

public:
	JmsStatusSender();
	virtual ~JmsStatusSender();
protected:
	virtual int postStatus(StatusItem *item) const throw (PostException);

};

}

#endif /*JMSSTATUSSENDER_H_*/
