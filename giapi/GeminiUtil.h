#ifndef GEMINIINTERACTIONUTIL_H_
#define GEMINIINTERACTIONUTIL_H_

#include <giapi/EpicsStatusHandler.h>
#include <giapi/giapi.h>

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
	 */
	static int subscribeEpicsStatus(const char *name,
			pEpicsStatusHandler handler);

	/**
	 * Unregister any handlers that might be associated to the given EPICS
	 * status item. 
	 * 
	 * @param name Name of the EPICS status item that no longer will be 
	 * monitored
	 * 
	 * @return status::OK if the deregistration was successful, otherwise
	 *         returns status::ERROR. 
	 */
	static int unsubscribeEpicsStatus(const char *name);

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
	 */
	static int postPcsUpdate(double zernikes[], int size);

	/**
	 * Provides the TCS Context information at the time of the call. 
	 * The TCS Context provides information about the TCS that 
	 * are needed to perform WCS conversions.
	 * 
	 * @param ctx Reference to the <code>TcsContext</code> structure. 
	 *        The content of this structure will be filled up by 
	 *        this call. 
	 * 
	 * @return status::OK if the TcsContext was filled up properly. 
	 *         status::ERROR if there was an error getting the TcsContext. 
	 */
	static int getTcsContext(TcsContext& ctx);

private:
	GeminiUtil();
	virtual ~GeminiUtil();
};

}

#endif /*GEMINIINTERACTIONUTIL_H_*/
