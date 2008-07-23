#include "JmsCommandUtil.h"
#include "LogCommandUtil.h"

namespace giapi {

log4cxx::LoggerPtr JmsCommandUtil::logger(log4cxx::Logger::getLogger("giapi.JmsCommandUtil"));

std::auto_ptr<JmsCommandUtil> JmsCommandUtil::INSTANCE(new JmsCommandUtil());

JmsCommandUtil::JmsCommandUtil() {

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

JmsCommandUtil& JmsCommandUtil::Instance() {
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
		gmp::SequenceCommandConsumer* consumer =
				new gmp::SequenceCommandConsumer(id, activities, handler);
		//Store this consumer and the associated sequence command/activities; use that info to control
		//future registers to the same sequence command and destroy consumers that are no longer in
		//use
		storeConsumerPointer(consumer, id, activities);

		return giapi::status::OK;
	}

	return giapi::status::ERROR;
}

void JmsCommandUtil::storeConsumerPointer(
		gmp::SequenceCommandConsumer * consumer,
		command::SequenceCommand sequenceCommand, command::ActivitySet set) {
	ActivityHolder * holder = _commandHolderMap[sequenceCommand];
	holder->registerConsumer(set, consumer);
}

gmp::SequenceCommandConsumer * ActivityHolder::getConsumer(
		command::Activity activity) {
	return _activityConsumerMap[activity];
}


////////////////////// ActivityHolder implementation ////////////////////////////

log4cxx::LoggerPtr ActivityHolder::logger(log4cxx::Logger::getLogger("giapi.ActivityHolder"));

ActivityHolder::ActivityHolder() {

}

ActivityHolder::~ActivityHolder() {

}

void ActivityHolder::unregisterConsumer(command::Activity activity) {
	_activityConsumerMap[activity] = NULL;
}

void ActivityHolder::registerConsumer(command::ActivitySet set,
		gmp::SequenceCommandConsumer * consumer) {

	//Decompose the set in the actual activities it involves.
	//TODO: We might want to improve the handling of PRESET_START, 
	//so if already register to PRESET or START, it could de-register those handlers...
	//Likewise, if already registered for PRESET_START, and then a register to PRESET happens, 
	//the system might remove the handler of PRESET_START altogheter, or move it to START alone. 
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
		gmp::SequenceCommandConsumer * consumer) {
	//first, let's see if there is a consumer already registered, and save it 
	gmp::SequenceCommandConsumer * oldConsumer = getConsumer(activity);
	//store the new consumer in the map for this activity
	_activityConsumerMap[activity] = consumer;

	if (oldConsumer == NULL) {
		//we are cool, nothing has been registered before. 
		return;
	}

	//if we are here, there is an old consumer. Let's see if we can destroy it. 
	//We can destroy it if it's not used in any other of the activities. 
	if (sameConsumer(command::PRESET, oldConsumer)) {
		return;
	}
	if (sameConsumer(command::START, oldConsumer)) {
		return;
	}
	if (sameConsumer(command::PRESET_START, oldConsumer)) {
		return;
	}
	if (sameConsumer(command::CANCEL, oldConsumer)) {
		return;
	}

	delete oldConsumer;

}

bool ActivityHolder::sameConsumer(command::Activity activity,
		gmp::SequenceCommandConsumer * consumer) {

	if (consumer == getConsumer(activity)) {
		return true; // the same consumer is stored. 
	}
	return false;

}

}
