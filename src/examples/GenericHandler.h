#ifndef GENERICHANDLER_H_
#define GENERICHANDLER_H_

#include <iostream>
#include <stdlib.h>
#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <decaf/lang/Thread.h>
#include <decaf/lang/Runnable.h>
#include <decaf/util/concurrent/CountDownLatch.h>
#include "ApplyHandler.h"

using namespace decaf::util::concurrent;
using namespace decaf::util;
using namespace decaf::lang;
using namespace giapi;
using namespace giapi::command;

/**
 * Example Sequence command handler implementation.
 */
class GenericHandler: public giapi::SequenceCommandHandler {

private:
	WorkerThread * worker;
	Thread* thread;

public:

	void printConfiguration(giapi::pConfiguration config)
    {
        // Print the configuration if it was sent
        if (config != NULL && config->getSize() > 0) {
			std::vector<std::string> keys = config->getKeys();
			std::vector<std::string>::iterator it = keys.begin();
			printf("Configuration\n");
			for (; it < keys.end(); it++) {
				std::cout << "{" << *it << " : " << config->getValue(*it)
						<< "}" << std::endl;
			}
		}
    }

    virtual giapi::pHandlerResponse handle(giapi::command::ActionId id, giapi::command::SequenceCommand sequenceCommand, giapi::command::Activity activity, giapi::pConfiguration config)
    {
        printConfiguration(config);

        // if the Activity is PRESET or CANCEL return ACCPETED
		if (activity == PRESET || activity == CANCEL) {
			printf("Response to caller %i\n", HandlerResponse::ACCEPTED);
			return HandlerResponse::create(HandlerResponse::ACCEPTED);
		}

		// For other activities return half of the time COMPLETED and half STARTED and later COMPLETED
		if (random() % 2 == 1) {
			printf("Response to caller %i\n", HandlerResponse::COMPLETED);
			return HandlerResponse::create(HandlerResponse::COMPLETED);
		} else {
			printf("Starting worker thread for %i\n", id);

			if (thread != NULL) {
				thread->join();// this is where  code must be smarter than
				//this, or else the invoker won't receive answers while
				//this thread is processing.
				delete thread;
			}
			thread = new Thread( worker );
			worker->setId(id);
			thread->start();
			return HandlerResponse::create(HandlerResponse::STARTED);
		}

	}

	static giapi::pSequenceCommandHandler create() {
		pSequenceCommandHandler handler(new GenericHandler());
		return handler;
	}

	virtual ~GenericHandler() {
		delete worker;
		if (thread != NULL) {
			thread->join();
			delete thread;
		}
	}

private:
	GenericHandler() {
		worker = new WorkerThread();
		thread = NULL;
        srandom(time(NULL));
	}
};

#endif /*HANDLER_H_*/
