#ifndef GIAPIERRORHANDLER_H_
#define GIAPIERRORHANDLER_H_

#include <tr1/memory>

namespace giapi {

/**
 * GIAPI Error handlers are registered in the GIAPI through the GIAPI Util
 * class. In case of a communication problem with the GMP, the GIAPI
 * automatically will attempt to re-establish the communication. Once
 * the communication is re-established, all the registered handlers will
 * be invoked.
 * <p/>
 * Handlers are useful to perform operations that will need to be re-executed
 * in case of a communication failure and the GMP needs to be restarted. For
 * instance, the subscription to different sequence commands, EPICS status items
 * are a few examples.
 */
class GiapiErrorHandler {
public:

	/**
	 * This method will be called when there is a problem with the GIAPI
	 * trying to communicate with the GMP.
	 * </p>
	 * Implementations of this class will put in this call all the activities
	 * that will need to be performed in case there is a problem with the GMP
	 * especially if the GMP needs to be restarted.
	 */
	virtual void onError() = 0;

	virtual ~GiapiErrorHandler();
};

/**
 * Smart pointer definition for GIAPI Error handlers.
 */
typedef std::tr1::shared_ptr<GiapiErrorHandler> pGiapiErrorHandler;


}

#endif /* GIAPIERRORHANDLER_H_ */
