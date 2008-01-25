#ifndef GIAPI_H_
#define GIAPI_H_

/**
 * GIAPI Status. Used for GIAPI calls to report status of the 
 * different library calls to the invoker. 
 */
namespace giapi {
	namespace status {
		typedef enum {
			GIAPI_NOK = -1, //Errors that don't have a status
			GIAPI_OK = 0, //Success
			GIAPI_WARNING, //Some warning condition
			GIAPI_INVALIDOBJ, //An object is invalid
			GIAPI_INVALIDARG, //An argument is bad
			GIAPI_CONVERT_ERROR, //An argument failed to convert
			GIAPI_NOMEMORY, // No memory available
			GIAPI_POST_ERROR, //Failed to post
			GIAPI_NOTFOUND, //Something was not found
			GIAPI_FOUND //Status: Something was found. 
		} GiapiStatus;
	}
}

#endif /*GIAPI_H_*/

