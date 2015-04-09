/*
 *
 */

#ifndef ABSTRACTSTATUSSENDER_H_
#define ABSTRACTSTATUSSENDER_H_

#include <cstdarg>
#include <log4cxx/logger.h>

#include <giapi/giapiexcept.h>

#include <status/senders/StatusSender.h>
#include <status/StatusItem.h>



namespace giapi {
/**
 * The Abstract Status Sender provides the general implementation
 * for the methods that post status information.
 *
 * This class is responsible for finding the appropriate StatusItem
 * to post (if it exists), and making sure only "dirty" items are
 * posted. Dirty items are those that have changed since the last time
 * they were posted.
 *
 * In addition to that, once the status is ready to be dispatched, this
 * class will mark them as "clean", so no new posts will be done on the same
 * item unless it changes its value.
 *
 */
class AbstractStatusSender : public StatusSender {
public:
	AbstractStatusSender();
	virtual ~AbstractStatusSender();

	/**
	 * Post all the dirty items. Look for the dirty items in
	 * the internal database that holds all the status information,
	 * and post them
	 */
	virtual int postStatus() const throw (PostException);

	/**
	 * Post the specific status item, identified by name. The
	 * status item will be sent only if it has been modified
	 * since the last time it was posted.
	 */
	virtual int postStatus(const std::string &name) const throw (PostException);

protected:
	/**
	 * This abstract post method must be implemented to perform
	 * the post operation of the specific status item. Different implementations
	 * can use different mechanisms to post the item, like JMS, or simply
	 * logging.
	 *
	 * The Status Item argument is validated at the time this method is
	 * invoked so implementors can assume the Status Item can be posted
	 * immediately.
	 */
	virtual int postStatus(pStatusItem item) const throw (PostException) = 0;

private:
	/**
	 * An internal method that will validate whether the status item has
	 * changed since the last post. If so, it will mark the item as  "clean",
	 * and will send it to the underlying post mechanism defined by
	 * implementing classes of the postStatus(StatusItem *item) method.
	 */
	int doPost(pStatusItem item) const throw (PostException);
	/*
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

};

}

#endif /* ABSTRACTSTATUSSENDER_H_ */
