/**
 * Example application to send file events to Gemini
 */

#include <iostream>
#include <stdlib.h>
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

		std::cout << "Starting File Event Example" << std::endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

		/* We start with some Ancillary File Events */
		if (DataUtil::postAncillaryFileEvent("01myfile1.txt", "S2009020201-1") == status::ERROR) {
			std::cerr << "ERROR posting ancillary file event" << std::endl;
		} else {
			std::cout << "Ancillary File Event: 01myfile1.txt, S2009020201-1" << std::endl;
		}

		if (DataUtil::postAncillaryFileEvent("myfile1.txt", "") == status::ERROR) {
			std::cerr << "Expected error - Dataset label not specified. No message should have been sent " << std::endl;
		}

		if (DataUtil::postAncillaryFileEvent("", "S2009020201-1") == status::ERROR) {
			std::cerr << "Expected error - Filename not specified. No message should have been sent " << std::endl;
		}

		if (DataUtil::postAncillaryFileEvent("", "") == status::ERROR) {
			std::cerr << "Expected error - No filename nor dataset specified. No message should have been sent " << std::endl;
		}

		/* Now we send intermediate file events */

		if (DataUtil::postIntermediateFileEvent("02myfile1.txt", "S2009020201-1", "") == status::ERROR) {
			std::cerr << "ERROR posting ancillary file event" << std::endl;
		} else {
			std::cout << "Intermediate File Event: 02myfile1.txt, S2009020201-1, null" << std::endl;
		}

		if (DataUtil::postIntermediateFileEvent("03myfile2.txt", "S2009020201-1", "hint") == status::ERROR) {
			std::cerr << "ERROR posting ancillary file event" << std::endl;
		} else {
			std::cout << "Intermediate File Event: 03myfile1.txt, S2009020201-1, hint" << std::endl;
		}


		if (DataUtil::postIntermediateFileEvent("myfile1.txt", "", "") == status::ERROR) {
			std::cerr << "Expected error - Dataset label not specified. No message should have been sent " << std::endl;
		}

		if (DataUtil::postIntermediateFileEvent("", "S2009020201-1", "") == status::ERROR) {
			std::cerr << "Expected error - Filename not specified. No message should have been sent " << std::endl;
		}

		if (DataUtil::postIntermediateFileEvent("", "", "") == status::ERROR) {
			std::cerr << "Expected error - No filename nor dataset specified. No message should have been sent  " << std::endl;
		}

	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

