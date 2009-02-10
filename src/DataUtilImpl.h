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
			const std::string & datalabel);

	int postAncillaryFileEvent(const std::string & filename, const std::string & datalabel);

	int postIntermediateFileEvent(const std::string & filename,
					const std::string & datalabel, const std::string & hint);

	virtual ~DataUtilImpl();

private:
	static std::auto_ptr<DataUtilImpl> INSTANCE;
	DataUtilImpl();
};

}

#endif /*DATAUTILIMPL_H_*/
