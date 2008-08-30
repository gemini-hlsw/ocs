#include <giapi/giapiexcept.h> 

namespace giapi {

GiapiException::GiapiException() throw() {
}

GiapiException::GiapiException(const GiapiException & ex) throw() {
	*this = ex;
}

GiapiException::~GiapiException() throw() {
	
}

PostException::PostException() : GiapiException() {
	
}

PostException::PostException(const std::string & message) {

}

InvalidOperation::InvalidOperation() :
	logic_error("Invalid Operation") {
}

InvalidOperation::InvalidOperation(const std::string & message) :
	logic_error(message) {
	
}


}
