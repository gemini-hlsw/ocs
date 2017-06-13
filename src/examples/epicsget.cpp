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
#include <giapi/GiapiErrorHandler.h>
#include <stdlib.h>

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
  }

  void init() {
    std::cout << "Get channel value" << std::endl;
    pEpicsStatusItem doubleStatusItem = GeminiUtil::getChannel("gpi:E1", 5000);
    std::cout << "Got channel " << doubleStatusItem->getName() << std::endl;
    std::cout << "Channel length " << doubleStatusItem->getCount() << std::endl;
    std::cout << "Channel value " << doubleStatusItem->getDataAsDouble(0) << std::endl;

    pEpicsStatusItem stringStatusItem = GeminiUtil::getChannel("agpi:E3", 5000);
    std::cout << "Got channel " << stringStatusItem->getName() << std::endl;
    std::cout << "Channel length " << stringStatusItem->getCount() << std::endl;

    std::cout << "Channel value " << stringStatusItem->getDataAsString(0) << std::endl;

  }

  ~Example() {
    std::cout << "Destroying " << std::endl;
  }
};

typedef std::tr1::shared_ptr<Example> pExample;

void terminate(int signal) {
  std::cout << "Exiting... " << std::endl;
  exit(1);
}

int main(int argc, char **argv) {

  try {
    std::cout << "Starting Epics Get Demo" << std::endl;

    decaf::util::concurrent::CountDownLatch lock(1);

    pExample example(new Example());

    //The error handler in the example is registered, so it
    //will be invoked in case of GMP failure
    GiapiUtil::registerGmpErrorHandler(example);

    signal(SIGABRT, terminate);
    signal(SIGTERM, terminate);
    signal(SIGINT, terminate);

    std::cout << "Await... " << std::endl;

    example->init();

    //Wait until is killed
    lock.await();
    return 0;

  } catch (GmpException &e) {
    std::cerr << "Is the GMP up?" << std::endl;
  }
  return 0;
}








