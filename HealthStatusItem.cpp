#include "HealthStatusItem.h"

namespace giapi
{

HealthStatusItem::HealthStatusItem(const char* name)
: StatusItem(name, type::INT)
{
	
}

HealthStatusItem::~HealthStatusItem()
{
}

int HealthStatusItem::setHealth(health::Health health) {
	
	return StatusItem::setValueAsInt(health);
	
}

}
