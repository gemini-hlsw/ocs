#ifndef GEMINISERVICESUTIL_H_
#define GEMINISERVICESUTIL_H_
#include <giapi/giapi.h>

namespace giapi {

class ServicesUtil {
public:
	static void systemLog(log::Level level, const char *msg);

	static long64 getObservatoryTime();
	
	static const char * getProperty(const char *key);

private:
	ServicesUtil();
	virtual ~ServicesUtil();
};

}

#endif /*GEMINISERVICESUTIL_H_*/
