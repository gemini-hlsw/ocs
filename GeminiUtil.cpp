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


int GeminiUtil::subscribeEPICSStatus(const char *name, pEPICSStatusHandler handler) {
	return GeminiUtilImpl::Instance().subscribeEPICSStatus(name, handler);
}
	
int GeminiUtil::unsubscribeEPICSStatus(const char *name) {
	return GeminiUtilImpl::Instance().unsubscribeEPICSStatus(name);
}
	
int GeminiUtil::postPCSUpdate(double zernikes[], int size) {
	return GeminiUtilImpl::Instance().postPCSUpdate(zernikes, size);
}
	
int GeminiUtil::getTCSContext(TCSContext& ctx) {
	return GeminiUtilImpl::Instance().getTCSContext(ctx);
}

}


