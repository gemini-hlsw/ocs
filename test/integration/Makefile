#
# Makefile for the GIAPI C++ Language Glue benchmark code
#

#include sources list and objects built from there
-include sources.mk
-include ../../conf/config.mk
-include ../../conf/common.mk


#Set the LD_LIBRARY_PATH to run the tests
LD_LIBRARY_PATH := ../../:$(LOG4CXX_LIB):$(CPPUNIT_LIB):$(ACTIVEMQ_LIB):$(APR_LIB)

#Includes to build
INC_DIRS := -I. -I../.. -I$(CPPUNIT_INCLUDE) 
# Libraries
LIB_DIRS := -L$(CPPUNIT_LIB) -L$(LOG4CXX_LIB) -L$(ACTIVEMQ_LIB) -L$(APR_LIB) -L../../
LIBS := -lcppunit -lgiapi-glue-cc -llog4cxx -lactivemq-cpp -lapr-1

EXE_NAME := libgiapi-integration

# Tool invocations
all: libgiapi-integration
	@ echo "Running integration tests"
	@ sh runtests.sh $(LD_LIBRARY_PATH)
	
libgiapi-integration: $(OBJS) 
	@echo 'Building target: $@'
	@ echo "X  = $(X)"
	@echo 'Invoking: $(OS) C++ Linker'
	$(CXX) $(LIB_DIRS) -o $(EXE_NAME) $(OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo ' '

clean:
	-$(RM) $(OBJS) $(CPP_DEPS) $(LIBRARIES) $(EXE_NAME)
	-@echo ' '
	
%.o: %.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' ' 
