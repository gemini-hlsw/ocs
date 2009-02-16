#ifndef GIAPICOMMANDSTEST_H_
#define GIAPICOMMANDSTEST_H_
#include <giapi/SequenceCommandHandler.h>

#include <cppunit/extensions/HelperMacros.h>

namespace giapi {

class GiapiCommandsTest : public CppUnit::TestFixture {

	CPPUNIT_TEST_SUITE( GiapiCommandsTest );
	CPPUNIT_TEST(testAddHandler);
	CPPUNIT_TEST(testAddApplyHandler);
	CPPUNIT_TEST(testPostCompletionInfo);
	CPPUNIT_TEST_SUITE_END();

public:

	void setUp();

	void tearDown();

	void testAddHandler();
	void testAddApplyHandler();
	void testPostCompletionInfo();

	GiapiCommandsTest();
	virtual ~GiapiCommandsTest();
private:
	giapi::pSequenceCommandHandler _handler;
};

}

class MyHandler: public giapi::SequenceCommandHandler {
public:
	giapi::pHandlerResponse handle(giapi::command::ActionId id,
				giapi::command::SequenceCommand sequenceCommand,
				giapi::command::Activity activity,
				giapi::pConfiguration config);
	static giapi::pSequenceCommandHandler create();
	virtual ~MyHandler();
private:
	MyHandler();
};




#endif /*GIAPICOMMANDSTEST_H_*/
