/**
 * Example application to send observation events to Gemini
 */

#include <iostream>
#include <fstream>
#include <signal.h>

#include <decaf/util/concurrent/CountDownLatch.h>
#include <decaf/util/concurrent/TimeUnit.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/DataUtil.h>
#include <giapi/ServicesUtil.h>`
#include <giapi/GiapiUtil.h>

using namespace giapi;
using namespace std;

void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

int main(int argc, char **argv) {

	try {

		cout << "Starting Observation Process Example" << endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

		cout << "Retrieving Data file location" << endl;

		string dataLocation = ServicesUtil::getProperty("DHS_SCIENCE_DATA_PATH", 1000);

		cout << "DataSciencePath : " << dataLocation << endl;

		string newLocation(dataLocation);
		newLocation.append("/");
		newLocation.append("S20110427-01.fits");

		cout << "TempFile : " << newLocation << endl;

		ifstream in("S20110427-01.fits", ifstream::binary);
		ofstream out(newLocation.c_str(), ifstream::binary);
		out << in.rdbuf();

		if (DataUtil::postObservationEvent(data::OBS_START_ACQ, "S20110427-01") == status::ERROR) {
			cout << "ERROR posting OBS_START_ACQ" << endl;
		}

		decaf::util::concurrent::TimeUnit::SECONDS.sleep(2);

		if (DataUtil::postObservationEvent(data::OBS_END_ACQ, "S20110427-01") == status::ERROR) {
			cout << "ERROR posting OBS_END_ACQ" << endl;
		}

	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

