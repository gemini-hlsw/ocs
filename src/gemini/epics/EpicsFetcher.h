#ifndef EPICSFETCHER_H_
#define EPICSFETCHER_H_

#include <tr1/memory>

#include <giapi/giapi.h>
#include <giapi/EpicsStatusItem.h>

namespace giapi {
namespace gemini {
namespace epics {

/**
 * The EpicsFetcher interface provides a mechanism to get information about
 * an EPICS channelfrom Gemini.
 */
class EpicsFetcher {

public:

  /**
   * Provides a pointer to an EpicsStatus item containing the latest channel
   * information available
   *
   * @param name Name of the EPICS status item that will be retrieved  
   * @param timeout time in milliseconds to wait for the TCS context to be
   *        retrieved. If not specified, the call will block until the
   *        GMP replies back.
   *
   * @return a smart pointer to an EpicsStatusItem with the latest known values
   *
   * @throws GiapiException if there is an error accessing the GMP
   *         or a timeout occurs.
   */
  virtual pEpicsStatusItem getChannel(const std::string &name, long timeout) throw (GiapiException) = 0;

  /**
   * Destructor
   */
  virtual ~EpicsFetcher() {};


};

/**
 * A smart pointer definition for the TcsFetcher class.
 */
typedef std::tr1::shared_ptr<EpicsFetcher> pEpicsFetcher;

}
}
}

#endif /* EPICSFETCHER_H_ */
