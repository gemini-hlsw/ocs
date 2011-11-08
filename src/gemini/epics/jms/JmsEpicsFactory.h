/*
 * JmsEpicsFactory.h
 *
 *  Created on: Mar 30, 2009
 *      Author: anunez
 */

#ifndef JMSEPICSFACTORY_H_
#define JMSEPICSFACTORY_H_

#include <giapi/EpicsStatusItem.h>
#include <giapi/giapiexcept.h>

#include <cms/BytesMessage.h>
#include <log4cxx/logger.h>

namespace giapi {

/**
 * Factory to reconstruct EPICS Status Item updates from the received
 * JMS Message. This factory uses internally JMS Epics Builder objects
 * to perform the actual decode of the messages.
 */

class JmsEpicsFactory {

public:

	/**
	 * Returns the EPICS Status Item update contained in the BytesMessage
	 */
	static pEpicsStatusItem buildEpicsStatusItem(const cms::BytesMessage * msg);

	virtual ~JmsEpicsFactory();
private:
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;

	JmsEpicsFactory();

	/**
	 * Data Types encoded in the JMS message, that represent the
	 * data type of the EPICS update
	 */
	enum DataType {
		SHORT = 1,
		INT = 5,
		DOUBLE = 6,
		FLOAT =2,
		STRING = 0,
		BYTE = 4
	};

};

}

#endif /* JMSEPICSFACTORY_H_ */
