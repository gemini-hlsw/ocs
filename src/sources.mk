################################################################################
# Automatically-generated file. Do not edit!
################################################################################
-include src/services/sources.mk
-include src/gmp/sources.mk


# Add inputs and outputs from these tool invocations to the build variables 
OBJS += $(patsubst %.cpp,%.o,$(wildcard ./src/*.cpp))

CPP_DEPS += $(patsubst %.cpp,%.d,$(wildcard ./src/*.cpp))



