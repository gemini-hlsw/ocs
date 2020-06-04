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

decaf::util::concurrent::CountDownLatch endLock(1);

void terminate(int signal) {
    std::cout << "Exiting... " << std::endl;
    endLock.countDown();
    //exit(0);
}

int main(int argc, char **argv) {

    signal(SIGABRT, terminate);
    signal(SIGTERM, terminate);
    signal(SIGINT, terminate);

	// Set of Commands that use the standard handler
	multimap<SequenceCommand, ActivitySet> commands_set;
	commands_set.insert(std::make_pair(TEST, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(INIT, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(DATUM, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(PARK, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(VERIFY, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(END_VERIFY, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(GUIDE, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(END_GUIDE, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(END_OBSERVE, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(PAUSE, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(CONTINUE, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(STOP, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(STOP_CYCLE, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(ABORT, SET_PRESET_START_CANCEL));
	commands_set.insert(std::make_pair(OBSERVE, SET_PRESET_START_CANCEL));

	try {

		std::cout << "Starting Accepting Any Sequence Commands Example"
				<< std::endl;


		for (multimap<SequenceCommand, ActivitySet>::iterator it =
				commands_set.begin(); it
				!= commands_set.end(); ++it) {
			pSequenceCommandHandler handler = GenericHandler::create();
			CommandUtil::subscribeSequenceCommand((*it).first, (*it).second,
					handler);
		}

		pSequenceCommandHandler handler = GenericHandler::create();
		CommandUtil::subscribeApply("gpi", SET_PRESET_START_CANCEL, handler);

		//Wait until is killed
		endLock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?... Exiting" << std::endl;

	}
	return 0;
}

