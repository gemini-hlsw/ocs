/*
 * StatusPostBenchmark.cpp
 *
 *  Created on: Feb 13, 2009
 *      Author: anunez
 */

#include "StatusPostBenchmark.h"

namespace giapi {

StatusPostBenchmark::StatusPostBenchmark() {

}

StatusPostBenchmark::~StatusPostBenchmark() {
}


int StatusPostBenchmark::getOps() {
	return NUM_MESSAGES * 5;
}

void StatusPostBenchmark::run() {

	for (int i = 0; i < NUM_MESSAGES; i++) {
		StatusUtil::setValueAsInt("gpi:cc:filter.X", i);
		StatusUtil::postStatus();
	}

	for (int i = 0; i < NUM_MESSAGES; i++) {
		StatusUtil::setValueAsDouble("gpi:cc:filter.Y", (double) i
				/ (double) NUM_MESSAGES);
		StatusUtil::postStatus();
	}

	for (int i = 0; i < NUM_MESSAGES; i++) {
		char x[256];
		sprintf(x, "Value %d", i);
		StatusUtil::setValueAsString("gpi:cc:filter.Z", x);
		StatusUtil::postStatus();
	}

	for (int i = 0; i < NUM_MESSAGES; i++) {
		char x[256];
		sprintf(x, "Failed %d times", i);
		StatusUtil::setAlarm("gpi:cc:filter.H", giapi::alarm::ALARM_FAILURE,
				giapi::alarm::ALARM_CAUSE_OTHER, x);
		StatusUtil::postStatus();
	}

	int healthMessages = NUM_MESSAGES / 3;
	for (int i = 0; i < healthMessages; i++) {
		StatusUtil::setHealth("gpi:health", giapi::health::GOOD);
		StatusUtil::postStatus();
		StatusUtil::setHealth("gpi:health", giapi::health::BAD);
		StatusUtil::postStatus();
		StatusUtil::setHealth("gpi:health", giapi::health::WARNING);
		StatusUtil::postStatus();
	}

}

void StatusPostBenchmark::setUp() {

	StatusUtil::createStatusItem("gpi:cc:filter.X", type::INT);
	StatusUtil::createStatusItem("gpi:cc:filter.Y", type::DOUBLE);
	StatusUtil::createStatusItem("gpi:cc:filter.Z", type::STRING);
	StatusUtil::createAlarmStatusItem("gpi:cc:filter.H", type::INT);
	StatusUtil::createHealthStatusItem("gpi:health");
}

}
