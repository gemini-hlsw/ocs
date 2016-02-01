## Seqexec Web Server

This bundle contains a web-based seqexec client. It is intended to run in the same process as the seqexec-server

### How to compile scala.js and reload the server

Go to the bundle project

```
    project bundle_edu_gemini_seqexec_web_server
    ~re-start
```

Now every time a file is changed, scala.js will compile the javascript files, and the server files will be compile by scalac, then the server will restart

### How to run the server with live reloading

Go to the bundle project

```
    project bundle_edu_gemini_seqexec_web_server
    ~re-start
```

Now every time a file is changed the files will be compiled and the server restarted

## TODO

* 
* Support websockets
* Authentication
* Review the decision to use http4s
* Support OSGi
