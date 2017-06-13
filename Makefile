#
# Makefile for the GIAPI C++ Language Glue code
#

#include sources list and objects built from there
-include conf/common.mk
-include src/sources.mk

#Version and minor version
V := 0
MV := 16

LIBRARY_NAME := libgiapi-glue-cc

ifeq ($(shell uname | grep -c "Darwin"),1)
	LIBRARY_NAME_LN := $(LIBRARY_NAME).dylib
	LIBRARY_NAME := $(LIBRARY_NAME).$(V).$(MV).dylib
else
	LIBRARY_NAME_LN := $(LIBRARY_NAME).so
	LIBRARY_NAME := $(LIBRARY_NAME).so.$(V).$(MV)
endif


# This directory things
INC_DIRS := -I. -I./external -I./src -I$(LOG4CXX_INCLUDE) -I$(ACTIVEMQ_INCLUDE) -I$(APR_INCLUDE)

# Directory for libraries
LIB_DIRS := -L$(LOG4CXX_LIB) -L$(ACTIVEMQ_LIB) -L$(APR_LIB)

# Libraries
LIBS := -llog4cxx -lactivemq-cpp -lapr-1

# Sub-directories
SUBDIRS :=  test

# All Target
all: libgiapi-glue-cc

# Test target
test: libgiapi-glue-cc
	@for subdir in $(SUBDIRS); \
        do \
          echo "Making $@ in $$subdir ..."; \
          ( cd $$subdir && $(MAKE) ) || exit 1; \
        done

# Install Target
install: libgiapi-glue-cc install-shared-lib install-headers

# Tool invocations
libgiapi-glue-cc: $(OBJS)
	@echo 'Building on $(UNAME)'
	@echo 'Building target: $@'
	@echo 'Invoking: $(OS) C++ Linker'
	$(MKLIB) $(LIB_DIRS) -o $(LIBRARY_NAME) $(OBJS) $(LIBS)
	@echo 'Finished building target: $@'
	@echo 'Making symlink'
	-$(RM) $(LIBRARY_NAME_LN)
	$(LN) $(LIBRARY_NAME) $(LIBRARY_NAME_LN)
	@echo ' '


clean:
	-$(RM) $(OBJS) $(CPP_DEPS) $(LIBRARIES) $(LIBRARY_NAME) $(LIBRARY_NAME_LN)
	-@echo ' '
	@for subdir in $(SUBDIRS); \
        do \
          echo "Making $@ in $$subdir ..."; \
          ( cd $$subdir && $(MAKE) $@ ) || exit 1; \
        done


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

#Make distribution directory
make-dist-dir:
	@ if [ ! -d $(DISTRIBUTION_DIR) ] ; then \
         echo "Creating: $(DISTRIBUTION_DIR)"; \
         $(MKDIRHIER) $(DISTRIBUTION_DIR); \
         chmod 755 $(DISTRIBUTION_DIR); \
    fi


make-tmp-dist-dir:
	@ $(RM) -f $(TMP_DIST_DIR)
	@ $(MKDIRHIER) $(TMP_DIST_DIR)
	@ chmod 755 $(TMP_DIST_DIR)


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

dist: install make-dist-dir make-tmp-dist-dir
	@ echo "Preparing distribution package... please wait."
	@ $(CPDIR) $(GIAPI_LIB_DIR) $(TMP_DIST_DIR)
	@ $(CPDIR) $(GIAPI_INCLUDE_BASE) $(TMP_DIST_DIR)
	@ $(CP) $(EXTRA_DIST_FILES) $(TMP_DIST_DIR)
	@ $(CP) -P $(LOG4CXX_LIB)/lib* $(TMP_DIST_DIR)/lib
	@ $(CP) -P $(ACTIVEMQ_LIB)/lib* $(TMP_DIST_DIR)/lib
	@ $(CP) -P $(APR_LIB)/lib* $(TMP_DIST_DIR)/lib
	@ tar -C /tmp -zcf $(DISTRIBUTION_DIR)/$(DIST_PACKAGE_NAME)-${V}.${MV}.tgz $(DIST_PACKAGE_NAME)
	@ $(RM) $(TMP_DIST_DIR)
	@ echo "Distribution package generated at $(DISTRIBUTION_DIR)/$(DIST_PACKAGE_NAME)-${V}.${MV}.tgz"

#Rule to copy the public headers.
$(GIAPI_INCLUDE_DIR)/%h : giapi/%h
	@ echo "Installing include file: $<"
	@ $(CP) $< $@
