#include <giapi/ServicesUtil.h>
#include "ServicesUtilImpl.h"
namespace giapi {

ServicesUtil::ServicesUtil() {
}

ServicesUtil::~ServicesUtil() {
}

void ServicesUtil::systemLog(log::Level level,
		const std::string &msg) throw (GiapiException) {
	ServicesUtilImpl::Instance()->systemLog(level, msg);
}

long64 ServicesUtil::getObservatoryTime() throw (GiapiException) {
	return ServicesUtilImpl::Instance()->getObservatoryTime();
}

const std::string ServicesUtil::getProperty(const std::string &key, long timeout) throw (GiapiException) {
	return ServicesUtilImpl::Instance()->getProperty(key, timeout);
}

}
