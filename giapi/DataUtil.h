#ifndef DATAUTIL_H_
#define DATAUTIL_H_

#include <giapi/giapi.h>

namespace giapi {

class DataUtil {
public:
	static int postObservationEvent(data::ObservationEvent event,
			const char * datalabel);

	static int postAncillaryFileEvent(const char * filename,
			const char * datalabel);
	
	static int postIntermediateFileEvent(const char * filename,
			const char * datalabel);

private:
	DataUtil();
	virtual ~DataUtil();
};

}

#endif /*DATAUTIL_H_*/
