#include "DataUtilImpl.h"

namespace giapi {

log4cxx::LoggerPtr DataUtilImpl::logger(log4cxx::Logger::getLogger("giapi.DataUtilImpl"));

std::auto_ptr<DataUtilImpl> DataUtilImpl::INSTANCE(0);

DataUtilImpl::DataUtilImpl() throw (CommunicationException) {
	pObsEventProducer = JmsObsEventProducer::create();
}

DataUtilImpl::~DataUtilImpl() {
	pObsEventProducer.release(); //just to make sure the object is destroyed.
}

DataUtilImpl& DataUtilImpl::Instance() throw (CommunicationException) {
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new DataUtilImpl());
	}
	return *INSTANCE;
}

int DataUtilImpl::postObservationEvent(data::ObservationEvent event,
		const std::string & datalabel) throw (CommunicationException) {

	return pObsEventProducer->postEvent(event, datalabel);

}

int DataUtilImpl::postAncillaryFileEvent(const std::string & filename,
		const std::string & datalabel) throw (CommunicationException) {
	LOG4CXX_INFO(logger, "postAncilliaryFileEvent: Filename " << filename
			<< " datalabel " << datalabel);
	return status::OK;
}

int DataUtilImpl::postIntermediateFileEvent(const std::string & filename,
		const std::string & datalabel, const std::string & hint) throw (CommunicationException) {
	LOG4CXX_INFO(logger, "postIntermediateFileEvent: Filename " << filename
			<< " datalabel " << datalabel << " hint " << hint);
	return status::OK;
}

}
