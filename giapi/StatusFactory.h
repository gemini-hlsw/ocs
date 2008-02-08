#ifndef STATUSFACTORY_H_
#define STATUSFACTORY_H_
#include <giapi/StatusSender.h>
#include <tr1/memory>

namespace giapi {
/**
 * This is the class that instantiates the {@link StatusSender} concrete
 * classes as requested. The specific {@link StatusSender} that will
 * be retrieved will depend on the type specified to the getter methods,
 * defined by the {@link StatusSenderType} enumerated type.
 * 
 */
class StatusFactory {
public:
	/**
	 * The different type of Status Senders available
	 */
	enum StatusSenderType {
		/**
		 * A LogSender logs the post commands, but doesn't really send
		 * items outside the current environment
		 */
		LogSender,
		/**
		 * A JMSSender uses JMS technology to braodcast status information
		 * to the Gemini Master Process (GMP). 
		 */
		JMSSender,
		/**
		 * Auxiliary item to be used as the count of items in this 
		 * enumeration
		 */
		Elements
	};

	/**
	 * Return the singleton instance of this factory.
	 * 
	 * @return the singleton instance of this factory.
	 */
	static StatusFactory& Instance(void); 
	
	/**
	 * Return the default StatusSender instance. The default status sender is 
	 * defined internally by the implementing class. Several calls to this
	 * method will return a reference to the same object.
	 *   
	 * @return the default StatusSender
	 */
	virtual StatusSender& getStatusSender(void) = 0;
	
	/**
	 * Return the StatusSender specified by the <code>type</code>. Several 
	 * calls to this method with the same argument will return a reference 
	 * to the same object
	 *   
	 * @return StatusSender associated to the type. 
	 */
	virtual StatusSender& getStatusSender(const StatusSenderType type) = 0;
	
	/**
	 * Destructor. 
	 */
	virtual ~StatusFactory();
private:
	/**
	 * Internal instance of this factory
	 */
	static std::auto_ptr<StatusFactory> INSTANCE;
protected:
	/**
	 * Protected default constructor, instantiated internally by the 
	 * Instance() method
	 */
	StatusFactory();
	
};

}

#endif /*STATUSFACTORY_H_*/
