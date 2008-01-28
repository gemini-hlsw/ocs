#ifndef ALARMSTATUSITEM_H_
#define ALARMSTATUSITEM_H_

#include "StatusItem.h"
#include <giapi/giapi.h>

namespace giapi {
/**
 * An Alarm Status Item is a Status Item that adds a severity, a cause
 * and an optional message when an exceptional condition happens in any
 * of the subsytems that might require user's attention.
 */
class AlarmStatusItem : public StatusItem {
private:
	const char * _message; //description of the cause of the alarm
	alarm::Severity _severity; //alarm severity
	alarm::Cause _cause; //alarm cause

public:
	/**
	 * Constructor. Initializes the status item with the
	 * given (unique) <code>name</name>. The status item will store
	 * a value of the given <code>type</code>
	 */
	AlarmStatusItem(const char *name, const type::Type type);
	virtual ~AlarmStatusItem();

	/**
	 * Set the alarm state of this item. Once set, the
	 * internal state of the item is marked as dirty, 
	 * therefore subsequent post invokations will cause
	 * this item to be published. If the alarm severity
	 * is set to alarm::NO_ALARM, the cause and message
	 * are automatically reset no matter what values are
	 * specified in the method parameters
	 * 
	 * @param severity the severity of the alarm. If there 
	 * is no alarm condition, the value must be set
	 * to alarm::NO_ALARM. If set to alarm::NO_ALARM, the 
	 * cause and message arguments are discarded. 
	 * @param cause the cause of the alarm. If there is 
	 * no alarm condition, the value must be set 
	 * to alarm::NO_CAUSE
	 * @param message the (optional) alarm message for this 
	 * status item
	 */
	void setAlarmState(alarm::Severity severity, alarm::Cause cause,
			const char * message = 0);
	
	/**
	 * Reset the alarm information. The status item is in a 
	 * no-alarm state after this call
	 */
	void reset();

	/**
	 * Return the (optional) message associated 
	 * to the alarm item. If no message is defined
	 * the method returns NULL
	 * 
	 * @return the message associated to the alarm item 
	 * or NULL if there is no one.
	 */
	const char * getMessage() const;

	/**
	 * Return the severity of the alarm
	 * 
	 * @return Alarm severity
	 */
	alarm::Severity getSeverity() const;

	/**
	 * Return the cause of the alarm
	 * 
	 * @return the alarm cause. 
	 */
	alarm::Cause getCause() const;

};
}

#endif /*ALARMSTATUSITEM_H_*/
