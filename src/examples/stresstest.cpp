/**
 * Application to stress the GIAPI status -> epics system.
 */

#include <iostream>
#include <signal.h>
#include <sys/time.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/StatusUtil.h>
#include <giapi/GiapiUtil.h>
#include <src/util/TimeUtil.h>
#include <src/util/PropertiesUtil.h>
#include <src/gmp/ConnectionManager.h>

using namespace giapi;



void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

int main(int argc, char **argv) {
    //util::PropertiesUtil::Instance().load(std::string("gmp.properties"));
    util::TimeUtil timer;
	double time;
	double throughput;
	int nReps = 10000;
	try {

		std::cout << "Starting Status Example" << std::endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);


		decaf::util::concurrent::CountDownLatch lock(1);
		StatusUtil::createStatusItem("gpi:status1", type::INT);
		StatusUtil::createAlarmStatusItem("gpi:alarm1", type::INT);
		StatusUtil::createHealthStatusItem("gpi:health1");
	
        timer.startTimer();
		for (int i = 0; i < nReps; i++) {
			StatusUtil::setValueAsInt("gpi:status1", i);
            StatusUtil::setValueAsInt("gpi:alarm1", nReps-i);
            if(i%2==0){
                StatusUtil::setAlarm("gpi:alarm1",giapi::alarm::ALARM_FAILURE,giapi::alarm::ALARM_CAUSE_HI,"Alarm message here!");
                StatusUtil::setHealth("gpi:health1",giapi::health::WARNING);
            }else{
                StatusUtil::setAlarm("gpi:alarm1",giapi::alarm::ALARM_OK,giapi::alarm::ALARM_CAUSE_OK,"");
                StatusUtil::setHealth("gpi:health1",giapi::health::GOOD);
            }
            StatusUtil::postStatus();
		}
        timer.stopTimer();
    	time = timer.getElapsedTime(util::TimeUtil::MSEC)/1000.0;

		throughput = double(nReps * 3) / time;

		std::cout << "Elapsed Time: " << time << " [sec]" << std::endl;
		std::cout << "Throughput  : " << throughput << " [msg/sec]" << std::endl;


	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

