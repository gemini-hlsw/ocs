FROM gcc:4.9
COPY . /home/software/giapi-glue-cc/external
USER root
# set common env variables for building
ENV GIAPI_ROOT=/home/software/giapi-glue-cc
ENV BOOST_ROOT=/home/software/giapi-glue-cc/external/boost

# Library Installation 
    # APR 
RUN cd $GIAPI_ROOT/external/apr-1.3.12 && \
    ./configure --prefix=$GIAPI_ROOT/external/apr && \
    make && make install && ln -s $GIAPI_ROOT/external/apr-1.3.12 apr-1 &&\
    # APR-util 
    cd $GIAPI_ROOT/external/apr-util-1.3.10 && \
    ./configure --with-apr=$GIAPI_ROOT/external/apr \
    --prefix=$GIAPI_ROOT/external/apr-util && \
    make && make install && \
    # ActiveMQ 
    cd $GIAPI_ROOT/external/activemq-cpp-library-3.4.1 && \
    ./configure --prefix=$GIAPI_ROOT/external/activemq-cpp \
    --with-apr=$GIAPI_ROOT/external/apr \ 
    -with-apr-util=$GIAPI_ROOT/external/apr-util --without-openssl && \
    make && make install && \
    # log4cxx 
    cd $GIAPI_ROOT/external/apache-log4cxx-0.10.0 && \
    ./configure --prefix=$GIAPI_ROOT/external/log4cxx \
    --with-apr=$GIAPI_ROOT/external/apr  \ 
    -with-apr-util=$GIAPI_ROOT/external/apr-util && \
    make && make install && \
    # cppunit 
    cd $GIAPI_ROOT/external/cppunit-1.12.1 && \
    ./configure --prefix=$GIAPI_ROOT/external/cppunit  && \
    make && make install && \
    # curlpp 
    cd $GIAPI_ROOT/external/curlpp-0.7.3 && \
    ./configure --prefix=$GIAPI_ROOT/external/curlpp --without-boost && \ 
    make && make install