# Wrapping Libraries For Use With OSGi

When a required third-party library isn't shipped with an OSGi-compatible
manifest, you can use BND to create an updated jar file with the necessary
header information.  This is documented here

	http://www.aqute.biz/Bnd/CommandLine

though some of the options don't seem to actually work.

With a bit of luck though, it may be quicker than manually creating
the missing MANIFEST information.  The procedure that worked for me
is:

````
cd <path-to-ocs-dist>/lib/bundle
java -jar ../tools/biz.aQute.bnd-3.1.0.jar wrap <path-to-raw-jar-file>
````
