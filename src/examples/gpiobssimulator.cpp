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

const char *openObservation = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \
    <methodCall>\
        <methodName>HeaderReceiver.openObservation</methodName>\
        <params>\
                <param>\
                        <value>\
                                <string>GS-2006B-Q-57</string>\
                        </value>\
                </param>\
                <param>\
                        <value>\
                                <string>%s</string>\
                        </value>\
                </param>\
        </params>\
</methodCall>";
const char *closeObservation = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \
    <methodCall>\
        <methodName>HeaderReceiver.closeObservation</methodName>\
        <params>\
                <param>\
                        <value>\
                                <string>%s</string>\
                        </value>\
                </param>\
        </params>\
</methodCall>";

const char *storeKeyword = "<methodCall>\
    <methodName>HeaderReceiver.storeKeywords</methodName>\
        <params>\
            <param>\
                    <value>\
                            <string>%s</string>\
                    </value>\
            </param>\
            <param>\
                <value>\
                <array><data>\
                    <value><string>GEMPRGID,STRING,1 </string></value>\
                    <value><string>OBSID,STRING,1 </string></value>\
                    <value><string>DATALAB,STRING,1 </string></value>\
                    <value><string>OBSERVER,STRING,1 </string></value>\
                    <value><string>OBSTYPE,STRING,1 </string></value>\
                    <value><string>OBSCLASS,STRING,1 </string></value>\
                    <value><string>SSA,STRING,1 </string></value>\
                    <value><string>RAWIQ,STRING,1 </string></value>\
                    <value><string>RAWCC,STRING,1 </string></value>\
                    <value><string>RAWWV,STRING,1 </string></value>\
                    <value><string>RAWBG,STRING,1 </string></value>\
                    <value><string>RAWPIREQ,STRING,1 </string></value>\
                    <value><string>RAWGEMQA,STRING,1 </string></value>\
                    <value><string>OBSERVAT,STRING,1 </string></value>\
                    <value><string>TELESCOP,STRING,1 </string></value>\
                    <value><string>INSTRUME,STRING,1 </string></value>\
                    <value><string>OBJECT,STRING,1 </string></value>\
                    <value><string>HUMIDITY,DOUBLE,1 </string></value>\
                    <value><string>TAMBIENT,DOUBLE,1 </string></value>\
                    <value><string>TAMBIEN2,DOUBLE,1 </string></value>\
                    <value><string>PRESSURE,DOUBLE,1 </string></value>\
                    <value><string>PRESSUR2,DOUBLE,1 </string></value>\
                    <value><string>DEWPOINT,DOUBLE,1 </string></value>\
                    <value><string>DEWPOIN2,DOUBLE,1 </string></value>\
                    <value><string>WINDSPEE,DOUBLE,1 </string></value>\
                    <value><string>WINDSPE2,DOUBLE,1 </string></value>\
                    <value><string>WINDDIRE,DOUBLE,1 </string></value>\
                    <value><string>AIRMASS,DOUBLE,1 </string></value>\
                    <value><string>AMEND,DOUBLE,1 </string></value>\
                    <value><string>AMSTART,DOUBLE,1 </string></value>\
                    <value><string>HA,STRING,1 </string></value>\
                    <value><string>LT,STRING,1 </string></value>\
                    <value><string>TRKFRAME,STRING,1 </string></value>\
                    <value><string>DECTRACK,DOUBLE,1 </string></value>\
                    <value><string>TRKEPOCH,DOUBLE,1 </string></value>\
                    <value><string>RATRACK,DOUBLE,1 </string></value>\
                    <value><string>TRKEQUIN,DOUBLE,1 </string></value>\
                    <value><string>RADECSYS,STRING,1 </string></value>\
                    <value><string>PMDEC,DOUBLE,1 </string></value>\
                    <value><string>PMRA,DOUBLE,1 </string></value>\
                    <value><string>RA,DOUBLE,1 </string></value>\
                    <value><string>DEC,DOUBLE,1 </string></value>\
                    <value><string>ELEVATIO,DOUBLE,1 </string></value>\
                    <value><string>AZIMUTH,DOUBLE,1 </string></value>\
                    <value><string>CRPA,DOUBLE,1 </string></value>\
                    <value><string>PARALLAX,DOUBLE,1 </string></value>\
                    <value><string>RADVEL,DOUBLE,1 </string></value>\
                    <value><string>EPOCH,DOUBLE,1 </string></value>\
                    <value><string>EQUINOX,DOUBLE,1 </string></value>\
                    <value><string>UT,STRING,1 </string></value>\
                    <value><string>DATE,STRING,1 </string></value>\
                    <value><string>ST,STRING,1 </string></value>\
                    <value><string>XOFFSET,DOUBLE,1 </string></value>\
                    <value><string>YOFFSET,DOUBLE,1 </string></value>\
                    <value><string>POFFSET,DOUBLE,1 </string></value>\
                    <value><string>QOFFSET,DOUBLE,1 </string></value>\
                    <value><string>RAOFFSET,DOUBLE,1 </string></value>\
                    <value><string>DECOFFSE,DOUBLE,1 </string></value>\
                    <value><string>RATRGOFF,DOUBLE,1 </string></value>\
                    <value><string>DECTRGOF,DOUBLE,1 </string></value>\
                    <value><string>PA,DOUBLE,1 </string></value>\
                    <value><string>IAA,DOUBLE,1 </string></value>\
                    <value><string>SFRT2,DOUBLE,1 </string></value>\
                    <value><string>SFTILT,DOUBLE,1 </string></value>\
                    <value><string>SFLINEAR,DOUBLE,1 </string></value>\
                    <value><string>AOFOLD,STRING,1 </string></value>\
                    <value><string>M2BAFFLE,STRING,1 </string></value>\
                    <value><string>M2CENBAF,STRING,1 </string></value>\
                    <value><string>INPORT,INT,1 </string></value>\
                    <value><string>CRFOLLOW,STRING,1 </string></value>\
                    <value><string>WAVELENG,DOUBLE,1 </string></value>\
                    <value><string>GCALLAMP,STRING,1 </string></value>\
                    <value><string>GCALFILT,STRING,1 </string></value>\
                    <value><string>GCALDIFF,STRING,1 </string></value>\
                    <value><string>GCALSHUT,STRING,1 </string></value>\
                    <value><string>GEADIMM,DOUBLE,1 </string></value>\
                    <value><string>GEAMASS,DOUBLE,1 </string></value>\
                    <value><string>GEAR0,DOUBLE,1 </string></value>\
                </data></array>\
                </value>\
            </param>\
    </params>\
</methodCall>";

