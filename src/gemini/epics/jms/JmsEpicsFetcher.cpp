#include "JmsEpicsFetcher.h"

#include <log4cxx/logger.h>
#include <gemini/epics/EpicsFetcher.h>
#include <gemini/epics/EpicsStatusItemImpl.h>
#include <gemini/epics/jms/JmsEpicsFactory.h>
#include <giapi/giapiexcept.h>
#include <giapi/giapi.h>
#include <giapi/EpicsStatusItem.h>
#include <gmp/GMPKeys.h>

namespace giapi {
namespace gemini {
namespace epics {

JmsEpicsFetcher::JmsEpicsFetcher() throw (CommunicationException) :
  JmsProducer(GMPKeys::GMP_GEMINI_EPICS_GET_DESTINATION) {
}

pEpicsFetcher JmsEpicsFetcher::create() throw (CommunicationException) {
  pEpicsFetcher fetcher(new JmsEpicsFetcher());
  return fetcher;
}

pEpicsStatusItem JmsEpicsFetcher::getChannel(const std::string &name, long timeout) throw (GiapiException) {
  Message * request = NULL;
  try {
    //an empty message to make the request. We don't need to provide any data.
    request = _session->createMessage();
    request->setStringProperty(gmp::GMPKeys::GMP_GEMINI_EPICS_CHANNEL_PROPERTY,
        name);
    //create temporary objects to get the answer
    TemporaryQueue * tmpQueue = _session->createTemporaryQueue();
    MessageConsumer * tmpConsumer = _session->createConsumer(tmpQueue);

    //define the destination for the service to provide an answer
    request->setCMSReplyTo(tmpQueue);
    //send the request
    _producer->send(request);
    //delete the request, not needed anymore
    delete request;

    //and wait for the response, timing out if necessary.
    Message *reply = (timeout > 0) ? tmpConsumer->receive(timeout)
        : tmpConsumer->receive();

    tmpConsumer->close();
    delete tmpConsumer;

    tmpQueue->destroy();
    delete tmpQueue;

    if (reply != NULL) {
      const BytesMessage* mapMessage =
        dynamic_cast<const BytesMessage*> (reply);

      if (mapMessage == NULL) {
        throw GiapiException("Incorrect reply from the GMP");
      }

      return JmsEpicsFactory::buildEpicsStatusItem(mapMessage);
    } else { //timeout .Throw an exception
      throw TimeoutException("Time out while waiting for Epics Get");
    }

  } catch (CMSException &e) {
    if (request != NULL) {
      delete request;
    }
    std::cout << "exc " << std::endl;

    throw CommunicationException("Problem fetching the TCS Context "
        + e.getMessage());
  }



}

}
}
}




