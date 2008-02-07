#include "giapi/ServicesUtil.h"
#include "ServicesUtilImpl.h"
namespace giapi {

ServicesUtil::ServicesUtil() {
}

ServicesUtil::~ServicesUtil() {
}

void ServicesUtil::systemLog(log::Level level, const char *msg) {
	ServicesUtilImpl::Instance().systemLog(level, msg);
}

long64 ServicesUtil::getObservatoryTime() {
	return ServicesUtilImpl::Instance().getObservatoryTime();
}

const char * ServicesUtil::getProperty(const char *key) {
	return ServicesUtilImpl::Instance().getProperty(key);
}

}
