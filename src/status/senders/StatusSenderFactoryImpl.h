#ifndef STATUSFACTORYIMPL_H_
#define STATUSFACTORYIMPL_H_
#include <status/senders/StatusSenderFactory.h>
#include <status/senders/StatusSender.h>

namespace giapi {
/**
 * Concrete implementation of the status factory interface.
 * Provide mechanisms to retrieve status senders
 */
class StatusSenderFactoryImpl : public StatusSenderFactory {
private:
	pStatusSender senders[StatusSenderFactory::Elements];
	static const StatusSenderType DEFAULT_SENDER = JMS_SENDER;
public:
	/**
	 * Default constructor
	 */
	StatusSenderFactoryImpl();
	virtual ~StatusSenderFactoryImpl();

	/**
	 * Return the default StatusSender instance. The default status sender is
	 * defined internally by the implementing class. Several calls to this
	 * method will return a reference to the same object.
	 *
	 * @return the default StatusSender
	 */
	virtual pStatusSender getStatusSender();

	/**
	 * Return the StatusSender specified by the <code>type</code>. Several
	 * calls to this method with the same argument will return a reference
	 * to the same object
	 *
	 * @return StatusSender associated to the type.
	 */
	virtual pStatusSender getStatusSender(StatusSenderType type);

};
}

#endif /*STATUSFACTORYIMPL_H_*/
