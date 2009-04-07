#ifndef EPICSSTATUSITEM_H_
#define EPICSSTATUSITEM_H_
#include <tr1/memory>
#include <string>
#include <list>

#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

namespace giapi {

/**
 * Interface for an Epics Status Item.
 *
 * EpicsStatusItem objects are managed internally
 * by the GIAPI. Instrument code will get smart pointer references to
 * these objects when the <code>update</code> method of the
 * <code>EpicsStatusHandler</code> interface is invoked.
 *
 */
class EpicsStatusItem {
public:
	/**
	 * Return the name of the Epics channel
	 * represented by this status item
	 *
	 * @return Name of the Epics channel represented by
	 *         this epics status item
	 */
	virtual const std::string getName() const = 0;

	/**
	 * Return the data type of the values contained
	 * in this epics status item
	 *
	 * @return the data type of the value or values
	 *         contained in this epics status item.
	 */
	virtual type::Type getType() const = 0;

	/**
	 * Provides access to the data associated to the
	 * epics status item. The caller is responsible
	 * to decode the content of the data.
	 *
	 * @return pointer to the data contained in this
	 *         epics status item.
	 */
	virtual const void * getData() const = 0;

	/**
	 * Return the number of elements stored in the data
	 * part of this epics status item. All the elements
	 * are of the same data type, as returned by the
	 * <code>getType</code> method.
	 *
	 * @return number of elements stored in the data
	 *         part of this epics status element
	 */
	virtual int getCount() const = 0;

	/**
	 * Virtual destructor
	 */
	virtual ~EpicsStatusItem();

	/**
	 * Read the data element at the specified index position as
	 * a string.
	 *
	 * @param index index of the element to retrieve. The index must be
	 * in the range 0 <= index < getCount()
	 *
	 * @return data element in the specified index as a standard string
	 *
	 * @throws InvalidOperationException If this status item type
	 * is not a string or the index provided is greater or equals to the
	 * number of elements in this status item (as returned by the
	 * the getCount() method)
	 */
	virtual const std::string getDataAsString(int index) const
			throw (InvalidOperation) = 0;

	/**
	 * Read the data element at the specified index position as
	 * an integer value
	 *
	 * @param index index of the element to retrieve. The index must be
	 * in the range 0 <= index < getCount()
	 *
	 * @return data element in the specified index as an integer
	 *
	 * @throws InvalidOperationException If this status item type
	 * is not integer or the index provided is greater or equals to the
	 * number of elements in this status item (as returned by the
	 * the getCount() method)
	 */
	virtual int getDataAsInt(int index) const throw (InvalidOperation) = 0;

	/**
	 * Read the data element at the specified index position as
	 * a float.
	 *
	 * @param index index of the element to retrieve. The index must be
	 * in the range 0 <= index < getCount()
	 *
	 * @return data element in the specified index as a float value
	 *
	 * @throws InvalidOperationException If this status item type
	 * is not float or the index provided is greater or equals to the
	 * number of elements in this status item (as returned by the
	 * the getCount() method)
	 */
	virtual float getDataAsFloat(int index) const throw (InvalidOperation) = 0;

	/**
	 * Read the data element at the specified index position as
	 * a double.
	 *
	 * @param index index of the element to retrieve. The index must be
	 * in the range 0 <= index < getCount()
	 *
	 * @return data element in the specified index as a double value
	 *
	 * @throws InvalidOperationException If this status item type
	 * is not double or the index provided is greater or equals to the
	 * number of elements in this status item (as returned by the
	 * the getCount() method)
	 */
	virtual double getDataAsDouble(int index) const throw (InvalidOperation) = 0;

	/**
	 * Read the data element at the specified index position as
	 * a byte.
	 *
	 * @param index index of the element to retrieve. The index must be
	 * in the range 0 <= index < getCount()
	 *
	 * @return data element in the specified index as a byte value
	 *
	 * @throws InvalidOperationException If this status item type
	 * is not byte or the index provided is greater or equals to the
	 * number of elements in this status item (as returned by the
	 * the getCount() method)
	 */
	virtual unsigned char getDataAsByte(int index) const throw (InvalidOperation) = 0;

};

/**
 * Definition of a smart pointer to an EpicsStatusItem instance
 */
typedef std::tr1::shared_ptr<EpicsStatusItem> pEpicsStatusItem;
}

#endif /*EPICSSTATUSITEM_H_*/
