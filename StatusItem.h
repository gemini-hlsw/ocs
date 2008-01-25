#ifndef STATUSITEM_H_
#define STATUSITEM_H_
#include <log4cxx/logger.h>

#include "KvPair.h"

namespace giapi {
class StatusItem : public KvPair {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;
private:
	bool _changedFlag;   //Set when attribute is changed
	unsigned long _time; //timestamp
protected:
	/**
	 * Mark the item as "dirty", allowing it to be sent. It 
	 * also registers the timestamp when the value was set. 
	 * </p>
	 * This method is used internally by the implementation, 
	 * in particular the setValue* methods. 
	 * 
	 */ 
	void _mark();
	
public:
	StatusItem(const char *name);

	virtual ~StatusItem();
	
	//Setters
	virtual int setValueAsInt(int value);
	virtual int setValueAsString(const char * value);
	
	
	
	/**
	 * Return true if the status item is changed since last time 
	 * it was initialized
	 */
	bool isChanged() const;
	
	/**
	 * Mark the status item as "clean"
	 */
	void clearChanged();
};

}

#endif /*STATUSITEM_H_*/
