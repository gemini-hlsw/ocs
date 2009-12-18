#ifndef TCSFETCHER_H_
#define TCSFETCHER_H_

#include <tr1/memory>

#include <giapi/giapi.h>

namespace giapi {

namespace gemini {

namespace tcs {

/**
 * The TcsFetcher interface provides a mechanism to get the TCS Context
 * from Gemini. The TCS Context provides telescope information that
 * is required, among others, to perform WCS calculations.
 */
class TcsFetcher {

public:

	/**
	 * Get the TCS Context from Gemini.
	 * @param ctx A reference to the TcsContext structure that will
	 *            be filled in by this call.
	 * @param timeout timeout in millisecond for the request to complete.
	 *        If not specified, the call will block until the GMP replies back
	 * @return status::OK if the TcsContext was filled up properly.
	 *         status::ERROR if there was an error getting the TcsContext
	 * @throws GiapiException if there is an error accessing the GMP to
	 *         obtain the TCS Context, or a timeout occurs.
	 *
	 */
	virtual int fetch(TcsContext &ctx, long timeout) throw (GiapiException) = 0;

	/**
	 * Destructor
	 */
	virtual ~TcsFetcher() {};


};

/**
 * A smart pointer definition for the TcsFetcher class.
 */
typedef std::tr1::shared_ptr<TcsFetcher> pTcsFetcher;


}
}
}
#endif /* TCSFETCHER_H_ */
