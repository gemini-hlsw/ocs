#include "JmsCommandUtil.h"
#include "LogCommandUtil.h"
#include <gmp/JmsUtil.h>


namespace giapi {

log4cxx::LoggerPtr JmsCommandUtil::logger(log4cxx::Logger::getLogger("giapi.JmsCommandUtil"));

pJmsCommandUtil JmsCommandUtil::INSTANCE(static_cast<JmsCommandUtil *>(0));

JmsCommandUtil::JmsCommandUtil() throw (CommunicationException) {
	_completionInfoProducer = gmp::CompletionInfoProducer::create();
}

JmsCommandUtil::~JmsCommandUtil() {
	LOG4CXX_DEBUG(logger, "Destroying Jms Command Util Service");

	//destroy all the activity holders
	CommandHolderMap :: const_iterator it;
	for (it = _commandHolderMap.begin(); it != _commandHolderMap.end(); it++ ) {
		ActivityHolder * tmp = (*it).second;
		if (tmp != NULL) {
			delete tmp;
		}
	}
}

pJmsCommandUtil JmsCommandUtil::Instance() throw (CommunicationException){
	if (INSTANCE.get() == 0) {
		INSTANCE.reset(new JmsCommandUtil());
	}
	return INSTANCE;
}

int JmsCommandUtil::subscribeApply(const std::string & prefix,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {

	if (LogCommandUtil::Instance()->subscribeApply(prefix, activities, handler)
			!= giapi::status::ERROR) {
		//Create a consumer for this prefix and activities
		pSequenceCommandConsumer consumer =
			SequenceCommandConsumer::create(prefix, activities, handler);

		//store this consumer....
		ActivityHolder * holder = _commandHolderMap[ gmp::JmsUtil::getTopic(prefix)];
		if (holder == NULL) {
			holder = new ActivityHolder();
			_commandHolderMap[ gmp::JmsUtil::getTopic(prefix) ]  = holder;
		}
		holder->registerConsumer(activities, consumer);
		return giapi::status::OK;
	}

	return giapi::status::ERROR;
}

int JmsCommandUtil::subscribeSequenceCommand(command::SequenceCommand id,
		command::ActivitySet activities,
		pSequenceCommandHandler handler) throw (CommunicationException) {

	if (LogCommandUtil::Instance()->subscribeSequenceCommand(id, activities, handler)
			!= giapi::status::ERROR) {
		//Create a consumer for this sequence commands and activities.
		pSequenceCommandConsumer consumer =
				SequenceCommandConsumer::create(id, activities, handler);
		//Store this consumer for the associated sequence command/activities;
		//use that info to control future registers to the same sequence command
		//and destroy consumers that are no longer in
		//use. If we don't do this, the consumer will be immediately
		//destroyed, since it's a smart pointer, and no references would be
		//held to it.
		ActivityHolder * holder = _commandHolderMap[ gmp::JmsUtil::getTopic(id)];
		if (holder == NULL) {
			holder = new ActivityHolder();
			_commandHolderMap[ gmp::JmsUtil::getTopic(id) ]  = holder;
		}
		holder->registerConsumer(activities, consumer);
		return giapi::status::OK;
	}

	return giapi::status::ERROR;
}

int JmsCommandUtil::postCompletionInfo(command::ActionId id,
		pHandlerResponse response) throw (PostException) {

	if (LogCommandUtil::Instance()->postCompletionInfo(id, response) !=
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
		pSequenceCommandConsumer consumer) {

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
		pSequenceCommandConsumer consumer) {
	//store the new consumer in the map for this activity
	_activityConsumerMap[activity] = consumer;
	//thanks to the magic of the smart pointers, if there was an old consumer,
	//that is not referenced anywhere else, then it will be destroyed
	//automatically. Yeee!!
}
}
