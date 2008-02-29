#include "giapi/GeminiUtil.h"
#include "GeminiUtilImpl.h"

namespace giapi
{

GeminiUtil::GeminiUtil()
{
}

GeminiUtil::~GeminiUtil()
{
}


int GeminiUtil::subscribeEpicsStatus(const char *name, pEpicsStatusHandler handler) {
	return GeminiUtilImpl::Instance().subscribeEpicsStatus(name, handler);
}
	
int GeminiUtil::unsubscribeEpicsStatus(const char *name) {
	return GeminiUtilImpl::Instance().unsubscribeEpicsStatus(name);
}
	
int GeminiUtil::postPcsUpdate(double zernikes[], int size) {
	return GeminiUtilImpl::Instance().postPcsUpdate(zernikes, size);
}
	
int GeminiUtil::getTcsContext(TcsContext& ctx) {
	return GeminiUtilImpl::Instance().getTcsContext(ctx);
}

}


