#Some common definitions for Making targets are here

#Include the configuration
-include conf/config.mk

# Compiler is always gcc for now
CXX := g++

RM := rm -rf

MKDIRHIER  := mkdir -p

LN := ln -s

CP := cp -f

CPDIR := cp -r

MAKE := make

MOVE := mv

#Common definition depending on the operating system
ifeq ($(shell uname | grep -c "Darwin"),1)
	OS := MacOS X
	MKLIB = $(CXX) -dynamiclib
else
	OS := Linux
	MKLIB = $(CXX) -shared -fPIC
endif

#Giapi Install directories
GIAPI_INCLUDE_BASE := $(INSTALL_DIR)/include
GIAPI_INCLUDE_DIR := $(GIAPI_INCLUDE_BASE)/giapi
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
ACTIVEMQ_INCLUDE := $(ACTIVEMQ_BASE)/include/activemq-cpp-3.4.1
# For OSX use 3.1.3
#ACTIVEMQ_INCLUDE := $(ACTIVEMQ_BASE)/include/activemq-cpp-3.1.3
ACTIVEMQ_LIB := $(ACTIVEMQ_BASE)/lib

#APR libs
APR_BASE:=$(EXTERNAL_LIB)/apr
APR_INCLUDE:= $(APR_BASE)/include/apr-1
APR_LIB:=$(APR_BASE)/lib

#cURLpp
CURLPP_BASE:=$(EXTERNAL_LIB)/curlpp
CURLPP_INCLUDE:= $(CURLPP_BASE)/include/
CURLPP_LIB:=$(CURLPP_BASE)/lib

#Extra distribution files to be included in the package for distribution
EXTRA_DIST_FILES := README RELEASE_NOTES

#Name to be used for the GIAPI distribution package
DIST_PACKAGE_NAME := giapi-dist

#Temporary directory to make distribution
TMP_DIST_DIR := /tmp/$(DIST_PACKAGE_NAME)

# Rule for building objects
%.o: %.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -g -O0 -Wall -fPIC -c -Wno-deprecated -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '
