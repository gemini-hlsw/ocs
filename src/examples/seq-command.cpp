/**
 * Example program to subscribe and process a sequence command
 */

#include <iostream>
#include <stdio.h>
#include <signal.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/CommandUtil.h>
#include <giapi/SequenceCommandHandler.h>
#include <giapi/giapi.h>
#include "MyHandler.h"


using namespace giapi;

int main(int argc, char **argv) {

	try {

		std::cout << "Starting Sequence Commands Example" << std::endl;

		decaf::util::concurrent::CountDownLatch lock(1);

		pSequenceCommandHandler handler = MyHandler::create();

		CommandUtil::subscribeSequenceCommand(command::PARK, command::SET_PRESET_START, handler);

		//Wait until is killed
		lock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?... Exiting" << std::endl;

	}
	return 0;
}

