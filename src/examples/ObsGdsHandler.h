#ifndef OBSGDSHANDLER_H_
#define OBSGDSHANDLER_H_

#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <giapi/giapi.h>
#include <giapi/SequenceCommandHandler.h>
#include <decaf/lang/Thread.h>
#include <decaf/lang/Runnable.h>
#include <decaf/util/concurrent/CountDownLatch.h>
#include <decaf/util/concurrent/TimeUnit.h>
#include <giapi/DataUtil.h>
#include <giapi/StatusUtil.h>

using namespace decaf::util::concurrent;
using namespace decaf::util;
using namespace decaf::lang;
using namespace giapi;
using namespace giapi::command;


void printConfiguration(giapi::pConfiguration config)
{
    static int i = 0;
    printf("Call: %d\n",i++);
    // Print the configuration if it was sent
    if (config != NULL && config->getSize() > 0) {
        std::vector<std::string> keys = config->getKeys();
        std::vector<std::string>::iterator it = keys.begin();
        printf("Configuration: \n");
        for (; it < keys.end(); it++) {
            std::cout << "{" << *it << " : " << config->getValue(*it)
                << "}" << std::endl;
        }
    }else{
        printf("Empty config\n");
    }
}

/**
 * Spawns a thread to post the observation events and send COMPLETED back to GMP
 */
class WorkerThread: public Runnable {
    private:
        giapi::command::ActionId id;
        giapi::pConfiguration config;
    public:

        WorkerThread() {
            this->id = 0;
        }

        virtual ~WorkerThread() {
            cleanup();
        }

        void setId(giapi::command::ActionId id) {
            this->id = id;
        }

        void setConfig(giapi::pConfiguration config) {
            this->config = giapi::pConfiguration(config);
        }

        virtual void run() {
            string datalabel = config->getValue("DATA_LABEL");

            std::cout<<"label: "<<datalabel<<std::endl;

            printf("Configuration in worker:\n");
            printConfiguration(config);
            printf("End config in worker\n");

            printf("Worker Thread started!\n");
            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            printf("Sending observation events...\n");
            if(DataUtil::postObservationEvent(data::OBS_PREP, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_PREP << endl;
            }

            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);

            StatusUtil::createStatusItem("gpi:status1", type::STRING);
            StatusUtil::setValueAsString("gpi:status1", "117");
            StatusUtil::postStatus();

            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            if(DataUtil::postObservationEvent(data::OBS_START_ACQ, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_START_ACQ << endl;
            }

            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);

            StatusUtil::createStatusItem("gpi:status2", type::STRING);
            StatusUtil::setValueAsString("gpi:status2", "42");
            StatusUtil::postStatus();

            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            if(DataUtil::postObservationEvent(data::OBS_END_ACQ, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_END_ACQ << endl;
            }
            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            if(DataUtil::postObservationEvent(data::OBS_START_READOUT, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_START_READOUT << endl;
            }
            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            if(DataUtil::postObservationEvent(data::OBS_END_READOUT, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_END_READOUT << endl;
            }

            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            if(DataUtil::postObservationEvent(data::OBS_START_DSET_WRITE, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_START_DSET_WRITE << endl;
            }
            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            if(DataUtil::postObservationEvent(data::OBS_END_DSET_WRITE, datalabel) == status::ERROR){
                cout << "ERROR posting " << data::OBS_END_DSET_WRITE << endl;
            }

            printf("Sending completion info...\n");
            decaf::util::concurrent::TimeUnit::SECONDS.sleep(1);
            CommandUtil::postCompletionInfo(id, HandlerResponse::create(
                        HandlerResponse::COMPLETED));
            printf("Processing done\n");
        }

    private:
        void cleanup() {
            printf("Destroying Worker Thread\n");
        }
};

/**
 * Example Sequence command handler implementation.
 */
class ObsGdsHandler: public giapi::SequenceCommandHandler {

    private:
        WorkerThread * worker;
        Thread* thread;

    public:

        virtual giapi::pHandlerResponse handle(giapi::command::ActionId id, giapi::command::SequenceCommand sequenceCommand, giapi::command::Activity activity, giapi::pConfiguration config)
        {
            printConfiguration(config);

            // if the Activity is PRESET or CANCEL return ACCPETED
            if (activity == PRESET || activity == CANCEL) {
                printf("Response to caller %i\n", HandlerResponse::ACCEPTED);
                return HandlerResponse::create(HandlerResponse::ACCEPTED);
            }

            // For other activities return STARTED, send events and later COMPLETED
            printf("Starting worker thread for %i\n", id);

            if (thread != NULL) {
                thread->join();// this is where  code must be smarter than
                //this, or else the invoker won't receive answers while
                //this thread is processing.
                delete thread;
            }
            //spawn new thread that will sleep a bit and then send the observation events to GDS and
            // completion info to the GMP
            worker->setId(id);
            worker->setConfig(config);
            thread = new Thread( worker );
            thread->start();
            return HandlerResponse::create(HandlerResponse::STARTED);
        }


        static giapi::pSequenceCommandHandler create() {
            pSequenceCommandHandler handler(new ObsGdsHandler());
            return handler;
        }

        virtual ~ObsGdsHandler() {
            delete worker;
            if (thread != NULL) {
                thread->join();
                delete thread;
            }
        }

    private:
        ObsGdsHandler() {
            worker = new WorkerThread();
            thread = NULL;
            srandom(time(NULL));
        }
};

#endif /*HANDLER_H_*/
