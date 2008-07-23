#
# Makefile for the GIAPI C++ Language Glue GMP Directory
#

#include sources list and objects built from there
-include sources.mk
-include ../config.mk
-include ../common.mk


#Includes to build
INC_DIRS := -I.. -I$(LOG4CXX_INCLUDE) -I$(ACTIVEMQ_INCLUDE) -I$(APR_INCLUDE)

# Tool invocations
all: $(OBJS)

clean:
	-$(RM) $(OBJS)$(CPP_DEPS)
	-@echo ' '
	