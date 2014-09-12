IMPORTANT NOTE (fn 2012, Jan 13):
XSL files are stored with ending ".xml" because IDEA by default does not include xsl files into the resulting jar
file. Chaning this in the module settings is futile since we re-create the idea project/module files all the time
using the ant task. Only way around that would be to improve the ant task in a way to set the idea module up
properly but that seems to be a bit of an overkill right now.