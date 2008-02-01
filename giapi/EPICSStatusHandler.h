#ifndef EPICSSTATUSHANDLER_H_
#define EPICSSTATUSHANDLER_H_
#include <tr1/memory>

namespace giapi
{

class EPICSStatusHandler
{
public:
	EPICSStatusHandler();
	virtual ~EPICSStatusHandler();
};

typedef std::tr1::shared_ptr<EPICSStatusHandler> pEPICSStatusHandler;

}

#endif /*EPICSSTATUSHANDLER_H_*/
