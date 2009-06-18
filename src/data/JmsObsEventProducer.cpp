/*
 * JmsObsEventProducer.cpp
 *
 *  Created on: Jun 10, 2009
 *      Author: anunez
 */

#include <data/JmsObsEventProducer.h>

#include <gmp/GMPKeys.h>

using namespace gmp;

namespace giapi {

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
		const std::string &fileName) throw (CommunicationException) {

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

	LOG4CXX_INFO(logger, "Observation Event: " << eventName << " datalabel: " << fileName);

	Message * msg;
	try {
		msg = _session->createMessage();

		msg->setStringProperty(GMPKeys::GMP_DATA_OBSEVENT_NAME, eventName);
		msg->setStringProperty(GMPKeys::GMP_DATA_OBSEVENT_FILENAME, fileName);

		_producer->send(msg);

	} catch (CMSException &e) {
		throw CommunicationException("Problem sending observation event "
				+ eventName + " associated to filename: " + fileName);
	}

	return status::OK;

}

}
