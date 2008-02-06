#include "ServicesUtilImpl.h"

namespace giapi {

log4cxx::LoggerPtr ServicesUtilImpl::logger(log4cxx::Logger::getLogger("giapi.ServicesUtilImpl"));

std::auto_ptr<ServicesUtilImpl> ServicesUtilImpl::INSTANCE(new ServicesUtilImpl());


ServicesUtilImpl::ServicesUtilImpl() {
}

ServicesUtilImpl::~ServicesUtilImpl() {
}

}
