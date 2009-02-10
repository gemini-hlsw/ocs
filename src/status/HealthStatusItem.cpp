#include "HealthStatusItem.h"

namespace giapi
{

HealthStatusItem::HealthStatusItem(const std::string &name)
: StatusItem(name, type::INT)
{

}

HealthStatusItem::~HealthStatusItem()
{
}

int HealthStatusItem::setHealth(health::Health health) {

	return setValueAsInt(health);

}

void HealthStatusItem::accept(StatusVisitor & visitor) {
	visitor.visitHealthItem(this);
}

}
