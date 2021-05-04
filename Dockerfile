FROM giapi-glue-external:R15
COPY . /home/software/giapi-glue-cc
# set common env variables for building
ENV GIAPI_ROOT=/home/software/giapi-glue-cc
ENV BOOST_ROOT=/home/software/giapi-glue-cc/external/boost
RUN sed 's/INSTALL.*/INSTALL_DIR := \/home\/software\/giapi-glue-cc\/install/g' /home/software/giapi-glue-cc/conf/config.mk > /home/software/giapi-glue-cc/conf/config2.mk
RUN  sed 's/EXTERNAL_LIB.*/EXTERNAL_LIB := \/home\/software\/giapi-glue-cc\/external/g' /home/software/giapi-glue-cc/conf/config2.mk > /home/software/giapi-glue-cc/conf/config3.mk
RUN sed 's/DISTRIBUTION_DIR.*/DISTRIBUTION_DIR := \/home\/softwaret\/giapi-glue-cc\/dist/g' /home/software/giapi-glue-cc/conf/config3.mk > /home/software/giapi-glue-cc/conf/config.mk
WORKDIR /home/software/giapi-glue-cc
# build lib giapi-glue        
RUN make && make install && \ 
    echo ${GIAPI_ROOT}/external/apr/lib >> /etc/ld.so.conf && \
    echo ${GIAPI_ROOT}/external/apr-util/lib >> /etc/ld.so.conf && \
    echo ${GIAPI_ROOT}/external/activemq-cpp/lib >> /etc/ld.so.conf  && \
    echo ${GIAPI_ROOT}/external/log4cxx/lib >> /etc/ld.so.conf  && \
    echo ${GIAPI_ROOT}/external/cppunit/lib >> /etc/ld.so.conf  && \
    echo ${GIAPI_ROOT}/external/curlpp/lib >> /etc/ld.so.conf  && \
    echo ${GIAPI_ROOT}/install/lib >> /etc/ld.so.conf && \
    ldconfig

ENTRYPOINT ["/bin/sh", "-c", "while true; do sleep 1000; done"]
