#ifndef SERVICESUTILIMPL_H_
#define SERVICESUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>

#include <giapi/giapi.h>

namespace giapi {

class ServicesUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;
	
public:
	static ServicesUtilImpl& Instance();
	
	void systemLog(log::Level level, const char *msg);

	long64 getObservatoryTime();
	
	const char * getProperty(const char *key);
	
	virtual ~ServicesUtilImpl();

private:
	static std::auto_ptr<ServicesUtilImpl> INSTANCE;
	ServicesUtilImpl();
};

}

#endif /*SERVICESUTILIMPL_H_*/
