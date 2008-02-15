#
# Makefile for the GIAPI C++ Language Glue code
#

#include sources list and objects built from there
-include sources.mk

# Compiler is always gcc for now
CXX := g++

RM := rm -rf

MKDIRHIER  := mkdir -p

LN := ln -s

CP := cp -f

#Version and minor version
V := 0
MV := 1.0

LOG4CXX_BASE := /Users/anunez/Projects/workspaces/external/apache-log4cxx/dist
LOG4CXX_INCLUDE := $(LOG4CXX_BASE)/include
LOG4CXX_LIB := $(LOG4CXX_BASE)/lib

INSTALL_DIR := /Users/anunez/test/giapi
GIAPI_INCLUDE_DIR := $(INSTALL_DIR)/include/giapi
GIAPI_LIB_DIR := $(INSTALL_DIR)/lib



LIBRARY_NAME := libgiapi-glue-cc

ifneq ($(UNAME),Darwin)
	OS := MacOS X
	MKLIB = $(CXX) -dynamiclib 
	LIBRARY_NAME_LN := $(LIBRARY_NAME).dylib
	LIBRARY_NAME := $(LIBRARY_NAME).dylib.$(V).$(MV)
else
	OS := Linux
	MKLIB = $(CXX) -shared
	LIBRARY_NAME_LN := $(LIBRARY_NAME).so
	LIBRARY_NAME := $(LIBRARY_NAME).so.$(V).$(MV)
endif


# This directory things
INC_DIRS := -I. -I./external -I$(LOG4CXX_INCLUDE) 

# Libraries
LIB_DIRS := -L$(LOG4CXX_LIB)

LIBS := -llog4cxx

# Rule for building objects
%.o: %.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


# All Target
all: libgiapi-glue-cc 

# Install Target
install: libgiapi-glue-cc install-shared-lib install-headers

# Tool invocations
libgiapi-glue-cc: $(OBJS) 
	@echo 'Building target: $@'
	@echo 'Invoking: $(OS) C++ Linker'
	$(MKLIB) $(LIB_DIRS) -o $(LIBRARY_NAME) $(OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '
	
	
clean:
	-$(RM) $(OBJS)$(CPP_DEPS)$(LIBRARIES) $(LIBRARY_NAME)
	-@echo ' '
	
	
#Make include directory	
make-include-dir:
	@ if [ ! -d $(GIAPI_INCLUDE_DIR) ] ; then \
		echo "Creating: $(GIAPI_INCLUDE_DIR)"; \
		$(MKDIRHIER) $(GIAPI_INCLUDE_DIR); \
		chmod 755 $(GIAPI_INCLUDE_DIR); \
	fi

#Make library directory
make-lib-dir:
	@ if [ ! -d $(GIAPI_LIB_DIR) ] ; then \
         echo "Creating: $(GIAPI_LIB_DIR)"; \
         $(MKDIRHIER) $(GIAPI_LIB_DIR); \
         chmod 755 $(GIAPI_LIB_DIR); \
    fi

install-shared-lib: libgiapi-glue-cc make-lib-dir
	@ echo "Installing $(LIBRARY_NAME)"
	@ $(CP) $(LIBRARY_NAME) $(GIAPI_LIB_DIR)/$(LIBRARY_NAME)
	-$(RM) $(GIAPI_LIB_DIR)/$(LIBRARY_NAME_LN)
	cd $(GIAPI_LIB_DIR); \
    $(LN) $(LIBRARY_NAME) $(LIBRARY_NAME_LN)

#Get the public headers in GIAPI
GIAPI_PUBLIC_HEADERS = $(wildcard giapi/*.h)

#Construct a list with the full name of the public headers
INC_TARGETS= $(GIAPI_PUBLIC_HEADERS:giapi/%.h=$(GIAPI_INCLUDE_DIR)/%.h)	

install-headers: make-include-dir $(INC_TARGETS) 
	
#Rule to copy the public headers. 
$(GIAPI_INCLUDE_DIR)/%h : giapi/%h
	@ echo "Installing include file: $<"
	@ $(CP) $< $@
