/**
 * Example program to subscribe and process a sequence command
 */

#include <iostream>
#include <signal.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/CommandUtil.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/giapi.h>
#include "ApplyHandler.h"


using namespace giapi;

int main(int argc, char **argv) {

	try {

		std::cout << "Starting Apply Sequence Command Example" << std::endl;

		decaf::util::concurrent::CountDownLatch lock(1);

		pSequenceCommandHandler handler = ApplyHandler::create();

		CommandUtil::subscribeApply("gpi", command::SET_PRESET_START, handler);

		//Wait until is killed
		lock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?... Exiting" << std::endl;

	}
	return 0;
}

