# Seqexec Web Server

This bundle contains a web-based seqexec client and server. It is intended to run in the same process as the seqexec-server

## Project Structure

The project is done using [Scala.js](http://www.scala-js.org/) to generate JavaScript out of Scala code. This requires a very specific structure to support cross compilation of the project. There are 3 directories on this bundle:

* jvm: Contains code target only to the JVM, i.e. class files. It includes the server side of the application
* js: Contains code that is compiled to JS, essentially the UI
* shared: Some classes need to be used by both **jvm** and **js**. Those are located here. In particular this includes model classes

## sbt plugins

This project requires 2 new plugins

* [scala-js](http://www.scala-js.org/doc/sbt-plugin.html): This plugins brings the support for scala.js compilation into sbt. Using the compiler we can take the Scala code and 
* [sbt-revolver](https://github.com/spray/sbt-revolver): This plugin allows to restart the web server and trigger a recompilation when the source code changes. I consider it essential for web development as it lets you rebuild as soon as files are changed

## How to compile and start the server

Go to the JVM project

```
    project bundle_edu_gemini_seqexec_web_serverJVM
    ~re-start
```

Now every time a file is changed in either the server or the client, scala.js will compile the javascript files, the server files will be compiled by scalac, then the server will restart

The page can be now reached at

http://localhost:9090

## Libraries summary

The project requires quite a bit of new libraries. Here is a summary of them and the reason to use them:

* uPiclke

## TODO

* Integrate router capability
* Package the application optimizing the output with fullOptJS 
* Package all the js dependencies in a single js file
* Load javascript files from webjars
* Support websockets
* Authentication
* Review the decision to use http4s
* Support OSGi
