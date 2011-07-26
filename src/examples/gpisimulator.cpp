/**
 * Example application to evaluate the performance of observations with many items 
 */

#include <iostream>
#include <fstream>
#include <signal.h>
#include <sys/time.h>

#include <decaf/util/concurrent/CountDownLatch.h>
#include <decaf/util/concurrent/TimeUnit.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/StatusUtil.h>
#include <giapi/GiapiUtil.h>
#include <giapi/DataUtil.h>
#include <giapi/ServicesUtil.h>
#include <src/util/TimeUtil.h>

#include <curlpp/cURLpp.hpp>
#include <curlpp/Easy.hpp>
#include <curlpp/Options.hpp>
#include <curlpp/Exception.hpp>

#define STATUS_ITEMS_COUNT 500

using namespace giapi;
using namespace std;

char *initObservation = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \
    <methodCall>\
        <methodName>HeaderReceiver.initObservation</methodName>\
        <params>\
                <param>\
                        <value>\
                                <string>GS-2006B-Q-57</string>\
                        </value>\
                </param>\
                <param>\
                        <value>\
                                <string>S20110427-01</string>\
                        </value>\
                </param>\
        </params>\
</methodCall>";

char *storeKeyword = "<methodCall>\
    <methodName>HeaderReceiver.storeKeyword</methodName>\
    <params>\
        <param>\
            <value>\
                <string>S20110427-01</string>\
            </value>\
        </param>\
        <param>\
            <value>\
                <string>GPISEQ</string>\
            </value>\
        </param>\
        <param>\
            <value>\
                <double>42.0</double>\
            </value>\
        </param>\
    </params>\
</methodCall>";
char *xmlRequest=NULL;
size_t readData(char *buffer, size_t size, size_t nitems) {
  strncpy(buffer, xmlRequest, size * nitems);
  return size * nitems;
}

string copyDataFile(string dataLocation, std::string dataLabel) {
	string newLocation(dataLocation);
	newLocation.append("/");
	newLocation.append(dataLabel);
	newLocation.append(".fits");

	cout << "TempFile : " << newLocation << endl;

	ifstream in("S20110427-01.fits", ifstream::binary);
	ofstream out(newLocation.c_str(), ifstream::binary);
	out << in.rdbuf();

	return newLocation;
}

void postXMLRequest(std::string dataLabel) {
     	try {
	    curlpp::Cleanup cleaner;
	    curlpp::Easy request;

	    std::list<std::string> header;
	    header.push_back("Content-Type: text/xml");

	    char buf[50];
	    int size = strlen(xmlRequest);
	    sprintf(buf, "Content-Length: %d", size);
	    header.push_back(buf);

	    using namespace curlpp::Options;
	    request.setOpt(new Url("http://localhost:12345/"));
	    //request.setOpt(new Verbose(true));
	    request.setOpt(new HttpHeader(header));
	    request.setOpt(new Post(true));
	    request.setOpt(new InfileSize(size));
	    request.setOpt(new ReadFunction(curlpp::types::ReadFunctionFunctor(readData)));

	    request.perform();
	  }
	  catch ( curlpp::LogicError & e ) {
	    std::cout << e.what() << std::endl;
	  }
	  catch ( curlpp::RuntimeError & e ) {
	    std::cout << e.what() << std::endl;
	  }
}

void postEvent(data::ObservationEvent eventType, int delay, std::string dataLabel)
{
    cout << "POST " << eventType << endl;
    if (DataUtil::postObservationEvent(eventType, dataLabel) == status::ERROR){
        cout << "ERROR posting " << eventType << endl;
    }

    decaf::util::concurrent::TimeUnit::MILLISECONDS.sleep(delay);
}

void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

int main(int argc, char **argv) {
	try {

		std::cout << "Starting GPI Simulation Test" << std::endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

                xmlRequest=initObservation;
                time_t seconds;
                seconds = time(NULL);

                std::stringstream ss;
                ss << "S" << seconds;
                cout << "Simulating datalabel " << ss.str() << endl;
                std::string dataLabel = ss.str();

               struct timeval start, end;
               gettimeofday (&start, NULL);
		postXMLRequest(dataLabel);
                gettimeofday (&end, NULL);
                double dif = (end.tv_sec - start.tv_sec) * 1000.0 + (end.tv_usec - start.tv_usec) / 1000.0;
                
                cout << endl << "time to post xmlrpc req:" << dif << " [ms]" << endl;
                
                string dataLocation = ServicesUtil::getProperty("DHS_SCIENCE_DATA_PATH", 1000);
                
                for (int i = 0; i < STATUS_ITEMS_COUNT; i++) {
                    std::stringstream ss;
                    ss << "S" << i;
                    StatusUtil::createStatusItem(ss.str(), type::INT);
                }
                    
		for (int i = 0; i < STATUS_ITEMS_COUNT; i++) {
			std::stringstream ss;
                        ss << "S" << i;
                        StatusUtil::setValueAsInt(ss.str(), i);
			StatusUtil::postStatus();
		}


		string newLocation = copyDataFile(dataLocation, dataLabel);
		postEvent(data::OBS_PREP, 400, dataLabel);
		postEvent(data::OBS_START_ACQ, 3000, dataLabel);
		postEvent(data::OBS_END_ACQ, 200, dataLabel);
		postEvent(data::OBS_START_READOUT, 1, dataLabel);
		postEvent(data::OBS_END_READOUT, 1, dataLabel);
		postEvent(data::OBS_START_DSET_WRITE, 200, dataLabel);
		postEvent(data::OBS_END_DSET_WRITE, 1, dataLabel);
                

	} catch (GmpException &e) {
		std::cerr << e.getMessage() <<  ". Is the GMP up?" << std::endl;
	}
	return 0;
}

