#ifndef MYHANDLER_H_
#define MYHANDLER_H_

#include <iostream>
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
 * A worker thread to be used inside the sequence
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
		printf("Destroying Thread");
	}
};


/**
 * Example Sequence command handler implementation.
 */
class MyHandler: public giapi::SequenceCommandHandler {

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

		if (id % 2 == 1)
//			return HandlerResponse::createError(
//					"This crap didn't work.I refuse to execute your order");
			return HandlerResponse::create(HandlerResponse::COMPLETED);
		else {
			//			Thread* thread = new Thread( worker);
			//			thread->start();


			printf("About to start working thread\n");

			if (thread != NULL) {
				thread->join();// this is where  code must be smarter than
				//this, or else the invoker won't receive answers while
				//this thread is processing.
				delete thread;
			}
			thread = new Thread( worker );
			worker->setId(id);
			thread->start();
//			//fake a fast handler
//			CommandUtil::postCompletionInfo(id, HandlerResponse::create(
//							HandlerResponse::COMPLETED));
//			CountDownLatch lock(1);
//						lock.await(100);



			return HandlerResponse::create(HandlerResponse::STARTED);
		}

	}

	static giapi::pSequenceCommandHandler create() {
		pSequenceCommandHandler handler(new MyHandler());
		return handler;
	}

	virtual ~MyHandler() {
		printf("Destroying MyHandler");
		delete worker;
		if (thread != NULL) {
			thread->join();
			delete thread;
		}
	}

private:
	MyHandler() {
		worker = new WorkerThread();
		thread = NULL;
	}
};

#endif /*MYHANDLER_H_*/
