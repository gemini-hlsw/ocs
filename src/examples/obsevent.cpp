/**
 * Example application to send observation events to Gemini
 */

#include <iostream>
#include <signal.h>

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

		decaf::util::concurrent::CountDownLatch lock(1);

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

		DataUtil::postObservationEvent(data::OBS_PREP, "S2009020201-1");
		DataUtil::postObservationEvent(data::OBS_START_ACQ, "S2009020201-1");
		DataUtil::postObservationEvent(data::OBS_END_ACQ, "S2009020201-1");
		DataUtil::postObservationEvent(data::OBS_START_READOUT, "S2009020201-1");
		DataUtil::postObservationEvent(data::OBS_END_READOUT, "S2009020201-1");
		DataUtil::postObservationEvent(data::OBS_START_DSET_WRITE, "S2009020201-1");
		DataUtil::postObservationEvent(data::OBS_END_DSET_WRITE, "S2009020201-1");

		//Wait until is killed
		lock.await();
	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

