#include "GiapiCommandsTest.h"

#include <giapi/giapi.h>
#include <giapi/CommandUtil.h>

namespace giapi {

GiapiCommandsTest::GiapiCommandsTest() {
}

GiapiCommandsTest::~GiapiCommandsTest() {
}

void GiapiCommandsTest::setUp() {
	_handler = MyHandler::create();
}

void GiapiCommandsTest::tearDown() {

}

void GiapiCommandsTest::testAddHandler() {
	//Add a handler to the DATUM sequence command
	CPPUNIT_ASSERT(CommandUtil::subscribeSequenceCommand(command::DATUM, command::SET_PRESET_START, _handler) == giapi::status::OK);

	pSequenceCommandHandler handler;
	//Add an uninitialized handler to the REBOOT sequence command
	CPPUNIT_ASSERT(CommandUtil::subscribeSequenceCommand(command::REBOOT, command::SET_PRESET_START, handler) == giapi::status::ERROR);
}

void GiapiCommandsTest::testAddApplyHandler() {
	//Add a handler to the DATUM sequence command
	CPPUNIT_ASSERT(CommandUtil::subscribeApply("inst:dc", command::SET_PRESET_START, _handler) == giapi::status::OK);
	pSequenceCommandHandler handler;
	//Add an uninitialized handler to the REBOOT sequence command
	CPPUNIT_ASSERT(CommandUtil::subscribeApply("inst:cc", command::SET_PRESET_START, handler) == giapi::status::ERROR);
}

void GiapiCommandsTest::testPostCompletionInfo() {
	command::ActionId action = 1;
	pHandlerResponse response = HandlerResponse::create(HandlerResponse::COMPLETED);
	//Post completion info. This should work
	CPPUNIT_ASSERT(CommandUtil::postCompletionInfo(action, response) == giapi::status::OK);
	pHandlerResponse response2;
	//Post completion info. This should not work, the answer is not initialized
	CPPUNIT_ASSERT(CommandUtil::postCompletionInfo(action, response2) == giapi::status::ERROR);
	response2 = HandlerResponse::create(HandlerResponse::STARTED);
	//Post completion info. This should not work, the answer is not valid
	CPPUNIT_ASSERT(CommandUtil::postCompletionInfo(action, response2) == giapi::status::ERROR);
	response2 = HandlerResponse::create(HandlerResponse::ERROR);
	//Post completion info. This should not work, Error response without message
	CPPUNIT_ASSERT(CommandUtil::postCompletionInfo(action, response2) == giapi::status::ERROR);
}
}


///// Sequence Command Handler for testing

using namespace giapi;

MyHandler::MyHandler() {

}

MyHandler::~MyHandler() {

}

giapi::pHandlerResponse MyHandler::handle(giapi::command::ActionId id,
				giapi::command::SequenceCommand sequenceCommand,
				giapi::command::Activity activity,
				giapi::pConfiguration config) {
	return HandlerResponse::create(HandlerResponse::ACCEPTED);
}

giapi::pSequenceCommandHandler MyHandler::create() {
	pSequenceCommandHandler handler(new MyHandler());
	return handler;
}
