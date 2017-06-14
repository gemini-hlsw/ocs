# Configure

```
export CXXFLAGS="-I$GIAPI_ROOT/external/libcurl/include -L$GIAPI_ROOT/external/libcurl/lib"
 ./configure --prefix=$GIAPI_ROOT/external/curlpp --without-boost
``  `

# Build

```
make && make install
```