string copyDataFile(string dataLocation, std::string dataLabel) {
    string newLocation(dataLocation);
    newLocation.append("/");
    newLocation.append(dataLabel);

    cout << "TempFile : " << newLocation << endl;

    ifstream in("S20110427-01.fits", ifstream::binary);
    ofstream out(newLocation.c_str(), ifstream::binary);
    out << in.rdbuf();

    return newLocation;
}

void postXMLRequest(std::string dataLabel, const char* xml) {
    try {
        curlpp::Cleanup cleaner;
        curlpp::Easy request;

        std::list<std::string> header;

        header.push_back("Content-Type: text/xml");

        char *xmlRequest = new char[dataLabel.size() + strlen(xml)];
        sprintf(xmlRequest, xml, dataLabel.c_str());
        std::istringstream instream(xmlRequest);
        int size = instream.str().size(); 

        char buf[50];
        sprintf(buf, "Content-Length: %d", size);
        header.push_back(buf);
        using namespace curlpp::Options;

        request.setOpt(new Url("http://localhost:8001/xmlrpc"));
        request.setOpt(new HttpHeader(header));
        request.setOpt(new Post(true));
        request.setOpt(new InfileSize(size));
        request.setOpt(new ReadStream(&instream));

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
            std::cout << "Starting GPI Simulator Test" << std::endl;

            signal(SIGABRT, terminate);
            signal(SIGTERM, terminate);
            signal(SIGINT, terminate);

            time_t seconds;
            seconds = time(NULL);

            std::stringstream ss;
            ss << "S" << seconds;
            cout << "Simulating datalabel " << ss.str() << endl;
            std::string dataLabel = ss.str();

            struct timeval start, end;
            gettimeofday (&start, NULL);
            //postXMLRequest(dataLabel, openObservation);
            //postXMLRequest(dataLabel, storeKeyword);
            //postXMLRequest(dataLabel, closeObservation);
            gettimeofday (&end, NULL);
            double dif = (end.tv_sec - start.tv_sec) * 1000.0 + (end.tv_usec - start.tv_usec) / 1000.0;
            
            cout << endl << "time to post xmlrpc req:" << dif << " [ms]" << endl;
            
            string dataLocation = ServicesUtil::getProperty("DHS_SCIENCE_DATA_PATH", 1000);
            
            /*for (int i = 0; i <= STATUS_ITEMS_COUNT; i++) {
                std::stringstream ss;
                ss << "S" << i;
                StatusUtil::createStatusItem(ss.str(), type::INT);
            }
                
            for (int i = 0; i <= STATUS_ITEMS_COUNT; i++) {
                    std::stringstream ss;
                    ss << "S" << i;
                    StatusUtil::setValueAsInt(ss.str(), i);
                    StatusUtil::postStatus();
            }*/

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

