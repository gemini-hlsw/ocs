#ifndef JMSTCSFETCHER_H_
#define JMSTCSFETCHER_H_

#include <giapi/giapiexcept.h>
#include <gemini/tcs/TcsFetcher.h>
#include <util/jms/JmsProducer.h>

namespace giapi {

namespace gemini {

namespace tcs {

namespace jms {

/**
 * A TCS Context Fetcher implemented using JMS. It is implemented as
 * a JMS Producer to perform the request and retrieve the result
 * synchronously.
 */
class JmsTcsFetcher: public TcsFetcher, util::jms::JmsProducer {

public:

	/**
	 * Get the TCS Context from Gemini.
	 * @param ctx A reference to the TcsContext structure that will
	 *            be filled in by this call.
	 * @param timeout timeout in millisecond for the request to complete.
	 *        If not specified, the call will block until the GMP replies back
	 * @return status::OK if the TcsContext was filled up properly.
	 *         status::ERROR if there was an error getting the TcsContext
	 * @throws CommunicationException if there is an error accessing the GMP to
	 *         obtain the TCS Context
	 * @throws TimeoutException if a timeout occurs.
	 *
	 */
	int fetch(TcsContext &ctx, long timeout) throw (CommunicationException,
			TimeoutException);

	/**
	 * Static factory method to instantiate a new JmsTcsFetcher object
	 * and obtain a smart pointer to access it.
	 */
	static pTcsFetcher create() throw (CommunicationException);

private:

	/**
	 * Private Constructor
	 */
	JmsTcsFetcher() throw (CommunicationException);

	/**
	 * Auxiliary method to reconstruct the TcsContext from
	 * a JMS message
	 *
	 * @param ctx the TcsContext structure to fill in with
	 *            the message content
	 * @param msg the JMS message to process containing the
	 *            content of the TCS context
	 * @throws CMSException if there is a problem reading
	 *         the content from the JMS Message
	 */
	int _buildTcsContext(TcsContext & ctx, Message * msg)
			throw (CMSException);

	/**
	 * Size of the TCS context
	 */
	static int const TCS_CTX_SIZE;

};

}

}

}

}
#endif /* JMSTCSFETCHER_H_ */
