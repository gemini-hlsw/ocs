#include "JmsCommandUtil.h"
#include "LogCommandUtil.h"

namespace giapi {

log4cxx::LoggerPtr JmsCommandUtil::logger(log4cxx::Logger::getLogger("giapi.JmsCommandUtil"));

std::auto_ptr<JmsCommandUtil> JmsCommandUtil::INSTANCE(0);

JmsCommandUtil::JmsCommandUtil() throw (CommunicationException) {
	_completionInfoProducer = gmp::CompletionInfoProducer::create();
	_commandHolderMap[command::TEST] = new ActivityHolder();
	_commandHolderMap[command::REBOOT] = new ActivityHolder();
	_commandHolderMap[command::INIT] = new ActivityHolder();
	_commandHolderMap[command::DATUM] = new ActivityHolder();
	_commandHolderMap[command::PARK] = new ActivityHolder();
	_commandHolderMap[command::VERIFY] = new ActivityHolder();
	_commandHolderMap[command::END_VERIFY] = new ActivityHolder();
	_commandHolderMap[command::GUIDE] = new ActivityHolder();
	_commandHolderMap[command::END_GUIDE] = new ActivityHolder();
	_commandHolderMap[command::APPLY] = new ActivityHolder();
	_commandHolderMap[command::OBSERVE] = new ActivityHolder();
	_commandHolderMap[command::END_OBSERVE] = new ActivityHolder();
	_commandHolderMap[command::PAUSE] = new ActivityHolder();
	_commandHolderMap[command::CONTINUE] = new ActivityHolder();
	_commandHolderMap[command::STOP] = new ActivityHolder();
	_commandHolderMap[command::ABORT] = new ActivityHolder();
}

JmsCommandUtil::~JmsCommandUtil() {
	LOG4CXX_DEBUG(logger, "Destroying JmsCommandUtil");
	delete _commandHolderMap[command::TEST];
	delete _commandHolderMap[command::REBOOT];
	delete _commandHolderMap[command::INIT];
	delete _commandHolderMap[command::DATUM];
	delete _commandHolderMap[command::PARK];
	delete _commandHolderMap[command::VERIFY];
	delete _commandHolderMap[command::END_VERIFY];
	delete _commandHolderMap[command::GUIDE];
	delete _commandHolderMap[command::END_GUIDE];
	delete _commandHolderMap[command::APPLY];
	delete _commandHolderMap[command::OBSERVE];
	delete _commandHolderMap[command::END_OBSERVE];
	delete _commandHolderMap[command::PAUSE];
	delete _commandHolderMap[command::CONTINUE];
	delete _commandHolderMap[command::STOP];
	delete _commandHolderMap[command::ABORT];
}

JmsCommandUtil& JmsCommandUtil::Instance() throw (CommunicationException){
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new JmsCommandUtil());
	}
	return *INSTANCE;
}

int JmsCommandUtil::subscribeApply(const char * prefix,
		command::ActivitySet activities, pSequenceCommandHandler handler) {
	LOG4CXX_INFO(logger, "subscribeApply method not implemented yet." << prefix);
	return giapi::status::ERROR;
}

int JmsCommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::ActivitySet activities, pSequenceCommandHandler handler) {

	if (LogCommandUtil::Instance().subscribeSequenceCommand(id, activities, handler)
			!= giapi::status::ERROR) {
		//Create a consumer for this sequence commands and activities.
		gmp::pSequenceCommandConsumer consumer =
				gmp::SequenceCommandConsumer::create(id, activities, handler);
		//Store this consumer for the associated sequence command/activities;
		//use that info to control future registers to the same sequence command
		//and destroy consumers that are no longer in
		//use. If we don't do this, the consumer will be automatically
		//destroyed, since it's a smart pointer, and no references would be
		//held to it.
		ActivityHolder * holder = _commandHolderMap[id];
		if (holder != NULL) {
			holder->registerConsumer(activities, consumer);
		}
		return giapi::status::OK;
	}

	return giapi::status::ERROR;
}

int JmsCommandUtil::postCompletionInfo(command::ActionId id,
		pHandlerResponse response) throw (PostException) {

	if (LogCommandUtil::Instance().postCompletionInfo(id, response) !=
		giapi::status::ERROR) {
		return _completionInfoProducer->postCompletionInfo(id, response);
	}
	return giapi::status::ERROR;

}

////////////////////// ActivityHolder implementation ////////////////////////////

log4cxx::LoggerPtr ActivityHolder::logger(log4cxx::Logger::getLogger("giapi.ActivityHolder"));

ActivityHolder::ActivityHolder() {
}

ActivityHolder::~ActivityHolder() {
	LOG4CXX_DEBUG(logger, "Destroying ActivityHolder for sequence command");
	_activityConsumerMap.clear();
}

void ActivityHolder::registerConsumer(command::ActivitySet set,
		gmp::pSequenceCommandConsumer consumer) {

	//Decompose the set in the actual activities it involves. Store a
	//reference of the consumer in each activity that is represented in the set
	//TODO: We might want to improve the handling of PRESET_START,
	//so if already register to PRESET or START, it could de-register those handlers...
	//Likewise, if already registered for PRESET_START, and then a register to PRESET happens,
	//the system might remove the handler of PRESET_START altogether, or move it to START alone.
	switch (set) {
	case command::SET_PRESET:
		registerConsumer(command::PRESET, consumer);
		break;
	case command::SET_START:
		registerConsumer(command::START, consumer);
		break;
	case command::SET_PRESET_START:
		registerConsumer(command::PRESET_START, consumer);
		break;
	case command::SET_CANCEL:
		registerConsumer(command::CANCEL, consumer);
		break;
	case command::SET_PRESET_CANCEL:
		registerConsumer(command::PRESET, consumer);
		registerConsumer(command::CANCEL, consumer);
		break;
	case command::SET_START_CANCEL:
		registerConsumer(command::START, consumer);
		registerConsumer(command::CANCEL, consumer);
		break;
	case command::SET_PRESET_START_CANCEL:
		registerConsumer(command::PRESET, consumer);
		registerConsumer(command::CANCEL, consumer);
		registerConsumer(command::START, consumer);
		break;
	}
}

void ActivityHolder::registerConsumer(command::Activity activity,
		gmp::pSequenceCommandConsumer consumer) {
	//store the new consumer in the map for this activity
	_activityConsumerMap[activity] = consumer;
	//thanks to the magic of the smart pointers, if there was an old consumer,
	//that is not referenced anywhere else, then it will be destroyed
	//automatically. Yeee!!
}
}
