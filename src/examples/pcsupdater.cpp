/*
 * A test application to demonstrate how to send zernikes corrections
 * to the PCS via the GIAPI
 *
 *  Created on: Dec 10, 2009
 *      Author: anunez
 */

#include <iostream>
#include <iomanip>

#include <giapi/GeminiUtil.h>

using namespace giapi;


int main(int argc, char **argv) {

	try {

		std::cout << "Starting PCS Zernikes Example" << std::endl;

		double zernikes[] = {
				1.0, 2.0, 3.0, 4.0, 5.0, 6.0
		};


		if (GeminiUtil::postPcsUpdate(zernikes, 6) == status::ERROR) {
			std::cout << "Can't post zernikes to the PCS..." << std::endl;
			return 0;
		}

	} catch (GiapiException &e) {
		std::cerr << "Is the GMP up?" << std::endl;
	}
	return 0;
}
