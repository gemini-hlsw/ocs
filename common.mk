#Some common definitions for Making targets are here

#Include the configuration
-include config.mk

# Compiler is always gcc for now
CXX := g++

RM := rm -rf

MKDIRHIER  := mkdir -p

LN := ln -s

CP := cp -f

MAKE := make

#Common definition depending on the operating system
ifeq ($(shell uname | grep -c "Darwin"),1) 
	OS := MacOS X
	MKLIB = $(CXX) -dynamiclib
else
	OS := Linux
	MKLIB = $(CXX) -shared -fPIC
endif

#Giapi Install directories
GIAPI_INCLUDE_DIR := $(INSTALL_DIR)/include/giapi
GIAPI_LIB_DIR := $(INSTALL_DIR)/lib


#Log4cxx stuff
LOG4CXX_BASE := $(EXTERNAL_LIB)/log4cxx
LOG4CXX_INCLUDE := $(LOG4CXX_BASE)/include
LOG4CXX_LIB := $(LOG4CXX_BASE)/lib

#CPPUnit stuff
CPPUNIT_BASE := $(EXTERNAL_LIB)/cppunit
CPPUNIT_INCLUDE := $(CPPUNIT_BASE)/include
CPPUNIT_LIB := $(CPPUNIT_BASE)/lib

#ActiveMQ-cpp stuff
ACTIVEMQ_BASE := $(EXTERNAL_LIB)/activemq-cpp
ACTIVEMQ_INCLUDE := $(ACTIVEMQ_BASE)/include/activemq-cpp-2.2
ACTIVEMQ_LIB := $(ACTIVEMQ_BASE)/lib

#APR libs
APR_INCLUDE:= $(SYSTEM_INCLUDE_DIR)/apr-1


# Rule for building objects
%.o: %.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -fPIC -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '
