#include <data/JmsObsEventProducer.h>

#include <gmp/GMPKeys.h>

#include <util/StringUtil.h>

using namespace gmp;
namespace giapi {

using namespace util;

JmsObsEventProducer::JmsObsEventProducer() throw (CommunicationException) :
	JmsProducer(GMPKeys::GMP_DATA_OBSEVENT_DESTINATION) {
}

JmsObsEventProducer::~JmsObsEventProducer() {

}

pJmsObsEventProducer JmsObsEventProducer::create()
		throw (CommunicationException) {
	pJmsObsEventProducer producer(new JmsObsEventProducer());
	return producer;
}

int JmsObsEventProducer::postEvent(data::ObservationEvent event,
		const std::string &dataLabel) throw (CommunicationException) {

	std::string eventName = "UNKNOWN";

	switch (event) {
	case data::OBS_END_ACQ:
		eventName = "OBS_END_ACQ";
		break;
	case data::OBS_END_DSET_WRITE:
		eventName = "OBS_END_DSET_WRITE";
		break;
	case data::OBS_END_READOUT:
		eventName = "OBS_END_READOUT";
		break;
	case data::OBS_PREP:
		eventName = "OBS_PREP";
		break;
	case data::OBS_START_ACQ:
		eventName = "OBS_START_ACQ";
		break;
	case data::OBS_START_DSET_WRITE:
		eventName = "OBS_START_DSET_WRITE";
		break;
	case data::OBS_START_READOUT:
		eventName = "OBS_START_READOUT";
		break;
	default:
		return status::ERROR;
	}

	/* If the string is empty, this is an error */
	if (StringUtil::isEmpty(dataLabel)) {
		LOG4CXX_WARN(logger, "The datalabel cannot be empty for observation event "<< eventName);
		return status::ERROR;
	}

	LOG4CXX_INFO(logger, "Observation Event: " << eventName << " datalabel: " << dataLabel);


	Message * msg = NULL;
	try {
		msg = _session->createMessage();

		msg->setStringProperty(GMPKeys::GMP_DATA_OBSEVENT_NAME, eventName);
		msg->setStringProperty(GMPKeys::GMP_DATA_OBSEVENT_FILENAME, dataLabel);

		_producer->send(msg);

		/* Destroy the message */
		delete msg;

	} catch (CMSException &e) {
		if (msg != NULL) {
			delete msg;
		}
		throw CommunicationException("Problem sending observation event "
				+ eventName + " associated to filename: " + dataLabel);
	}

	return status::OK;

}

}
