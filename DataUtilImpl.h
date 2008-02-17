#ifndef DATAUTILIMPL_H_
#define DATAUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>
#include <giapi/giapi.h>

namespace giapi {

class DataUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;
public:
	static DataUtilImpl& Instance();

	int postObservationEvent(data::ObservationEvent event,
			const char * datalabel);

	int postAncillaryFileEvent(const char * filename, const char * datalabel);

	int postIntermediateFileEvent(const char * filename,
					const char * datalabel, const char * hint);

	virtual ~DataUtilImpl();

private:
	static std::auto_ptr<DataUtilImpl> INSTANCE;
	DataUtilImpl();
};

}

#endif /*DATAUTILIMPL_H_*/
