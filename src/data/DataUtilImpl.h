#ifndef DATAUTILIMPL_H_
#define DATAUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>
#include <giapi/giapi.h>

#include <data/JmsObsEventProducer.h>
#include <data/JmsFileEventsProducer.h>

namespace giapi {

class DataUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;
public:
	static DataUtilImpl& Instance() throw (CommunicationException);

	int postObservationEvent(data::ObservationEvent event, const std::string & datalabel) throw (CommunicationException);

	int postAncillaryFileEvent(const std::string & filename, const std::string & datalabel) throw (CommunicationException);

	int postIntermediateFileEvent(const std::string & filename,
					const std::string & datalabel, const std::string & hint) throw (CommunicationException);

	virtual ~DataUtilImpl();

private:
	static std::auto_ptr<DataUtilImpl> INSTANCE;

	pJmsObsEventProducer pObsEventProducer;

	pJmsFileEventsProducer pFileEventsProducer;

	DataUtilImpl() throw (CommunicationException);
};

}

#endif /*DATAUTILIMPL_H_*/
