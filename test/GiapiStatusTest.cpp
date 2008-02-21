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
	
	//Set a few values there
	StatusUtil::setValueAsInt("test-item", 37);
	StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_WARNING, giapi::alarm::ALARM_CAUSE_HIHI);
	StatusUtil::setHealth("health-item", giapi::health::WARNING);
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

void GiapiStatusTest::testSetValuesStatusItem() {
	//should work. test-item is an int
	CPPUNIT_ASSERT( StatusUtil::setValueAsInt("test-item", 48) == giapi::status::OK );
	//shouldn't work. test-item-3 doesn't exists
	CPPUNIT_ASSERT(StatusUtil::setValueAsInt("test-item-3", 48) == giapi::status::ERROR);
	//shouldn't work. test-item is not string
	CPPUNIT_ASSERT(StatusUtil::setValueAsString("test-item", "Value") == giapi::status::ERROR);
	//warning. Value didn't change since last post
	CPPUNIT_ASSERT(StatusUtil::setValueAsInt("test-item", 48) == giapi::status::WARNING);
}


void GiapiStatusTest::testSetValuesAlarms() {

	//should work. 
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_HI) == giapi::status::OK );
	//shouldn't work. alarm-item-3 doesn't exists
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item-3", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_HIHI)  == giapi::status::ERROR);
	//shouldn't work. test-item is not an alarm
	CPPUNIT_ASSERT( StatusUtil::setAlarm("test-item", giapi::alarm::ALARM_WARNING, giapi::alarm::ALARM_CAUSE_HIHI)  == giapi::status::ERROR );
	//warning. Value didn't change since last post
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_HI) == giapi::status::WARNING);

}

void GiapiStatusTest::testSetValuesAlarmsOtherCause() {
	//should work. The cause is other an a message is set.
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER, "Error Message") == giapi::status::OK);
	//shouldn't work. There is not message in for the cause "other"
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER) == giapi::status::ERROR );
	//warning. Value didn't change in an alarm. Comparing the error message in this case
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER, "Error Message") == giapi::status::WARNING);
	//should work. The error message is different
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER, "ERROR Message") == giapi::status::OK);
}

void GiapiStatusTest::testClearAlarms() {
	//clearing the alarm state. Should work.
	CPPUNIT_ASSERT( StatusUtil::clearAlarm("alarm-item") == giapi::status::OK);
	//clearing the alarm state. Error since the alarm item doesn't exist.
	CPPUNIT_ASSERT( StatusUtil::clearAlarm("alarm-item-3") == giapi::status::ERROR);
	//clearing the alarm state. Error since the alarm item is not an alarm
	CPPUNIT_ASSERT( StatusUtil::clearAlarm("test-item") == giapi::status::ERROR);
	//clearing the alarm state. Warning since the alarm is already cleared since the last post operation. 
	CPPUNIT_ASSERT( StatusUtil::clearAlarm("alarm-item") == giapi::status::WARNING);
}

void GiapiStatusTest::testSetValuesHealth() {
	
	//should work. 
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item", giapi::health::BAD) == giapi::status::OK) ;
	//shouldn't work. health-item-3 doesn't exists
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item-3", giapi::health::BAD) == giapi::status::ERROR );
	//shouldn't work. test-item is not a Health Status Item
	CPPUNIT_ASSERT( StatusUtil::setHealth("test-item", giapi::health::BAD) == giapi::status::ERROR );
	//warning. Value didn't change
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item", giapi::health::BAD) == giapi::status::WARNING);
}



void GiapiStatusTest::testPostStatusItem() {
	//Value has been set. This should be OK
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item") == giapi::status::OK);
	//Value has not been set. Not posting, issue warning
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item-2") == giapi::status::WARNING);
	//This is an error. No such status item
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item-3") == giapi::status::ERROR);
}


void GiapiStatusTest::testPostAlarms() {
	//Value has been set. This should be OK
	CPPUNIT_ASSERT( StatusUtil::postStatus("alarm-item") == giapi::status::OK);
	//Value has not been set. Not posting, issue warning
	CPPUNIT_ASSERT( StatusUtil::postStatus("alarm-item-2") == giapi::status::WARNING);
	//This is an error. No such status item
	CPPUNIT_ASSERT( StatusUtil::postStatus("alarm-item-3") == giapi::status::ERROR);
}

void GiapiStatusTest::testPostHealth() {
	//Value has been set. This should be OK
	CPPUNIT_ASSERT( StatusUtil::postStatus("health-item") == giapi::status::OK);
	//Value has not been set. Not posting, issue warning
	CPPUNIT_ASSERT( StatusUtil::postStatus("health-item-2") == giapi::status::WARNING);
	//This is an error. No such status item
	CPPUNIT_ASSERT( StatusUtil::postStatus("health-item-3") == giapi::status::ERROR);
}




