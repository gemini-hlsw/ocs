#include "giapi/giapiexcept.h" 

namespace giapi {
PostException::PostException() :
	runtime_error("PostException") {
}

PostException::PostException(const std::string & message) :
	runtime_error(message) {

}

InvalidOperation::InvalidOperation() :
	logic_error("Invalid Operation") {
}

InvalidOperation::InvalidOperation(const std::string & message) :
	logic_error(message) {
	
}


}