#
# Makefile for the GIAPI C++ Language Glue test code
#

#include sources list and objects built from there
-include sources.mk
-include ../common.mk
# Compiler is always gcc for now


#The DYLD_LIBRARY_PATH so we can run the executable 
DYLD_LIBRARY_PATH := $(GIAPI_LIB_DIR)

#Includes to build
INC_DIRS := -I../  -I$(CPPUNIT_INCLUDE) 
# Libraries
LIB_DIRS := -L$(CPPUNIT_LIB) -L$(LOG4CXX_LIB) -L$(GIAPI_LIB_DIR)
LIBS := -lcppunit -llog4cxx -lgiapi-glue-cc

EXE_NAME := libgiapi-test

# Tool invocations
libgiapi-test: $(OBJS) 
	@echo 'Building target: $@'
	@echo 'Invoking: $(OS) C++ Linker'
	$(CXX) $(LIB_DIRS) -o $(EXE_NAME) $(OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '
	
run-test: libgiapi-test
	@ echo "Runnnin unit testing"
	@./$(EXE_NAME)

clean:
	-$(RM) $(OBJS)$(CPP_DEPS)$(LIBRARIES) $(EXE_NAME)
	-@echo ' '
	
