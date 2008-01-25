#ifndef STATUSSENDER_H_
#define STATUSSENDER_H_
#include <giapi/giapiexcept.h>
namespace giapi {
/**
 * This is the public status sender interface. These methods
 * are used by client programs to interact with the status sender. 
 * Instances of status senders are obtained through the  
 * @{link StatusFactory}. 
 * 
 * @see StatusFactory
 */
class StatusSender {
public:

	/**
	 * Post all pending status to Gemini. Pending statuses are those
	 * whose value has changed since last time posted by GIAPI. A status
	 * item is marked as "dirty" every time a new value is set. 
	 * 
	 * @return giapi::status::GIAPI_OK if the post suceeded. 
	 *         giapi::status::GIAPI_WARNING if there is no status item pending
	 *         for sending  
	 *         giapi::status::GIAPI_NOK if there is some error in the attempt
	 *         to send  
	 * @throws PostException in case there is a problem with the underlying 
	 *         mechanisms to execute the post. 
	 */
	virtual int postStatus() const throw (PostException) = 0;

	/**
	 * Post the specified status to Gemini. The status will be sent only
	 * if it has changed its value since the last time it was posted. A status
	 * item is marked as "dirty" every time a new value is set. 
	 * 
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
	virtual int postStatus(const char* name) const throw (PostException) = 0;

	virtual ~StatusSender();

protected:
	StatusSender();
};

}

#endif /*STATUSSENDER_H_*/
