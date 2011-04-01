/**
 * Example program to subscribe and process a sequence command
 */

#include <iostream>
#include <signal.h>
#include <map>
#include <utility>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/CommandUtil.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/giapi.h>
#include "GenericHandler.h"
#include "ApplyHandler.h"

using namespace giapi;
using namespace giapi::command;

int main(int argc, char **argv) {

	// Set of Commands that use the standard handler
	multimap<SequenceCommand, ActivitySet> standard_handler;
	standard_handler.insert(std::make_pair(TEST, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(INIT, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(DATUM, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(PARK, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(VERIFY, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(END_VERIFY, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(GUIDE, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(END_GUIDE, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(END_OBSERVE, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(PAUSE, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(CONTINUE, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(STOP, SET_PRESET_START_CANCEL));
	standard_handler.insert(std::make_pair(ABORT, SET_PRESET_START_CANCEL));

	// Set of Commands that require a configuration
	multimap<SequenceCommand, ActivitySet> with_config_handler;
	//with_config_handler.insert(std::make_pair(REBOOT, SET_PRESET_START_CANCEL));
	with_config_handler.insert(std::make_pair(OBSERVE, SET_PRESET_START_CANCEL));

	try {

		std::cout << "Starting Accepting Any Sequence Commands Example"
				<< std::endl;

		decaf::util::concurrent::CountDownLatch lock(1);

		for (multimap<SequenceCommand, ActivitySet>::iterator it =
				standard_handler.begin(); it
				!= standard_handler.end(); ++it) {
			pSequenceCommandHandler handler = GenericHandler::create();
			CommandUtil::subscribeSequenceCommand((*it).first, (*it).second,
					handler);
		}

		for (multimap<SequenceCommand, ActivitySet>::iterator it =
				with_config_handler.begin(); it
				!= with_config_handler.end(); ++it) {
			// Reuse APPLY handler
			pSequenceCommandHandler handler = ApplyHandler::create();
			CommandUtil::subscribeSequenceCommand((*it).first, (*it).second,
					handler);
		}

		pSequenceCommandHandler handler = ApplyHandler::create();
		CommandUtil::subscribeApply("gpi", SET_PRESET_START_CANCEL, handler);

		//Wait until is killed
		lock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?... Exiting" << std::endl;

	}
	return 0;
}

