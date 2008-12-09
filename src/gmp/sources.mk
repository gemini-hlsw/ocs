################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
OBJS += \
./src/gmp/ConnectionManager.o \
./src/gmp/GMPKeys.o \
./src/gmp/JmsUtil.o \
./src/gmp/SequenceCommandConsumer.o \
./src/gmp/CompletionInfoProducer.o

CPP_DEPS += \
./src/gmp/ConnectionManager.d \
./src/gmp/GMPKeys.d \
./src/gmp/JmsUtil.d \
./src/gmp/SequenceCommandConsumer.d \
./src/gmp/CompletionInfoProducer.d

# Each subdirectory must supply rules for building sources it contributes
src/gmp/%.o: ../src/gmp/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking $(OS) C++ Compiler'
	$(CXX) $(INC_DIRS) -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"../$(@:%.o=%.d)" -MT"../$(@:%.o=%.d)" -o"../$@" "$<"
	@echo 'Finished building: $<'
	@echo ' ' 
