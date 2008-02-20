#
# Makefile for the GIAPI C++ Language Glue test code
#

#include sources list and objects built from there
-include sources.mk
-include ../common.mk


#Set the LD_LIBRARY_PATH to run the tests
ifneq ($(UNAME),Darwin)
	DYLD_LIBRARY_PATH := ../
else
	LD_LIBRARY_PATH := ../
endif

#Includes to build
INC_DIRS := -I../  -I$(CPPUNIT_INCLUDE) 
# Libraries
LIB_DIRS := -L$(CPPUNIT_LIB) -L$(LOG4CXX_LIB) -L$(GIAPI_LIB_DIR)
LIBS := -lcppunit -llog4cxx -lgiapi-glue-cc

EXE_NAME := libgiapi-test

# Tool invocations
all: libgiapi-test
	@ echo "Build Base $(DYLD_LIBRARY_PATH)"
	@ echo "Runnnin unit testing"
	@./$(EXE_NAME)

libgiapi-test: $(OBJS) 
	@echo 'Building target: $@'
	@echo 'Invoking: $(OS) C++ Linker'
	$(CXX) $(LIB_DIRS) -o $(EXE_NAME) $(OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '
	
clean:
	-$(RM) $(OBJS)$(CPP_DEPS)$(LIBRARIES) $(EXE_NAME)
	-@echo ' '
	
