#include "giapi/DataUtil.h"
#include "DataUtilImpl.h"
namespace giapi {

DataUtil::DataUtil() {
}

DataUtil::~DataUtil() {
}

int DataUtil::postObservationEvent(data::ObservationEvent event,
		const char * datalabel) throw (GiapiException) {
	return DataUtilImpl::Instance().postObservationEvent(event, datalabel);
}

int DataUtil::postAncillaryFileEvent(const char * filename,
		const char * datalabel) throw (GiapiException) {
	return DataUtilImpl::Instance().postAncillaryFileEvent(filename, datalabel);
}

int DataUtil::postIntermediateFileEvent(const char * filename,
		const char * datalabel, const char * hint) throw (GiapiException) {
	return DataUtilImpl::Instance().postIntermediateFileEvent(filename, datalabel, hint);
}

}
