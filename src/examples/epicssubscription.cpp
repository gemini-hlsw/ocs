/*
 * An example program to show how to register and process
 * EPICS channels notifications
 */

#include <decaf/util/concurrent/CountDownLatch.h>

#include <giapi/GeminiUtil.h>
#include <giapi/GiapiUtil.h>
#include <giapi/EpicsStatusHandler.h>

#include "EpicsHandlerDemo.h"

#include <signal.h>
#include <stdlib.h>
#include <giapi/GiapiErrorHandler.h>

using namespace giapi;

/**
 * We use a GiapiErrorHandler to reconnect/resubscribe
 * the EPICS channels in case the connection to the GMP
 * gets broken.
 */
class Example: public GiapiErrorHandler {

public:
	Example() {
	}

	void onError() {
		std::cout << "Re-initing subscriptions" << std::endl;
		init();
	}

	void init() {
		pEpicsStatusHandler handler = EpicsHandlerDemo::create();
		GeminiUtil::subscribeEpicsStatus("gpi:ws:wsFilter.VALL", handler);
		GeminiUtil::subscribeEpicsStatus("gpi:ws:cpWf", handler);
	}

	~Example() {
		std::cout << "Destroying " << std::endl;
	}
};
typedef std::tr1::shared_ptr<Example> pExample;

void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	GeminiUtil::unsubscribeEpicsStatus("ws:wsFilter.VALL");
	GeminiUtil::unsubscribeEpicsStatus("ws:cpWf");
	exit(1);
}

int main(int argc, char **argv) {

	try {
		std::cout << "Starting Epics Subscription Demo" << std::endl;

		decaf::util::concurrent::CountDownLatch lock(1);

		pExample example(new Example());

		//The error handler in the example is registered, so it
		//will be invoked in case of GMP failure
		GiapiUtil::registerGmpErrorHandler(example);

		example->init();

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

		//Wait until is killed
		lock.await();
	} catch (GmpException &e) {
		std::cerr << "Is the GMP up?" << std::endl;

	}
	return 0;
}

