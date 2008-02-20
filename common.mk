#Some common definitions for Making targets are here

# Compiler is always gcc for now
CXX := g++

RM := rm -rf

MKDIRHIER  := mkdir -p

LN := ln -s

CP := cp -f

#Common definition depending ton the operating system
ifneq ($(UNAME),Darwin)
	OS := MacOS X
	MKLIB = $(CXX) -dynamiclib
else
	OS := Linux
	MKLIB = $(CXX) -shared
endif

#Installation directory
INSTALL_DIR := /Users/anunez/test/giapi
GIAPI_INCLUDE_DIR := $(INSTALL_DIR)/include/giapi
GIAPI_LIB_DIR := $(INSTALL_DIR)/lib

#Log4cxx stuff
LOG4CXX_BASE := /Users/anunez/Projects/eclipse/workspaces/apache-log4cxx/dist
LOG4CXX_INCLUDE := $(LOG4CXX_BASE)/include
LOG4CXX_LIB := $(LOG4CXX_BASE)/lib

#CPPUnit stuff
CPPUNIT_BASE := /Users/anunez/Projects/extra/cppunit-1.12.0/dist
CPPUNIT_INCLUDE := $(CPPUNIT_BASE)/include
CPPUNIT_LIB := $(CPPUNIT_BASE)/lib

# Rule for building objects
%.o: %.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '
