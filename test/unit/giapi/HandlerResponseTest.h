/*
 * HandlerResponseTest.h
 *
 *  Created on: Dec 23, 2008
 *      Author: anunez
 */

#ifndef HANDLERRESPONSETEST_H_
#define HANDLERRESPONSETEST_H_

#include <cppunit/extensions/HelperMacros.h>

namespace giapi {

class HandlerResponseTest : public CppUnit::TestFixture {
	CPPUNIT_TEST_SUITE( HandlerResponseTest );
	CPPUNIT_TEST(testConstructHandler);
	CPPUNIT_TEST(testCopyConstructor);
	CPPUNIT_TEST(testAssignment);
	CPPUNIT_TEST_SUITE_END();

public:


	void setUp();

	void tearDown();

	void testConstructHandler();
	void testCopyConstructor();
	void testAssignment();

	HandlerResponseTest();
	virtual ~HandlerResponseTest();
};
}
#endif /* HANDLERRESPONSETEST_H_ */


