#ifndef GEMINIINTERACTIONUTIL_H_
#define GEMINIINTERACTIONUTIL_H_
#include <string>

#include <giapi/EpicsStatusHandler.h>
#include <giapi/giapi.h>
#include <giapi/giapiexcept.h>

namespace giapi {
/**
 * Provides the mechanisms for the instrument to interact with other
 * Gemini Principal Systems.
 */
class GeminiUtil {
public:
	/**
	 * Register a handler to receive updates when the specified EPICS status
	 * item is updated.
	 * <p/>
	 * This method is called with the <code>name</code> of the EPICS status
	 * item and a <code>handler</code> that will be called when the EPICS
	 * system publishes an update.
	 *
	 * @param name Name of the EPICS status item that will be monitored.
	 * @param handler Handler that will be called when an update is
	 *        published.The most recently registered epics status handler will
	 *        be used.
	 *
	 * @return status::OK if the subscription was successful,
	 *         status::ERROR if there is a problem with the subscription
	 *         (for instance, the epics channel name is invalid)
	 *
	 * @throws GiapiException if there is an error accessing the GMP to
	 *         subscribe this handler to monitor the given EPICS channel item
	 */
	static int subscribeEpicsStatus(const std::string &name,
			pEpicsStatusHandler handler) throw (GiapiException);

	/**
	 * Unregister any handlers that might be associated to the given EPICS
	 * status item.
	 *
	 * @param name Name of the EPICS status item that no longer will be
	 * monitored
	 *
	 * @return status::OK if the deregistration was successful, otherwise
	 *         returns status::ERROR
	 *
	 * @throws GiapiException if there is an error accessing the GMP to
	 *         unregister to receive updates for the given epics status item.
	 */
	static int unsubscribeEpicsStatus(const std::string &name) throw (GiapiException);

	/**
	 * Offload wavefront corrections to the Primary Control System (PCS).
	 * These wavefront sensor updates are in the form of slowly changing
	 * set of Zernike coefficients.
	 *
	 * @param zernikes array of zernike coefficients
	 * @param size zernike coefficients' array size.
	 *
	 * @return status::OK if the zernikes were offloaded to the PCS
	 *         or status::ERROR if there was a problem in the offload
	 *
	 * @throws GiapiException if there is an error accessing the GMP to
	 *         post the PCS update
	 */
	static int postPcsUpdate(double zernikes[], int size) throw (GiapiException);

	/**
	 * Provides the TCS Context information at the time of the call.
	 * The TCS Context provides information about the TCS that
	 * are needed to perform WCS conversions.
	 *
	 * @param ctx Reference to the <code>TcsContext</code> structure.
	 *        The content of this structure will be filled up by
	 *        this call.
	 * @param timeout time in milliseconds to wait for the TCS context to be
	 *        retrieved. If not specified, the call will block until the
	 *        GMP replies back.
	 *
	 * @return status::OK if the TcsContext was filled up properly.
	 *         status::ERROR if there was an error getting the TcsContext
	 *
	 * @throws GiapiException if there is an error accessing the GMP to
	 *         obtain the TCS Context, or a timeout occurs.
	 */
	static int getTcsContext(TcsContext& ctx, long timeout) throw (GiapiException);

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
	static pEpicsStatusItem getChannel(const std::string &name, long timeout) throw (GiapiException);

private:
	GeminiUtil();
	virtual ~GeminiUtil();
};

}

#endif /*GEMINIINTERACTIONUTIL_H_*/
