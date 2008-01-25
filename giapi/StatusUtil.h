#ifndef STATUSUTIL_H_
#define STATUSUTIL_H_

#include <giapi/giapiexcept.h>

namespace giapi {

/**
 * A collection of utility mechanisms to deal with status in GIAPI
 */
class StatusUtil {

public:
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
	 *         value so the operation was aborted. This can happen for instance
	 *         if the <code>name</code> is set to NULL  
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
	 *         value so the operation was aborted. This can happen for instance
	 *         if the <code>name</code> is set to NULL  
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
	 *         value so the operation was aborted. This can happen for instance
	 *         if the <code>name</code> is set to NULL. The StatusItem will not
	 *         be marked as dirty in this case. 
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
	 *         value so the operation was aborted. This can happen for instance
	 *         if the <code>name</code> is set to NULL  
	 */
	static int setValueAsDouble(const char *name, double value);

private:
	StatusUtil();
	~StatusUtil();
};

}
#endif /*STATUSUTIL_H_*/
