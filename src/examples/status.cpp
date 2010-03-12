/**
 * Example application to send status updates to Gemini
 */

#include <iostream>
#include <signal.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/StatusUtil.h>
#include <giapi/GiapiUtil.h>


using namespace giapi;



void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

int main(int argc, char **argv) {

	try {

		std::cout << "Starting Status Example" << std::endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);


		decaf::util::concurrent::CountDownLatch lock(1);

		StatusUtil::createStatusItem("gpi:status1", type::INT);

		StatusUtil::createStatusItem("gpi:status2", type::INT);

		for (int i = 0; i < 100000; i++) {
			StatusUtil::setValueAsInt("gpi:status1", i);
			StatusUtil::setValueAsInt("gpi:status2", 100000-i);
			StatusUtil::postStatus();
		}


	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

