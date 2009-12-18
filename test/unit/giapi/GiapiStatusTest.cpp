
#include "GiapiStatusTest.h"
#include <giapi/giapi.h>
#include <giapi/StatusUtil.h>
using namespace giapi;

namespace giapi {

GiapiStatusTest::~GiapiStatusTest() {

}

void GiapiStatusTest::setUp() {
	//Initialize a few status items
	StatusUtil::createStatusItem("test-item", giapi::type::INT);
	StatusUtil::createStatusItem("test-item-double", giapi::type::DOUBLE);
	StatusUtil::createStatusItem("test-item-float", giapi::type::FLOAT);
	StatusUtil::createAlarmStatusItem("alarm-item", giapi::type::DOUBLE);
	StatusUtil::createHealthStatusItem("health-item");

	//Set a few values there
	StatusUtil::setValueAsInt("test-item", 37);
	StatusUtil::setValueAsDouble("test-item-double", 56.92);
	StatusUtil::setValueAsFloat("test-item-float", (float)31.5);
	StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_WARNING,
			giapi::alarm::ALARM_CAUSE_HIHI);
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
	//OK. Value didn't change since last post
	CPPUNIT_ASSERT(StatusUtil::setValueAsInt("test-item", 48) == giapi::status::OK);

	//should work. test-item-double is a double
	CPPUNIT_ASSERT( StatusUtil::setValueAsDouble("test-item-double", 335.293) == giapi::status::OK );
	//should work. no changes since last time
	CPPUNIT_ASSERT( StatusUtil::setValueAsDouble("test-item-double", 335.293) == giapi::status::OK );

	//should work. test-item-float is a float
	CPPUNIT_ASSERT( StatusUtil::setValueAsFloat("test-item-float", 16.75f) == giapi::status::OK );
	//should work. no changes since last time
	CPPUNIT_ASSERT( StatusUtil::setValueAsFloat("test-item-float", 16.75f) == giapi::status::OK );

	//should not work. test-item-float is a float
	CPPUNIT_ASSERT( StatusUtil::setValueAsInt("test-item-float", 16) == giapi::status::ERROR );
	//should not work. test-item-float is a float
	CPPUNIT_ASSERT( StatusUtil::setValueAsDouble("test-item-float", 16.75) == giapi::status::ERROR );



}

void GiapiStatusTest::testSetValuesAlarms() {

	//should work.
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_HI) == giapi::status::OK );
	//shouldn't work. alarm-item-3 doesn't exists
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item-3", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_HIHI) == giapi::status::ERROR);
	//shouldn't work. test-item is not an alarm
	CPPUNIT_ASSERT( StatusUtil::setAlarm("test-item", giapi::alarm::ALARM_WARNING, giapi::alarm::ALARM_CAUSE_HIHI) == giapi::status::ERROR );
	//OK. Value didn't change since last post
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_HI) == giapi::status::OK);

}

void GiapiStatusTest::testSetValuesAlarmsOtherCause() {
	//should work. The cause is other an a message is set.
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER, "Error Message") == giapi::status::OK);
	//shouldn't work. There is not message in for the cause "other"
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER) == giapi::status::ERROR );
	//OK. Value didn't change in an alarm. Comparing the error message in this case
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_OTHER, "Error Message") == giapi::status::OK);
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
	//clearing the alarm state. OK since the alarm is already cleared since the last post operation.
	CPPUNIT_ASSERT( StatusUtil::clearAlarm("alarm-item") == giapi::status::OK);
}

void GiapiStatusTest::testSetValuesHealth() {

	//should work.
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item", giapi::health::BAD) == giapi::status::OK);
	//shouldn't work. health-item-3 doesn't exists
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item-3", giapi::health::BAD) == giapi::status::ERROR );
	//shouldn't work. test-item is not a Health Status Item
	CPPUNIT_ASSERT( StatusUtil::setHealth("test-item", giapi::health::BAD) == giapi::status::ERROR );
	//OK. Value didn't change
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item", giapi::health::BAD) == giapi::status::OK);
}

void GiapiStatusTest::testPostStatusItem() {
	//Value has been set. This should be OK
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item") == giapi::status::OK);
	//Value has not been set. Not posting, return OK immediately
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item-2") == giapi::status::OK);
	//This is an error. No such status item
	CPPUNIT_ASSERT( StatusUtil::postStatus("test-item-3") == giapi::status::ERROR);
}

void GiapiStatusTest::testPostAlarms() {
	//Value has been set. This should be OK
	CPPUNIT_ASSERT( StatusUtil::postStatus("alarm-item") == giapi::status::OK);
	//Value has not been set. Not posting, return OK immediately
	CPPUNIT_ASSERT( StatusUtil::postStatus("alarm-item-2") == giapi::status::OK);
	//This is an error. No such status item
	CPPUNIT_ASSERT( StatusUtil::postStatus("alarm-item-3") == giapi::status::ERROR);
}

void GiapiStatusTest::testPostHealth() {
	//Value has been set. This should be OK
	CPPUNIT_ASSERT( StatusUtil::postStatus("health-item") == giapi::status::OK);
	//Value has not been set. Not posting, return OK immediately
	CPPUNIT_ASSERT( StatusUtil::postStatus("health-item-2") == giapi::status::OK);
	//This is an error. No such status item
	CPPUNIT_ASSERT( StatusUtil::postStatus("health-item-3") == giapi::status::ERROR);
}

void GiapiStatusTest::testPostAll() {
	//will mark a few items dirty first
	CPPUNIT_ASSERT( StatusUtil::setValueAsInt("test-item", 99) == giapi::status::OK);
	CPPUNIT_ASSERT( StatusUtil::setHealth("health-item", giapi::health::WARNING) == giapi::status::OK);
	CPPUNIT_ASSERT( StatusUtil::setAlarm("alarm-item", giapi::alarm::ALARM_FAILURE, giapi::alarm::ALARM_CAUSE_LO) == giapi::status::OK);

	//All the pending values should be posted
	CPPUNIT_ASSERT( StatusUtil::postStatus() == giapi::status::OK);
	//Ok too, but this shouldn't post anything
	CPPUNIT_ASSERT( StatusUtil::postStatus() == giapi::status::OK);

}

}

