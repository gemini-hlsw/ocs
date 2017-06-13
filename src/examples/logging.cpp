/*
 * logging.cpp
 *
 *  This is an example file to show how to send
 *  system logging information through the GIAPI
 */


#include <iostream>
#include <stdlib.h>
#include <signal.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/giapi.h>
#include <giapi/GiapiErrorHandler.h>
#include <giapi/ServicesUtil.h>
#include <giapi/GiapiUtil.h>

using namespace giapi;

log::Level & operator++ (log::Level & level) {
	return level = (level == log::Level_end) ? log::Level_begin : log::Level(level + 1);
}


void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

int main(int argc, char **argv) {

	try {

		std::cout << "Starting Logging Example - 10 Log messages for each level" << std::endl;

		decaf::util::concurrent::CountDownLatch lock(1);
		int nMsg = 10;

		for (log::Level level = log::Level_begin; level != log::Level_end; ++level) {
			for (int i = 1; i <= nMsg; i++) {
				std::stringstream stream;
				stream << "Log Message (" << i <<  ")";
				ServicesUtil::systemLog(level, stream.str());
				lock.await(250);
			}
		}


		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);


	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}


