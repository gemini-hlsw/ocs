#ifndef SERVICESUTILIMPL_H_
#define SERVICESUTILIMPL_H_

#include <tr1/memory>
#include <log4cxx/logger.h>

namespace giapi {

class ServicesUtilImpl {
	/**
	 * Logging facility
	 */
	static log4cxx::LoggerPtr logger;
	
public:
	static ServicesUtilImpl& Instance();
	virtual ~ServicesUtilImpl();

private:
	static std::auto_ptr<ServicesUtilImpl> INSTANCE;
	ServicesUtilImpl();
};

}

#endif /*SERVICESUTILIMPL_H_*/
