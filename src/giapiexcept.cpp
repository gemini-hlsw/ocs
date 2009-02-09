#include <giapi/giapiexcept.h>

#include <stdarg.h>

#include <apr_strings.h>
#include <decaf/internal/AprPool.h>

using namespace decaf;
using namespace decaf::internal;

namespace giapi {

GiapiException::GiapiException() throw() {
}

GiapiException::GiapiException(const GiapiException & ex) throw() {
	*this = ex;
}

GiapiException::GiapiException(const std::string & message) throw () {
	_message = message;
}

GiapiException::~GiapiException() throw() {

}

void GiapiException::setMessage(const char * msg, ...) {
    va_list vargs;
    va_start( vargs, msg );
    buildMessage( msg, vargs );
    va_end( vargs );
}

void GiapiException::buildMessage(const char *format, va_list& args) {
    // Allocate buffer with a guess of it's size
    AprPool pool;

    // Allocate a buffer of the specified size.
    char* buffer = apr_pvsprintf( pool.getAprPool(), format, args );

    // Guessed size was enough. Assign the string.
    _message.assign( buffer, strlen( buffer ) );
}



CommunicationException::CommunicationException() throw() : GiapiException() {

}

CommunicationException::CommunicationException(const std::string & message) throw() :
	GiapiException(message){
}

GmpException::GmpException() throw() : CommunicationException() {
}

GmpException::GmpException(const std::string & message) throw() :
	CommunicationException(message) {
}



PostException::PostException() throw() : CommunicationException() {
}

PostException::PostException(const std::string & message) throw() :
	CommunicationException(message){
}

InvalidOperation::InvalidOperation() throw() : GiapiException() {
}

InvalidOperation::InvalidOperation(const std::string & message) throw() :
	GiapiException(message) {
}

TimeoutException::TimeoutException() throw() : GiapiException() {
}

TimeoutException::TimeoutException(const std::string & message) throw() :
	GiapiException(message) {

}



}
