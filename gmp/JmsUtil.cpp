#include "JmsUtil.h"
#include "GMPKeys.h"

namespace gmp {

log4cxx::LoggerPtr JmsUtil::logger(log4cxx::Logger::getLogger("gmp:JmsUtil"));

std::auto_ptr<JmsUtil> JmsUtil::INSTANCE(new JmsUtil());

JmsUtil::JmsUtil() {
	JmsUtil::activityMap[GMPKeys::GMP_ACTIVITY_PRESET] = giapi::command::PRESET;
	JmsUtil::activityMap[GMPKeys::GMP_ACTIVITY_START] = giapi::command::START;
	JmsUtil::activityMap[GMPKeys::GMP_ACTIVITY_PRESET_START] = giapi::command::PRESET_START;
	JmsUtil::activityMap[GMPKeys::GMP_ACTIVITY_CANCEL] = giapi::command::CANCEL;
	
	JmsUtil::handlerResponseMap[HandlerResponse::ACCEPTED] = GMPKeys::GMP_HANDLER_RESPONSE_ACCEPTED;
	JmsUtil::handlerResponseMap[HandlerResponse::STARTED] = GMPKeys::GMP_HANDLER_RESPONSE_STARTED;
	JmsUtil::handlerResponseMap[HandlerResponse::COMPLETED] = GMPKeys::GMP_HANDLER_RESPONSE_COMPLETED;
	JmsUtil::handlerResponseMap[HandlerResponse::ERROR] = GMPKeys::GMP_HANDLER_RESPONSE_ERROR;
	
	JmsUtil::sequenceCommandMap[command::TEST] = GMPKeys::GMP_SEQUENCE_COMMAND_TEST;
	JmsUtil::sequenceCommandMap[command::REBOOT] = GMPKeys::GMP_SEQUENCE_COMMAND_REBOOT;
	JmsUtil::sequenceCommandMap[command::INIT] = GMPKeys::GMP_SEQUENCE_COMMAND_INIT;
	JmsUtil::sequenceCommandMap[command::DATUM] = GMPKeys::GMP_SEQUENCE_COMMAND_DATUM;
	JmsUtil::sequenceCommandMap[command::PARK] = GMPKeys::GMP_SEQUENCE_COMMAND_PARK;
	JmsUtil::sequenceCommandMap[command::VERIFY] = GMPKeys::GMP_SEQUENCE_COMMAND_VERIFY;
	JmsUtil::sequenceCommandMap[command::END_VERIFY] = GMPKeys::GMP_SEQUENCE_COMMAND_END_VERIFY;
	JmsUtil::sequenceCommandMap[command::GUIDE] = GMPKeys::GMP_SEQUENCE_COMMAND_GUIDE;
	JmsUtil::sequenceCommandMap[command::END_GUIDE] = GMPKeys::GMP_SEQUENCE_COMMAND_END_GUIDE;
	JmsUtil::sequenceCommandMap[command::APPLY] = GMPKeys::GMP_SEQUENCE_COMMAND_APPLY;
	JmsUtil::sequenceCommandMap[command::OBSERVE] = GMPKeys::GMP_SEQUENCE_COMMAND_OBSERVE;
	JmsUtil::sequenceCommandMap[command::END_OBSERVE] = GMPKeys::GMP_SEQUENCE_COMMAND_END_OBSERVE;
	JmsUtil::sequenceCommandMap[command::PAUSE] = GMPKeys::GMP_SEQUENCE_COMMAND_PAUSE;
	JmsUtil::sequenceCommandMap[command::CONTINUE] = GMPKeys::GMP_SEQUENCE_COMMAND_CONTINUE;
	JmsUtil::sequenceCommandMap[command::STOP] = GMPKeys::GMP_SEQUENCE_COMMAND_STOP;
	JmsUtil::sequenceCommandMap[command::ABORT] = GMPKeys::GMP_SEQUENCE_COMMAND_ABORT;
}

JmsUtil::~JmsUtil() {
}

std::string JmsUtil::getTopic(command::SequenceCommand id) {
	return INSTANCE->sequenceCommandMap[id];
}

command::Activity JmsUtil::getActivity(const std::string &id) {
	return (command::Activity)INSTANCE->activityMap[id];
}
	
std::string JmsUtil::getHandlerResponse(pHandlerResponse response) {
	return INSTANCE->handlerResponseMap[response->getResponse()];
}


}
