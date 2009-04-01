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
	CPPUNIT_ASSERT( GeminiUtil::subscribeEpicsStatus("ws:wsFilter.VALL", _handler) == giapi::status::OK );
	CPPUNIT_ASSERT( GeminiUtil::subscribeEpicsStatus("random-channel", _handler) == giapi::status::ERROR );
}

void EpicsSubscribeTest::testUnsubscribeEpics() {
	CPPUNIT_ASSERT( GeminiUtil::unsubscribeEpicsStatus("ws:wsFilter.VALL") == giapi::status::OK );

	CPPUNIT_ASSERT( GeminiUtil::unsubscribeEpicsStatus("random-channel") == giapi::status::ERROR );
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






