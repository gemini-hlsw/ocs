#include "DataUtilImpl.h"

namespace giapi {

log4cxx::LoggerPtr DataUtilImpl::logger(log4cxx::Logger::getLogger("giapi.DataUtilImpl"));

std::auto_ptr<DataUtilImpl> DataUtilImpl::INSTANCE(new DataUtilImpl());

DataUtilImpl::DataUtilImpl() {
}

DataUtilImpl::~DataUtilImpl() {
}

DataUtilImpl& DataUtilImpl::Instance() {
	return *INSTANCE;
}

int DataUtilImpl::postObservationEvent(data::ObservationEvent event,
		const std::string & datalabel) {

	char *eventName = "UNKNOWN";

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
	}

	LOG4CXX_INFO(logger, "postObservationEvent: " << eventName << " datalabel: " << datalabel);
	return status::OK;
}

int DataUtilImpl::postAncillaryFileEvent(const std::string & filename,
		const std::string & datalabel) {
	LOG4CXX_INFO(logger, "postAncilliaryFileEvent: Filename " << filename
			<< " datalabel " << datalabel);
	return status::OK;
}

int DataUtilImpl::postIntermediateFileEvent(const std::string & filename,
		const std::string & datalabel, const std::string & hint) {
	LOG4CXX_INFO(logger, "postIntermediateFileEvent: Filename " << filename
			<< " datalabel " << datalabel << " hint " << hint);
	return status::OK;
}

}
