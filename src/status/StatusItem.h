#ifndef STATUSITEM_H_
#define STATUSITEM_H_
#include <log4cxx/logger.h>
#include <giapi/giapi.h>
#include <tr1/memory>
#include "StatusVisitor.h"
#include "KvPair.h"

namespace giapi {

/**
 * A Status Item is a Key-Value pair that holds a value representing
 * the state of a specific subsytem component at any given time.
 *
 */
class StatusItem : public KvPair {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;
private:
	bool _changedFlag; //Set when attribute is changed
	long64 _time; //timestamp
	type::Type _type; //Type of the values stored in this status item
protected:
	/**
	 * Mark the item as "dirty", allowing it to be sent. It
	 * also registers the timestamp when the item becomes dirty.
	 * </p>
	 * This method is used internally by the implementation,
	 * in particular the setValue* methods.
	 */
	void _mark();

public:
	/**
	 * Constructor. Initializes the status item with the
	 * given (unique) <code>name</name>. The status item will store
	 * a value of the given <code>type</code>
	 */
	StatusItem(const std::string &name, const type::Type type);

	virtual ~StatusItem();

	/**
	 * Set the value of the item to the provided
	 * integer value.
	 *
	 * @param value Integer value to store in the status item.
	 *
	 * @return giapi::status::OK if the value was set correctly
	 *         giapi::status::ERROR  if there is a problem setting the
	 *         value and the operation was aborted. This can happen if the
	 *         type of the status item was not defined as type::INTEGER
	 */
	virtual int setValueAsInt(int value);

	/**
	 * Set the value of the item to the provided
	 * string value.
	 *
	 * @param value String value to store in the status item.
	 *
	 * @return giapi::status::OK if the value was set correctly
	 *         giapi::status::ERROR  if there is a problem setting the
	 *         value and the operation was aborted. This can happen if the
	 *         type of the status item was not defined as type::STRING
	 */
	virtual int setValueAsString(const std::string &value);

	/**
	 * Set the value of the item to the provided
	 * double value.
	 *
	 * @param value Double value to store in the status item.
	 *
	 * @return giapi::status::OK if the value was set correctly
	 *         giapi::status::ERROR  if there is a problem setting the
	 *         value and the operation was aborted. This can happen if the
	 *         type of the status item was not defined as type::DOUBLE
	 */
	virtual int setValueAsDouble(double value);

	/**
	 * Set the value of the item to the provided
	 * float value.
	 *
	 * @param value Float value to store in the status item.
	 *
	 * @return giapi::status::OK if the value was set correctly
	 *         giapi::status::ERROR  if there is a problem setting the
	 *         value and the operation was aborted. This can happen if the
	 *         type of the status item was not defined as type::FLOAT
	 */
	virtual int setValueAsFloat(float value);

	/**
	 * Return true if the status item is changed since last time
	 * it was initialized
	 */
	bool isChanged() const;

	/**
	 * Mark the status item as "clean"
	 */
	void clearChanged();

	/**
	 * Return the status type (type of the value in this status item)
	 * for this status item
	 */
	const type::Type getStatusType() const;


	/**
	 * Return the timestamp for this status item
	 */
	const long64 getTimestamp() const;

	/**
	 * The accept interface for the visitor pattern
	 */
	virtual void accept(StatusVisitor &);
};

typedef std::tr1::shared_ptr<StatusItem> pStatusItem;
 
}

#endif /*STATUSITEM_H_*/
