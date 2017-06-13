#ifndef APPLYHANDLER_H_
#define APPLYHANDLER_H_

#include <iostream>
#include <stdio.h>
#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <decaf/lang/Thread.h>
#include <decaf/lang/Runnable.h>
#include <decaf/util/concurrent/CountDownLatch.h>

using namespace decaf::util::concurrent;
using namespace decaf::util;
using namespace decaf::lang;
using namespace giapi;

/**
 * A worker thread to be used inside the apply sequence
 * command handler implementation below
 */
class WorkerThread: public Runnable {
private:

	int numMessages;

	giapi::command::ActionId id;
public:

	WorkerThread() {
		this->numMessages = 0;
		this->id = 0;
	}

	virtual ~WorkerThread() {
		cleanup();
	}

	void setId(giapi::command::ActionId id) {
		this->id = id;
		numMessages = 0;
	}

	virtual void run() {
		printf("Worker Thread started!\n");
		while (numMessages < 10) {
			printf("Messages processed = %d \n", numMessages++);
			CountDownLatch lock(1);
			lock.await(500);
		}
		CommandUtil::postCompletionInfo(id, HandlerResponse::create(
				HandlerResponse::COMPLETED));
	}

private:

	void cleanup() {
		printf("Destroying Worker Thread");
	}
};


/**
 * Example Sequence command handler implementation.
 */
class ApplyHandler: public giapi::SequenceCommandHandler {

private:
	WorkerThread * worker;
	Thread* thread;

public:

	virtual giapi::pHandlerResponse handle(giapi::command::ActionId id,
			giapi::command::SequenceCommand sequenceCommand,
			giapi::command::Activity activity, giapi::pConfiguration config) {

		if (config != NULL) {
			std::vector<std::string> keys = config->getKeys();
			std::vector<std::string>::iterator it = keys.begin();
			printf("Configuration\n");
			for (; it < keys.end(); it++) {
				std::cout << "{" << *it << " : " << config->getValue(*it)
						<< "}" << std::endl;
			}
		}

		printf("Starting worker thread on id %i\n", id);

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

	static giapi::pSequenceCommandHandler create() {
		pSequenceCommandHandler handler(new ApplyHandler());
		return handler;
	}

	virtual ~ApplyHandler() {
		delete worker;
		if (thread != NULL) {
			thread->join();
			delete thread;
		}
	}

private:
	ApplyHandler() {
		worker = new WorkerThread();
		thread = NULL;
	}
};

#endif /*APPLYHANDLER_H_*/
