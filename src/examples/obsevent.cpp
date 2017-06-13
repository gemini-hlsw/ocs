/**
 * Example application to send observation events to Gemini
 */

#include <iostream>
#include <signal.h>
#include <stdlib.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/DataUtil.h>
#include <giapi/GiapiUtil.h>

using namespace giapi;

void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

int main(int argc, char **argv) {

	try {

		std::cout << "Starting Observation Event Example" << std::endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

		if (DataUtil::postObservationEvent(data::OBS_PREP, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_START_ACQ, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_END_ACQ, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_START_READOUT, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_END_READOUT, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_START_DSET_WRITE, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_END_DSET_WRITE, "S2009020201-1") == status::ERROR) {
			std::cout << "ERROR posting OBS_PREP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_END_DSET_WRITE, "") == status::ERROR) {
			std::cout << "This is an expected error, the dataset label was empty. No message was sent to GMP" << std::endl;
		}

		if (DataUtil::postObservationEvent(data::OBS_END_DSET_WRITE, " ") == status::ERROR) {
			std::cout << "This is an expected error, the dataset label was empty. No message was sent to GMP" << std::endl;
		}


	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

