#ifndef GEMINIINTERACTIONUTIL_H_
#define GEMINIINTERACTIONUTIL_H_

#include <giapi/EPICSStatusHandler.h>
#include <giapi/giapi.h>

namespace giapi {

class GeminiUtil {
public:
	
	static int subscribeEPICSStatus(const char *name, pEPICSStatusHandler handler);
	
	static int unsubscribeEPISCSStatus(const char *name);
	
	static int postPCSUpdate(double zernikes[], int size);
	
	static int getTCSContext(TCSContext& ctx);
	

private:
	GeminiUtil();
	virtual ~GeminiUtil();
};

}

#endif /*GEMINIINTERACTIONUTIL_H_*/
