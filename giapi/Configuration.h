#ifndef CONFIGURATION_H_
#define CONFIGURATION_H_
#include <tr1/memory>

namespace giapi
{

class Configuration
{
public:
	Configuration();
	virtual ~Configuration();
};

typedef std::tr1::shared_ptr<Configuration> pConfiguration;
}

#endif /*CONFIGURATION_H_*/
