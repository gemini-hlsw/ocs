#ifndef STATUSUTIL_H_
#define STATUSUTIL_H_
#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>
//$Id$
namespace giapi {

/**
 * A collection of utility mechanisms to handle status information and 
 * alarms in the GIAPI.
 * 
 */
class StatusUtil {

public:
	/**
	 * Create an status item in the GIAPI framework. 
	 *  
	 * @param name The name of the status item that will be 
	 * 	           created. If an status item with the same name already exists, 
	 *             the method will return giapi::status::ERROR
	 * @param type Type information for the values to be stored in the status 
	 *             item. 
	 * 
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::ERROR if there is an error. 
	 */
	static int createStatusItem(const char * name, const type::Type type);

	/**
	 * Create an alarm status item in the GIAPI framework. An alarm 
	 * status item is a status item that adds Severity, Cause and a Message  
	 *  
	 * @param name The name of the alarm status item that will be 
	 * 	           created. If an status item with the same name already exists, 
	 *             the method will return giapi::status::ERROR
	 * @param type Type information for the values to be stored in the status 
	 *             item. 
	 * 
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::ERROR if there is an error. 
	 */
	static int createAlarmStatusItem(const char *name, const type::Type type);

	/**
	 * Create a health status item in the GIAPI framework. A health status 
	 * item provides the overall operational status of a system or subsystem. 
	 * The health can be: health::GOOD, health::WARNING or health::BAD. 
	 * </p>
	 * The default state for health after creation is health::GOOD.   
	 *  
	 * @param name The name of the health status item that will be 
	 * 	           created. If an status item with the same name already exists, 
	 *             the method will return giapi::status::ERROR
	 * 
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::ERROR if there is an error. 
	 */
	static int createHealthStatusItem(const char *name);
	
	/**
	 * Post all pending status to Gemini. Pending statuses are those
	 * whose value has changed since last time posted by GIAPI.
	 * 
	 * @return giapi::status::OK if the post suceeded. 
	 *         giapi::status::WARNING if there is no status item pending
	 *         for sending  
	 *         giapi::status::ERROR if there is some error in the attempt
	 *         to send  
	 * 
	 * @throws PostException in case there is a problem with the underlying 
	 *         mechanisms to execute the post. 
	 */
	static int postStatus() throw (PostException);

	/**
	 * Post specified status to Gemini. The status will be sent only
	 * if it has changed since the last time it was posted. 
	 * 
	 * @args   name The name of the status item to be posted
	 * 
	 * @return giapi::status::OK if the post suceeds. 
	 *         giapi::status::WARNING if the status item value has not 
	 *         changed since last post, therefore there is no need to send it
	 *         again.   
	 *         giapi::status::ERROR if there is no information associated
	 *         to the specified status item or the <code>name</code> 
	 *         parameter is NULL
	 * 
	 * @throws PostException in case there is a problem with the underlying 
	 *         mechanisms to execute the post. 
	 */
	static int postStatus(const char* name) throw (PostException);

	/**
	 * Set the value of the given status item to the provided 
	 * integer value.
	 * 
	 * @param name Name of the status item whose value will be set
	 * @param value Integer value to store in the status item. 
	 * 
	 * @return giapi::status::OK if the value was set correctly 
	 *         giapi::status::WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::ERROR  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen if the 
	 *         type of the status item was not defined as type::INTEGER, 
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL  
	 */
	static int setValueAsInt(const char *name, int value);

	/**
	 * Set the value of the given status item to the provided 
	 * char value.
	 * 
	 * @param name Name of the status item whose value will be set
	 * @param value char value to store in the status item. 
	 * 
	 * @return giapi::status::OK if the value was set correctly 
	 *         giapi::status::WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::ERROR  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen if the 
	 *         type of the status item was not defined as type::CHAR, 
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL  
	 */

	static int setValueAsChar(const char *name, char value);

	/**
	 * Set the value of the given status item to the provided 
	 * string value.
	 * 
	 * @param name Name of the status item whose value will be set
	 * @param value String value to store in the status item. 
	 * 
	 * @return giapi::status::OK if the value was set correctly. The 
	 *         StatusItem is marked dirty. 
	 *         giapi::status::WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::ERROR  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen if the 
	 *         type of the status item was not defined as type::STRING, 
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL  
	 * 			  
	 */
	static int setValueAsString(const char *name, const char *value);

	/**
	 * Set the value of the given status item to the provided 
	 * double value.
	 * 
	 * @param name Name of the status item whose value will be set
	 * @param value double value to store in the status item. 
	 * 
	 * @return giapi::status::OK if the value was set correctly 
	 *         giapi::status::WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::ERROR  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen if the 
	 *         type of the status item was not defined as type::DOUBLE, 
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL    
	 */
	static int setValueAsDouble(const char *name, double value);

	/**
	 * Set the alarm for the specified status alarm item. 
	 * 
	 * @param name Name of the alarm item. The alarm items should have been 
	 *             initialized by a call to {@link #createAlarmStatusItem()}. 
	 *             Failing to do so will return an error. 
	 *             
	 * @param severity the alarm severity.
	 * @param cause the cause of the alarm 
	 * @param message Optional message to describe the alarm. This argument is
	 *        mandatory if the cause is alarm::ALARM_CAUSE_OTHER. 
	 * 
	 * @return giapi::status::OK if alarm was sucessfully set 
	 *         giapi::status::ERROR if there was an error setting the alarm (for
	 *         instance, the alarm item hasn't been created or the name doesn't
	 *         correspond to an alarm status item). giapi::status::ERROR is 
	 *         returned also if no message is specified when the cause is 
	 *         set to alarm::ALARM_CAUSE_OTHER.
	 * 
	 * @see giapi::alarm::Severity
	 * @see giapi::alarm::Cause
	 */
	static int setAlarm(const char *name, alarm::Severity severity,
			alarm::Cause cause, const char *message = 0);
	
	/**
	 * Clear the alarm state of the alarm status item specified 
	 * by name. 
	 * 
	 * @param name Name of the alarm item. The alarm items should have been 
	 *             initialized by a call to {@link #createAlarmStatusItem()}. 
	 *             Failing to do so will return an error
	 * @return giapi::status::OK if the alarm was cleared
	 *         giapi::status::ERROR if there was an error clearing the alarm
	 *         (for instance, the alarm has not been created, or the name is
	 *         not associated to an alarm status item)
	 */
	static int clearAlarm(const char *name);
	
	/**
	 * Set the health value for the given health status item
	 * 
	 * @param name Name of the health item. The health item must have been 
	 *             initialized by a call to {@link #createHealthStatusItem()}. 
	 *             Failing to do so will return an error. 
	 *             
	 * @param health the health state of the health status item specified by 
	 *               name
	 * 
	 * @return giapi::status::OK if the health was sucessfully set 
	 *         giapi::status::ERROR if there was an error setting the health 
	 *         (for instance, the health status item hasn't been created or
	 *         the name doesn't correspond to a health status item).  
	 * 
	 * @see giapi::health::Health
	 * 
	 */
	static int setHealth(const char *name, const health::Health health);

	

private:
	StatusUtil();
	~StatusUtil();
};

}
#endif /*STATUSUTIL_H_*/
