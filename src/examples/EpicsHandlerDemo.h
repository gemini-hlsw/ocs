/*
 * An example implementation of an Epics Status Handler
 */

#ifndef EPICSHANDLERDEMO_H_
#define EPICSHANDLERDEMO_H_

#include <giapi/EpicsStatusHandler.h>
#include <giapi/EpicsStatusItem.h>

using namespace giapi;

class EpicsHandlerDemo : public EpicsStatusHandler{
public:
	virtual ~EpicsHandlerDemo();

	/**
	 * This is the callback that will be invoked whenever
	 * the registered channel changes.
	 */
	virtual void channelChanged(pEpicsStatusItem item);

	static pEpicsStatusHandler create();

private:
	EpicsHandlerDemo();

};

#endif /* EPICSHANDLERDEMO_H_ */
