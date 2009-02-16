#ifndef GIAPISTATUSTEST_H_
#define GIAPISTATUSTEST_H_


#include <cppunit/extensions/HelperMacros.h>

namespace giapi {

class GiapiStatusTest : public CppUnit::TestFixture {

	CPPUNIT_TEST_SUITE( GiapiStatusTest );

	CPPUNIT_TEST( testCreateStatusItem );
	CPPUNIT_TEST( testCreateAlarmStatusItem );
	CPPUNIT_TEST( testCreateHealthStatusItem );

	CPPUNIT_TEST(testSetValuesStatusItem);
	CPPUNIT_TEST(testSetValuesAlarms);
	CPPUNIT_TEST(testSetValuesHealth);

	CPPUNIT_TEST(testSetValuesAlarmsOtherCause);
	CPPUNIT_TEST(testClearAlarms);

	CPPUNIT_TEST(testPostStatusItem);
	CPPUNIT_TEST(testPostAlarms);
	CPPUNIT_TEST(testPostHealth);
	CPPUNIT_TEST(testPostAll);

	CPPUNIT_TEST_SUITE_END();



public:
	void setUp();

	void tearDown();

	void testCreateStatusItem();
	void testCreateAlarmStatusItem();
	void testCreateHealthStatusItem();

	void testSetValuesStatusItem();
	void testSetValuesAlarms();
	void testSetValuesHealth();

	void testSetValuesAlarmsOtherCause();
	void testClearAlarms();

	void testPostStatusItem();
	void testPostAlarms();
	void testPostHealth();
	void testPostAll();

	virtual ~GiapiStatusTest();

};

}
#endif /*GIAPISTATUSTEST_H_*/
