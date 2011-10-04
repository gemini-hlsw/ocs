/**
 * Example program to subscribe and process a sequence command
 */

#include <iostream>
#include <signal.h>
#include <map>
#include <utility>

#include <decaf/util/concurrent/CountDownLatch.h>
#include <decaf/util/concurrent/TimeUnit.h>


#include <giapi/CommandUtil.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/giapi.h>
#include "ObsGdsHandler.h"

using namespace giapi;
using namespace giapi::command;

decaf::util::concurrent::CountDownLatch endLock(1);

void terminate(int signal) {
    std::cout << "Exiting... " << std::endl;
    endLock.countDown();
    decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
//    exit(0);
}

int main(int argc, char **argv) {

    signal(SIGABRT, terminate);
    signal(SIGTERM, terminate);
    signal(SIGINT, terminate);

	// Set of Commands that use the standard handler

	try {

		std::cout << "Starting Observe Simulator Example"
				<< std::endl;


			pSequenceCommandHandler handler = ObsGdsHandler::create();
			CommandUtil::subscribeSequenceCommand(OBSERVE, SET_PRESET_START_CANCEL,
					handler);


		//Wait until is killed
		endLock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?... Exiting" << std::endl;

	}
	return 0;
}

