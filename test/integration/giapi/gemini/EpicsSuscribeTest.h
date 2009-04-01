/*
 * EpicsSuscribeTest.h
 *
 *  Created on: Feb 16, 2009
 *      Author: anunez
 */

#ifndef EPICSSUSCRIBETEST_H_
#define EPICSSUSCRIBETEST_H_

#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>
#include <giapi/EpicsStatusHandler.h>

namespace giapi {
namespace gemini {

class EpicsSubscribeTest : public CppUnit::TestFixture {

	CPPUNIT_TEST_SUITE( EpicsSubscribeTest );

	CPPUNIT_TEST ( testSubscribeEpics );
	CPPUNIT_TEST ( testUnsubscribeEpics );


	CPPUNIT_TEST_SUITE_END();

public:
	EpicsSubscribeTest();
	virtual ~EpicsSubscribeTest();

	void testSubscribeEpics();

	void testUnsubscribeEpics();

	void setUp();

	void tearDown();

private:
	pEpicsStatusHandler _handler;

};

}
}



#include <giapi/EpicsStatusItem.h>

class MyEpicsHandler : public giapi::EpicsStatusHandler {

public:
	virtual void channelChanged(giapi::pEpicsStatusItem item);
	virtual ~MyEpicsHandler() {}
	static giapi::pEpicsStatusHandler create();
private:
	MyEpicsHandler() {}
};




#endif /* EPICSSUSCRIBETEST_H_ */
