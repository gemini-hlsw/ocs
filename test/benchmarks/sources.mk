
-include status-benchmark/sources.mk

OBJS += $(patsubst %.cpp,%.o,$(wildcard ./*.cpp))

CPP_DEPS += $(patsubst %.cpp,%.d,$(wildcard ./*.cpp))
