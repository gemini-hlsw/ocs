#ifndef EPICSSTATUSITEM_H_
#define EPICSSTATUSITEM_H_
#include <tr1/memory>

#include <giapi/giapi.h>

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
	virtual const char * getName() const = 0;

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
	 * epics status item.
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
};

/**
 * Definition of a smart pointer to an EpicsStatusItem instance 
 */
typedef std::tr1::shared_ptr<EpicsStatusItem> pEpicsStatusItem;
}

#endif /*EPICSSTATUSITEM_H_*/
