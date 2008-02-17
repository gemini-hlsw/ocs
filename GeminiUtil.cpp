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
	
int GeminiUtil::postPCSUpdate(double zernikes[], int size) {
	return GeminiUtilImpl::Instance().postPCSUpdate(zernikes, size);
}
	
int GeminiUtil::getTCSContext(TCSContext& ctx) {
	return GeminiUtilImpl::Instance().getTCSContext(ctx);
}

}


