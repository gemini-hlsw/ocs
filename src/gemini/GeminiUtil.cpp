#include <giapi/GeminiUtil.h>
#include "GeminiUtilImpl.h"

namespace giapi
{

GeminiUtil::GeminiUtil()
{
}

GeminiUtil::~GeminiUtil()
{
}


int GeminiUtil::subscribeEpicsStatus(const std::string &name,
		pEpicsStatusHandler handler) throw (GiapiException){
	return GeminiUtilImpl::Instance()->subscribeEpicsStatus(name, handler);
}

int GeminiUtil::unsubscribeEpicsStatus(const std::string &name) throw (GiapiException){
	return GeminiUtilImpl::Instance()->unsubscribeEpicsStatus(name);
}

int GeminiUtil::postPcsUpdate(double zernikes[],
		int size) throw (GiapiException) {
	return GeminiUtilImpl::Instance()->postPcsUpdate(zernikes, size);
}

int GeminiUtil::getTcsContext(TcsContext& ctx, long timeout) throw (GiapiException) {
	return GeminiUtilImpl::Instance()->getTcsContext(ctx, timeout);
}

pEpicsStatusItem GeminiUtil::getChannel(const std::string &name, long timeout) throw (GiapiException)  {
  return GeminiUtilImpl::Instance()->getChannel(name, timeout);
}

}


