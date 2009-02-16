/*
 * HandlerResponseTest.cpp
 *
 *  Created on: Dec 23, 2008
 *      Author: anunez
 */

#include <giapi/HandlerResponse.h>

#include "HandlerResponseTest.h"

namespace giapi {

HandlerResponseTest::HandlerResponseTest() {

}

HandlerResponseTest::~HandlerResponseTest() {
}

void HandlerResponseTest::setUp() {

}

void HandlerResponseTest::tearDown() {

}

void HandlerResponseTest::testConstructHandler() {

	pHandlerResponse response = HandlerResponse::create(HandlerResponse::ACCEPTED);
	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::ACCEPTED);

	response = HandlerResponse::create(HandlerResponse::COMPLETED);
	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::COMPLETED);

	response = HandlerResponse::create(HandlerResponse::STARTED);
	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::STARTED);

	response = HandlerResponse::createError("Example Message");
	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::ERROR);
	CPPUNIT_ASSERT(response->getMessage() == "Example Message");

	char * msg = new char[16];
	strcpy(msg, "Another Message");
	response = HandlerResponse::createError(msg);
	//delete the original message
	delete msg;
	//the error message shouldn't be affected
	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::ERROR);
	CPPUNIT_ASSERT(response->getMessage() == "Another Message");

}

void HandlerResponseTest::testCopyConstructor() {

	pHandlerResponse response = HandlerResponse::createError("DEMO");

	//Construct a copy of the current response by copy constructor
	pHandlerResponse otherResponse(response);

	//now replace the original response
	response = HandlerResponse::create(HandlerResponse::ACCEPTED);

	//and check everything is consistent
	CPPUNIT_ASSERT(otherResponse->getResponse() == HandlerResponse::ERROR);
	CPPUNIT_ASSERT(otherResponse->getMessage() == "DEMO");

	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::ACCEPTED);
	CPPUNIT_ASSERT(response->getMessage().empty());
}

void HandlerResponseTest::testAssignment() {
	pHandlerResponse response = HandlerResponse::createError("DEMO_2_AND_VERY_LONG_STRING");

	//construct a copy of the current response, by assignment
	pHandlerResponse otherResponse = response;

	//now replace the original response
	response = HandlerResponse::create(HandlerResponse::ACCEPTED);

	//and check everything is consistent
	CPPUNIT_ASSERT(otherResponse->getResponse() == HandlerResponse::ERROR);
	CPPUNIT_ASSERT(otherResponse->getMessage() == "DEMO_2_AND_VERY_LONG_STRING");

	CPPUNIT_ASSERT(response->getResponse() == HandlerResponse::ACCEPTED);
	CPPUNIT_ASSERT(response->getMessage().empty());

}

}
