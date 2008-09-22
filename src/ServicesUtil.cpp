#include "giapi/ServicesUtil.h"
#include "ServicesUtilImpl.h"
namespace giapi {

ServicesUtil::ServicesUtil() {
}

ServicesUtil::~ServicesUtil() {
}

void ServicesUtil::systemLog(log::Level level,
		const char *msg) throw (GiapiException) {
	ServicesUtilImpl::Instance().systemLog(level, msg);
}

long64 ServicesUtil::getObservatoryTime() throw (GiapiException) {
	return ServicesUtilImpl::Instance().getObservatoryTime();
}

const char * ServicesUtil::getProperty(const char *key) throw (GiapiException) {
	return ServicesUtilImpl::Instance().getProperty(key);
}

}
