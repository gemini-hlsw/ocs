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
#include "EngineeringHandler.h"

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

	try {

		std::cout << "Starting Accepting Engineering Commands Example"
				<< std::endl;


        pSequenceCommandHandler handler = EngineeringHandler::create();
		CommandUtil::subscribeSequenceCommand(ENGINEERING, SET_PRESET_START_CANCEL, handler);

		//Wait until is killed
		endLock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?... Exiting" << std::endl;

	}
	return 0;
}

