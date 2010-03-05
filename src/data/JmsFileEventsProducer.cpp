#include "JmsFileEventsProducer.h"
#include <gmp/GMPKeys.h>
#include <util/StringUtil.h>

using namespace gmp;

namespace giapi {

using namespace util;

JmsFileEventsProducer::JmsFileEventsProducer() throw (CommunicationException) :
	JmsProducer(GMPKeys::GMP_DATA_FILEEVENT_DESTINATION) {
}

JmsFileEventsProducer::~JmsFileEventsProducer() {
}

pJmsFileEventsProducer JmsFileEventsProducer::create()
		throw (CommunicationException) {
	pJmsFileEventsProducer producer(new JmsFileEventsProducer());
	return producer;
}

int JmsFileEventsProducer::postAncillaryFileEvent(const std::string & filename,
		const std::string & dataLabel) throw (CommunicationException) {

	/* Sends an Ancillary File event with the given parameters */
	return sendFileEventMessage(ANCILLARY_TYPE, filename, dataLabel);

}

int JmsFileEventsProducer::postIntermediateFileEvent(
		const std::string & filename, const std::string & dataLabel,
		const std::string &hint) throw (CommunicationException) {
	/* Sends an Intermediate File event with the given parameters */
	return sendFileEventMessage(INTERMEDIATE_TYPE, filename, dataLabel, hint);
}

int JmsFileEventsProducer::sendFileEventMessage(EventFileType type,
		const std::string & filename, const std::string & dataLabel,
		const std::string &hint) {

	/* If the filename is empty, this is an error */
	if (StringUtil::isEmpty(filename)) {
		LOG4CXX_WARN(logger, "The filename cannot be empty for file events ");
		return status::ERROR;
	}

	/* If the datalabel is empty, this is an error */
	if (StringUtil::isEmpty(dataLabel)) {
		LOG4CXX_WARN(logger, "The datalabel cannot be empty for file events ");
		return status::ERROR;
	}

	MapMessage * msg = NULL;
	try {
		/* Creates the Map Message to be used to dispatch the File Event */
		msg = _session->createMapMessage();


		/* The type is encoded as a message property in the message, so
		 * clients can filter it.
		 */
		msg->setIntProperty(GMPKeys::GMP_DATA_FILEEVENT_TYPE, type);

		/*
		 * The filename and the datalabel are common between file events.
		 */
		msg->setString(GMPKeys::GMP_DATA_FILEEVENT_FILENAME, filename);
		msg->setString(GMPKeys::GMP_DATA_FILEEVENT_DATALABEL, dataLabel);

		/*
		 * Intermediate file events have an optional "hint" parameter, we
		 * add it to the MapMessage if it exists.
		 */
		if (type == INTERMEDIATE_TYPE && !StringUtil::isEmpty(hint)) {
			msg->setString(GMPKeys::GMP_DATA_FILEEVENT_HINT, hint);
		}

		/* Finally we send the message */
		_producer->send(msg);
		/* Destroy the message */
		delete msg;

	} catch (CMSException &e) {
		if (msg != NULL) {
			delete msg;
		}
		throw CommunicationException("Problem sending file event " + filename
				+ " associated to datalabel: " + dataLabel);
	}
	return status::OK;
}

int JmsFileEventsProducer::sendFileEventMessage(EventFileType type,
		const std::string & filename, const std::string & dataLabel) {
	return sendFileEventMessage(type, filename, dataLabel, "");
}


}
