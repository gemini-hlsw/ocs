#ifndef STATUSFACTORY_H_
#define STATUSFACTORY_H_
#include <tr1/memory>

#include <status/senders/StatusSender.h>

namespace giapi {

class StatusSenderFactory;
typedef std::tr1::shared_ptr<StatusSenderFactory> pStatusSenderFactory;

/**
 * This is the class that instantiates the {@link StatusSender} concrete
 * classes as requested. The specific {@link StatusSender} that will
 * be retrieved will depend on the type specified to the getter methods,
 * defined by the {@link StatusSenderType} enumerated type.
 *
 */
class StatusSenderFactory {
public:
	/**
	 * The different type of Status Senders available
	 */
	enum StatusSenderType {
		/**
		 * A LOG_SENDER logs the post commands, but doesn't really send
		 * items outside the current environment
		 */
		LOG_SENDER,
		/**
		 * A JMS_SENDER uses JMS technology to broadcast status information
		 * to the Gemini Master Process (GMP).
		 */
		JMS_SENDER,
		/**
		 * Auxiliary item to be used as the count of items in this
		 * enumeration. Should not be used as a valid StatusSenderType!!
		 */
		Elements
	};

	/**
	 * Return the singleton instance of this factory.
	 *
	 * @return the singleton instance of this factory.
	 */
	static pStatusSenderFactory Instance(void);

	/**
	 * Return the default StatusSender instance. The default status sender is
	 * defined internally by the implementing class. Several calls to this
	 * method will return a reference to the same object.
	 *
	 * @return the default StatusSender
	 */
	virtual pStatusSender getStatusSender(void) = 0;

	/**
	 * Return the StatusSender specified by the <code>type</code>. Several
	 * calls to this method with the same argument will return a reference
	 * to the same object
	 *
	 * @param type the Status Sender type that will be retrieved by this call
	 *
	 * @return StatusSender associated to the type.
	 */
	virtual pStatusSender getStatusSender(const StatusSenderType type) = 0;

	/**
	 * Destructor.
	 */
	virtual ~StatusSenderFactory();
private:
	/**
	 * Internal instance of this factory
	 */
	static pStatusSenderFactory INSTANCE;
protected:
	/**
	 * Protected default constructor, instantiated internally by the
	 * Instance() method
	 */
	StatusSenderFactory();

};

}

#endif /*STATUSFACTORY_H_*/
