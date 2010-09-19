/**
 * Example application to test GIAPI Services
 */

#include <iostream>
#include <signal.h>

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

	} catch(GmpException & e) {
        std:: cerr << e.getMessage() << ". Is the GMP up?" << std::endl;
	}
	return 0;
}
