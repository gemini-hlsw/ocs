#ifndef STATUSSENDER_H_
#define STATUSSENDER_H_
#include <cstdarg>
#include <giapi/giapiexcept.h>
#include <tr1/memory>

namespace giapi {
/**
 * This is the public status sender interface. These methods
 * are used by client programs to interact with the status sender.
 * Instances of status senders are obtained through the
 * {@link StatusFactory}.
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
	 * @return giapi::status::OK if the post succeeded.
	 *         giapi::status::ERROR if there is some error in the attempt
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
	 * @return giapi::status::OK if the post suceeds.
	 *         giapi::status::ERROR if there is no information associated
	 *         to the specified status item or the <code>name</code>
	 *         parameter is NULL
	 * @throws PostException in case there is a problem with the underlying
	 *         mechanisms to execute the post.
	 */
	virtual int postStatus(const std::string & name) const throw (PostException) = 0;

	virtual ~StatusSender();

protected:
	/**
	 * Protected default constructor. StatusSender concrete objects
	 * are constructed using a {@link StatusSenderFactory}.
	 *
	 * @see StatusSenderFactory
	 */
	StatusSender();
};

/**
 * Definition of a smart pointer to a StatusSender instance
 */
typedef std::tr1::shared_ptr<StatusSender> pStatusSender;

}

#endif /*STATUSSENDER_H_*/
