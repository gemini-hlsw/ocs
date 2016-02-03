# Seqexec Web Server

This bundle contains a web-based seqexec client and server. It is intended to run in the same process as the seqexec-server

## Project Structure

The project is done using [Scala.js](http://www.scala-js.org/) to generate JavaScript out of Scala code. This requires a very specific structure to support cross compilation of the project. There are 3 directories on this bundle:

* jvm: Contains code target only to the JVM, i.e. class files. It includes the server side of the application
* js: Contains code that is compiled to JS, essentially the UI
* shared: Some classes need to be used by both **jvm** and **js**. Those are located here. In particular this includes model classes

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
