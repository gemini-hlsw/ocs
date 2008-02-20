#include "GiapiStatusTest.h"
#include <giapi/giapi.h>
#include <giapi/StatusUtil.h>
using namespace giapi;

CPPUNIT_TEST_SUITE_REGISTRATION( GiapiStatusTest );

GiapiStatusTest::~GiapiStatusTest() {
	
}

void GiapiStatusTest::setUp() {
	//Initialize a few status items
	StatusUtil::createStatusItem("test-item", giapi::type::INT);
	StatusUtil::createAlarmStatusItem("alarm-item", giapi::type::DOUBLE);
	StatusUtil::createHealthStatusItem("health-item");
	
}

void GiapiStatusTest::tearDown() {

}

void GiapiStatusTest::testCreateStatusItem() {
	//should return errror, test-item already exists
	CPPUNIT_ASSERT( StatusUtil::createStatusItem("test-item", giapi::type::INT) == giapi::status::ERROR );
	//should work  fine. 
	CPPUNIT_ASSERT( StatusUtil::createStatusItem("test-item-2", giapi::type::INT) == giapi::status::OK );
}


void GiapiStatusTest::testCreateAlarmStatusItem() {
	//should return error, test-item already exists
	CPPUNIT_ASSERT( StatusUtil::createAlarmStatusItem("alarm-item", giapi::type::INT) == giapi::status::ERROR );
	//should work out  fine.
	CPPUNIT_ASSERT( StatusUtil::createAlarmStatusItem("alarm-item-2", giapi::type::INT) == giapi::status::OK );
}

void GiapiStatusTest::testCreateHealthStatusItem() {
	//should return error, health-item already exists
	CPPUNIT_ASSERT(StatusUtil::createHealthStatusItem("health-item") == giapi::status::ERROR );
	//should work  fine. 
	CPPUNIT_ASSERT( StatusUtil::createHealthStatusItem("health-item-2") == giapi::status::OK );
}



void GiapiStatusTest::testPostStatusItem() {
	
	//Value has not been set. Not posting, issue warning
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item") == giapi::status::WARNING);
}
