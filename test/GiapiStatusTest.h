#ifndef GIAPISTATUSTEST_H_
#define GIAPISTATUSTEST_H_


#include <cppunit/extensions/HelperMacros.h>

class GiapiStatusTest : public CppUnit::TestFixture {
	
	CPPUNIT_TEST_SUITE( GiapiStatusTest );
	CPPUNIT_TEST( testCreateStatusItem );
	CPPUNIT_TEST( testCreateAlarmStatusItem );
	CPPUNIT_TEST(testPostStatusItem);
	CPPUNIT_TEST_SUITE_END();
	
	

public:
	void setUp();
	
	void tearDown();
	
	void testCreateStatusItem();
	void testCreateAlarmStatusItem();
	
	void testPostStatusItem();
	
	virtual ~GiapiStatusTest();
	
};


#endif /*GIAPISTATUSTEST_H_*/
