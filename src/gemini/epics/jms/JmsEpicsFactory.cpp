
#include "JmsEpicsFactory.h"

#include <log4cxx/logger.h>

#include <gemini/epics/EpicsStatusItemImpl.h>

#include "JmsDoubleEpicsBuilder.h"
#include "JmsIntEpicsBuilder.h"
#include "JmsStringEpicsBuilder.h"
#include "JmsByteEpicsBuilder.h"
#include "JmsFloatEpicsBuilder.h"
#include "JmsShortEpicsBuilder.h"

namespace giapi {

log4cxx::LoggerPtr JmsEpicsFactory::logger(log4cxx::Logger::getLogger(
		"giapi::gemini::JmsEpicsFactory"));

JmsEpicsFactory::JmsEpicsFactory() {

}

JmsEpicsFactory::~JmsEpicsFactory() {
}

pEpicsStatusItem JmsEpicsFactory::buildEpicsStatusItem(
		const cms::BytesMessage * message) {

	cms::BytesMessage * msg = message->clone(); //clone this message, so we can put it in read mode

	pEpicsStatusItem item;

	JmsEpicsBuilder * builder = NULL;

	try {
		msg->reset(); //to mark the message as read only.
		//type
		unsigned char type = msg->readByte();

		switch (type) {
		case (SHORT):
			builder = new JmsShortEpicsBuilder(msg);
			break;

		case (INT):
			builder = new JmsIntEpicsBuilder(msg);
			break;

		case (DOUBLE):
			builder = new JmsDoubleEpicsBuilder(msg);
			break;

		case (FLOAT):
			builder = new JmsFloatEpicsBuilder(msg);

			break;

		case (STRING):
			builder = new JmsStringEpicsBuilder(msg);
			break;

		case (BYTE):
			builder = new JmsByteEpicsBuilder(msg);
			break;

		}

	} catch (cms::CMSException &e) {
		LOG4CXX_WARN(logger, "Problem parsing EPICS status item: " << e.getMessage());
	}

	//if we got a right builder, retrieve the item using it.
	if (builder != NULL) {
		item = builder->getEpicsStatusItem();
		delete builder;
	}

	if (msg != NULL) {
		delete msg;
	}

	return item;

}

}
