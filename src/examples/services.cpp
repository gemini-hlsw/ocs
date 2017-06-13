/**
 * Example application to test GIAPI Services
 */

#include <iostream>
#include <signal.h>
#include <stdlib.h>

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/ServicesUtil.h>
#include <giapi/GiapiUtil.h>

using namespace giapi;

void
terminate(int signal)
{
    std::cout << "Exiting... " << std::endl;
    exit(1);
}

int
main(int argc, char **argv)
{

    try {

        std:: cout << "Starting Services Util Example" << std::endl;

        signal(SIGABRT, terminate);
        signal(SIGTERM, terminate);
        signal(SIGINT, terminate);

        std:: string host = ServicesUtil::getProperty("GMP_HOST_NAME", 1000);

        std:: cout << "Hostname: " << host << std::endl;

        std:: string ancilliaryDataPath = ServicesUtil::getProperty("DHS_ANCILLARY_DATA_PATH", 1000);

        std:: cout << "Ancilliary Data Path: " << ancilliaryDataPath << std::endl;

        std:: string sciencePath = ServicesUtil::getProperty("DHS_SCIENCE_DATA_PATH", 1000);

        std:: cout << "Science Path: " << sciencePath << std::endl;

        std:: string permanentSciencePath = ServicesUtil::getProperty("DHS_PERMANENT_SCIENCE_DATA_PATH", 1000);

        std:: cout << "Permanent Science Path: " << permanentSciencePath << std::endl;

        std:: string intermediatePath = ServicesUtil::getProperty("DHS_INTERMEDIATE_DATA_PATH", 1000);

        std:: cout << "Intermediate Data Path: " << intermediatePath << std::endl;

        std:: string defaultValue = ServicesUtil::getProperty("DEFAULT", 1000);

        std:: cout << "Default: " << defaultValue << std::endl;

    } catch(GmpException & e) {
        std:: cerr << e.getMessage() << ". Is the GMP up?" << std::endl;
    }
    return 0;
}
