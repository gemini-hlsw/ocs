#include "PropertiesUtil.h"
#include "StringUtil.h"
#include <cstdlib>
#include <log4cxx/helpers/exception.h>
#include <log4cxx/helpers/fileinputstream.h>

namespace giapi {
namespace util{
log4cxx::LoggerPtr PropertiesUtil::logger(log4cxx::Logger::getLogger("giapi.util.PropertiesUtil"));
    PropertiesUtil& PropertiesUtil::Instance(){
        static PropertiesUtil _singleton;
        char *configName = std::getenv("GMP_CONFIGURATION");
        std::string fileName;
        if(configName!=NULL){
            fileName=configName;
        }
        if(StringUtil::isEmpty(fileName)){
            fileName = "gmp.properties";
        }
        _singleton.load(fileName);
        return _singleton;
    }

    void PropertiesUtil::load(const std::string& fileName){
        try {
            log4cxx::helpers::InputStreamPtr inputStream = new log4cxx::helpers::FileInputStream(log4cxx::File(fileName));
            properties.load(inputStream);
        } catch(const log4cxx::helpers::IOException& ie) {
            LOG4CXX_WARN(logger, std::string("Could not read configuration file [") + fileName + std::string("]. Using defaults. Please set the environment variable GMP_CONFIGURATION to point to your configuration file, or place a gmp.properties file in the current directory."));
            return;
        }
    } 

    log4cxx::LogString PropertiesUtil::getProperty(const log4cxx::LogString& propName){
        return properties.getProperty(propName);
    }
    PropertiesUtil::~PropertiesUtil(){}
    PropertiesUtil::PropertiesUtil(){}
    PropertiesUtil::PropertiesUtil(PropertiesUtil const &){}
//    PropertiesUtil& PropertiesUtil::operator=(PropertiesUtil const &){}
}
}
