#include "giapi/giapiexcept.h" 

namespace giapi {
PostException::PostException() :
	runtime_error("PostException") {
}

PostException::PostException(const char * message) :
	runtime_error(message) {

}

//const char * PostException::what() const throw() {
//	return "giapi::post_exception: "
//		"failed while attempting to post event";
//}

}