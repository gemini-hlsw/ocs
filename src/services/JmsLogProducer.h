/**
 * A JMS Producer that sends logging information to the GMP
 */

#ifndef JMSLOGPRODUCER_H_
#define JMSLOGPRODUCER_H_

#include <cstdarg>
#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>
#include <util/jms/JmsProducer.h>


namespace giapi {
/**
 * Forward declaration of JMS Class
 */
class JmsLogProducer;

/**
 * Smart pointer definition for this class
 */
typedef std::auto_ptr<JmsLogProducer> pJmsLogProducer;

/**
 * A JMS Producer in charge of sending Logs to the GMP
 */
class JmsLogProducer :  public util::jms::JmsProducer  {
public:
	/**
	 * Factory method to create a new JmsLogProducer.
	 * This factory returns a smart pointer object to
	 * simplify the management.
	 */
	static pJmsLogProducer create() throw (CommunicationException);

	/**
	 * Sends the system logging information to the GMP.
	 */
	void postLog(log::Level level, const std::string &msg)
		throw (CommunicationException);

	virtual ~JmsLogProducer();
private:
	/**
	 * Private constructor.
	 */
	JmsLogProducer() throw (CommunicationException) ;
};

}

#endif /* JMSLOGPRODUCER_H_ */
