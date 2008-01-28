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
	 *             the method will return giapi::status::NOK
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::NOK if there is an error. 
	 */
	static int createStatusItem(const char * name);

	/**
	 * Create an alarm status item in the GIAPI framework. An alarm 
	 * status item is a status item that adds Severity, Cause and a Message  
	 *  
	 * @param name The name of the alarm status item that will be 
	 * 	           created. If an status item with the same name already exists, 
	 *             the method will return giapi::status::NOK
	 * @return giapi::status::OK if the item was sucessfully created, 
	 *         giapi::status::NOK if there is an error. 
	 */
	static int createAlarmStatusItem(const char *name);

	/**
	 * Post all pending status to Gemini. Pending statuses are those
	 * whose value has changed since last time posted by GIAPI.
	 * </p>
	 * @return giapi::status::GIAPI_OK if the post suceeded. 
	 *         giapi::status::GIAPI_WARNING if there is no status item pending
	 *         for sending  
	 *         giapi::status::GIAPI_NOK if there is some error in the attempt
	 *         to send  
	 * @throws PostException in case there is a problem with the underlying 
	 *         mechanisms to execute the post. 
	 */
	static int postStatus() throw (PostException);

	/**
	 * Post specified status to Gemini. The status will be sent only
	 * if it has changed since the last time it was posted. 
	 * </p>
	 * @args   name The name of the status item to be posted
	 * @return giapi::status::GIAPI_OK if the post suceeds. 
	 *         giapi::status::GIAPI_WARNING if the status item value has not 
	 *         changed since last post, therefore there is no need to send it
	 *         again.   
	 *         giapi::status::GIAPI_NOK if there is no information associated
	 *         to the specified status item or the <code>name</code> 
	 *         parameter is NULL
	 * @throws PostException in case there is a problem with the underlying 
	 *         mechanisms to execute the post. 
	 */
	static int postStatus(const char* name) throw (PostException);

	/**
	 * Set the value of the current status item to the provided 
	 * integer value.
	 * @param name Name of the status item whose value will be set
	 * @param value Integer value to set in the status item. 
	 * @return giapi::status::GIAPI_OK if the value was set correctly 
	 *         giapi::status::GIAPI_WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::GIAPI_NOK  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen for instance
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL  
	 */
	static int setValueAsInt(const char *name, int value);

	/**
	 * Set the value of the current status item to the provided 
	 * char value.
	 * @param name Name of the status item whose value will be set
	 * @param value char value to set in the status item. 
	 * @return giapi::status::GIAPI_OK if the value was set correctly 
	 *         giapi::status::GIAPI_WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::GIAPI_NOK  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen for instance
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL  
	 */

	static int setValueAsChar(const char *name, char value);

	/**
	 * Set the value of the current status item to the provided 
	 * string value.
	 * @param name Name of the status item whose value will be set
	 * @param value String value to set in the status item. 
	 * @return giapi::status::GIAPI_OK if the value was set correctly. The 
	 *         StatusItem is marked dirty. 
	 *         giapi::status::GIAPI_WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::GIAPI_NOK  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen for instance
	 *         if there is no StatusItem associated to the <code>name</code> or
	 *         if <code>name</code> is set to NULL  
	 * 			  
	 */
	static int setValueAsString(const char *name, const char *value);

	/**
	 * Set the value of the current status item to the provided 
	 * double value.
	 * @param name Name of the status item whose value will be set
	 * @param value double value to set in the status item. 
	 * @return giapi::status::GIAPI_OK if the value was set correctly 
	 *         giapi::status::GIAPI_WARNING if the current status value is
	 *         already set to the new value. The StatusItem will not 
	 *         be marked as dirty in this case. 
	 *         giapi::status::GIAPI_NOK  if there is a problem setting the 
	 *         value and the operation was aborted. This can happen for instance
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
	 * @param message Optional message to describe the alarm
	 * 
	 * @return giapi::status::OK if alarm was sucessfully set 
	 *         giapi::status::NOK if there was an error setting the alarm (for
	 *         instance, the alarm item hasn't been created or the name doesn't
	 *         correspond to an alarm status item).  
	 * 
	 * @see alarm::Severity
	 * @see alarm::Cause
	 */
	static int setAlarm(const char *name, alarm::Severity severity,
			alarm::Cause cause, const char *message = 0);

private:
	StatusUtil();
	~StatusUtil();
};

}
#endif /*STATUSUTIL_H_*/
