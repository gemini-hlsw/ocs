#ifndef STATUSFACTORYIMPL_H_
#define STATUSFACTORYIMPL_H_
#include <giapi/StatusFactory.h>

namespace giapi {
/**
 * Concrete implementation of the status factory interface.
 * Provide mechanisms to retrieve status senders
 */
class StatusFactoryImpl : public StatusFactory {
private:
	StatusSender* senders[StatusFactory::Elements];
	static const StatusSenderType DEFAULT_SENDER = LogSender;
public:
	/**
	 * Default constructor
	 */
	StatusFactoryImpl();
	virtual ~StatusFactoryImpl();

	/**
	 * Return the default StatusSender instance. The default status sender is 
	 * defined internally by the implementing class. Several calls to this
	 * method will return a reference to the same object.
	 *   
	 * @return the default StatusSender
	 */
	virtual StatusSender& getStatusSender();

	/**
	 * Return the StatusSender specified by the <code>type</code>. Several 
	 * calls to this method with the same argument will return a reference 
	 * to the same object
	 *   
	 * @return StatusSender associated to the type. 
	 */
	virtual StatusSender& getStatusSender(StatusSenderType type);

};
}

#endif /*STATUSFACTORYIMPL_H_*/
