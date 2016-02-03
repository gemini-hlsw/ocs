# Seqexec Web Server

This bundle contains a web-based seqexec client and server. It is intended to run in the same process as the seqexec-server

## Project Structure

The project is done using [Scala.js]()

### How to compile and reload the server

Go to the bundle project

```
    project bundle_edu_gemini_seqexec_web_server
    ~ ;fastOptJS ;re-start
```

Now every time a file is changed, scala.js will compile the javascript files, the server files will be compiled by scalac, then the server will restart

### How to run the server with live reloading

Go to the bundle project

```
    project bundle_edu_gemini_seqexec_web_server
    ~re-start
```

Now every time a file is changed the files will be compiled and the server restarted

## Libraries summary

The project requires quite a bit of new libraries. Here is a summary of them and the reason to use them:

* uPiclke

## TODO

* Package the application compressing with fullOptJS
* Load javascript files from webjars
* Support websockets
* Authentication
* Review the decision to use http4s
* Support OSGi
