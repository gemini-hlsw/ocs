#include "JmsEpicsFetcher.h"

#include <log4cxx/logger.h>
#include <gemini/epics/EpicsFetcher.h>
#include <giapi/giapiexcept.h>

namespace giapi {
namespace gemini {
namespace epics {

JmsEpicsFetcher::JmsEpicsFetcher() throw (CommunicationException) {
}

pEpicsFetcher JmsEpicsFetcher::create() throw (CommunicationException) {
  pEpicsFetcher fetcher(new JmsEpicsFetcher());
  return fetcher;
}

pEpicsStatusItem JmsEpicsFetcher::getChannel(const std::string &name) throw (GiapiException) {
  throw new GiapiException();
}

}
}
}
