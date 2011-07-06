/**
 * Example application to send observation events to Gemini
 */

#include <iostream>
#include <fstream>
#include <signal.h>

#include <curlpp/cURLpp.hpp>
#include <curlpp/Easy.hpp>
#include <curlpp/Options.hpp>
#include <curlpp/Exception.hpp>

#include <decaf/util/concurrent/CountDownLatch.h>
#include <decaf/util/concurrent/TimeUnit.h>

#include <giapi/GiapiErrorHandler.h>
#include <giapi/DataUtil.h>
#include <giapi/ServicesUtil.h>
#include <giapi/GiapiUtil.h>

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

void terminate(int signal) {
	std::cout << "Exiting... " << std::endl;
	exit(1);
}

string copyDataFile(string dataLocation) {
	string newLocation(dataLocation);
	newLocation.append("/");
	newLocation.append("S20110427-01.fits");

	cout << "TempFile : " << newLocation << endl;

	ifstream in("S20110427-01.fits", ifstream::binary);
	ofstream out(newLocation.c_str(), ifstream::binary);
	out << in.rdbuf();

	return newLocation;
}

void postXMLRequest() {
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
	    request.setOpt(new Verbose(true));
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

void postEvent(data::ObservationEvent eventType, int delay)
{
    //giapi::data::ObservationEvent eventType = data::OBS_PREP;
	cout << "POST " << eventType << endl;
    if(DataUtil::postObservationEvent(eventType, "S20110427-01") == status::ERROR){
        cout << "ERROR posting " << eventType << endl;
    }

    decaf::util::concurrent::TimeUnit::SECONDS.sleep(delay);
}

int main(int argc, char **argv) {

	try {
		cout << "Starting Observation Process Example" << endl;

		signal(SIGABRT, terminate);
		signal(SIGTERM, terminate);
		signal(SIGINT, terminate);

        xmlRequest=initObservation;
		postXMLRequest();

        xmlRequest=storeKeyword;;
		postXMLRequest();

		cout << "Retrieving Data file location" << endl;

		string dataLocation = ServicesUtil::getProperty("DHS_SCIENCE_DATA_PATH", 1000);

		cout << "DataSciencePath : " << dataLocation << endl;

		string newLocation = copyDataFile(dataLocation);
		postEvent(data::OBS_PREP, 1);
		postEvent(data::OBS_START_ACQ, 2);
		postEvent(data::OBS_END_ACQ, 2);
		postEvent(data::OBS_START_READOUT, 1);
		postEvent(data::OBS_END_READOUT, 1);
		postEvent(data::OBS_START_DSET_WRITE, 1);
		postEvent(data::OBS_END_DSET_WRITE, 1);
	} catch (GmpException &e) {
		std::cerr << e.getMessage() << ". Is the GMP up?" << std::endl;
	}
	return 0;
}

