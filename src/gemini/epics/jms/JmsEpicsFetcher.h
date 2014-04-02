#ifndef JMSEPICSFETCHER_H_
#define JMSEPICSFETCHER_H_

#include <gemini/epics/EpicsFetcher.h>
#include <util/jms/JmsProducer.h>

namespace giapi {
namespace gemini {
namespace epics {

class JmsEpicsFetcher: public EpicsFetcher, util::jms::JmsProducer {

public:

  /**
   * Static factory method to instantiate a new JmsEpicsFetcher object
   * and obtain a smart pointer to access it.
   */
  static pEpicsFetcher create() throw (CommunicationException);

  virtual pEpicsStatusItem getChannel(const std::string &name, long timeout) throw (GiapiException);

private:

  /**
   * Private Constructor
   */
  JmsEpicsFetcher() throw (CommunicationException);

};

}
}
}

#endif /* EPICSFETCHER_H_ */
