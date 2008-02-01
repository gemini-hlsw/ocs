#ifndef GEMINIINTERACTIONUTIL_H_
#define GEMINIINTERACTIONUTIL_H_

#include <giapi/EPICSStatusHandler.h>
#include <giapi/giapi.h>

namespace giapi {

class GeminiInteractionUtil {
public:
	
	int subscribeEPICSStatus(const char *name, pEPICSStatusHandler handler);
	
	int unsubscribeEPISCSStatus(const char *name);
	
	int postPCSUpdate(double zernikes[], int size);
	
	const TCSContext getTCSContext() const;
	

private:
	GeminiInteractionUtil();
	virtual ~GeminiInteractionUtil();
};

}

#endif /*GEMINIINTERACTIONUTIL_H_*/
