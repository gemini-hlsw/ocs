#ifndef JMSEPICSBUILDERBASE_H_
#define JMSEPICSBUILDERBASE_H_


#include <giapi/EpicsStatusItem.h>

#include <cms/BytesMessage.h>

#include <string>

using namespace cms;

namespace giapi {

/**
 * This is the base class for all the EPICS builder objects. An EPICS
 * builder is an object that can extract an EPICS Status Item from
 * an appropriate JMS Message.
 */
class JmsEpicsBuilder {
public:
	JmsEpicsBuilder(BytesMessage * bm);
	virtual ~JmsEpicsBuilder();

	/**
	 * Return a smart pointer to an EPICS status item contained in the
	 * JMS Message specified during construction. Implementations of this
	 * method will deal with the specific data types contained in the
	 * EPICS status items supported by the GIAPI.
	 */
	virtual pEpicsStatusItem getEpicsStatusItem() = 0;

protected:
	/**
	 * Name of the EPICS status item
	 */
	std::string _name;

	/**
	 * Number of elements in the status item. All of the elements
	 * are of the same type
	 */
	int _nElements;

	/**
	 * The BytesMessage that contains the codified EPICS status item.
	 */
	BytesMessage * _message;

};

}

#endif /* JMSEPICSBUILDERBASE_H_ */
