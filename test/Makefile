#
# Makefile for the GIAPI C++ Language Glue test code
#

#include sources list and objects built from there
-include ../conf/config.mk
-include ../conf/common.mk


# Tool invocations
all: unit

SUBDIRS = unit benchmarks integration

unit::  
	@-cd unit; make

benchmarks::  
	@-cd benchmarks; make

integration::  
	@-cd integration; make

dummy:: 
	echo $(shell find $(SUBDIRS) -name *.o)
	echo $(wildcard **/*.o)
clean:
	@for subdir in $(SUBDIRS); \
	do \
		echo "Cleaning $$subdir ..."; \
		(cd $$subdir && make clean ) || exit 1; \
	done
	
