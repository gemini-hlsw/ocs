/*
 * EpicsSuscribeTest.cpp
 *
 *  Created on: Feb 16, 2009
 *      Author: anunez
 */

#include "EpicsSuscribeTest.h"
#include <giapi/GeminiUtil.h>

namespace giapi {
namespace gemini {

EpicsSubscribeTest::EpicsSubscribeTest() {
}

EpicsSubscribeTest::~EpicsSubscribeTest() {
}


void EpicsSubscribeTest::setUp() {
	_handler = MyEpicsHandler::create();
}

void EpicsSubscribeTest::tearDown() {

}

void EpicsSubscribeTest::testSubscribeEpics() {

	CPPUNIT_ASSERT( GeminiUtil::subscribeEpicsStatus("tcs:context", _handler) == giapi::status::OK );



}

}
}


//// Epics Status Handler code
using namespace giapi;

pEpicsStatusHandler MyEpicsHandler::create() {
	 pEpicsStatusHandler handler(new MyEpicsHandler());
	 return handler;
}

void MyEpicsHandler::channelChanged(pEpicsStatusItem item) {
	//do something with it.
}






