package edu.gemini.tools.p1pdfmaker;

import edu.gemini.model.p1.pdf.P1PDF;
import org.apache.commons.cli.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import scala.collection.JavaConversions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class P1PdfMaker {

    private static final Map<String, P1PDF.Template> templatesMap;
    private static final StringBuffer partnerNames;
    static {
        // get templates lookup map
        templatesMap = JavaConversions.mapAsJavaMap(P1PDF.templatesMap());

        // create list of valid partner/country names
        partnerNames = new StringBuffer();
        int i = 0;
        for (scala.Tuple2<String, P1PDF.Template> key : JavaConversions.seqAsJavaList(P1PDF.templatesList())) {
            partnerNames.append(key._1());
            if (i++ < templatesMap.size() - 1) {
                partnerNames.append(", ");
            }
        }
    }

    private static final Option OPT_RECURSIVE;
    private static final Option OPT_STYLESHEET;
    private static final Option OPT_XML_SOURCE;
    private static final Option OPT_PDF_DEST;
    static {
        OptionBuilder.withLongOpt("recursive");
        OptionBuilder.withDescription("recurse into subdirectories");
        OptionBuilder.hasArg();
        OPT_RECURSIVE = OptionBuilder.create("r");

        OptionBuilder.withLongOpt("partner");
        OptionBuilder.withDescription("partner or country name, one of: " + partnerNames);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired();
        OPT_STYLESHEET = OptionBuilder.create("c");

        OptionBuilder.withLongOpt("xmlin");
        OptionBuilder.withDescription("input file or a folder");
        OptionBuilder.withType(File.class);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired();
        OPT_XML_SOURCE = OptionBuilder.create("x");

        OptionBuilder.withLongOpt("pdfout");
        OptionBuilder.withDescription("output folder for resulting pdf file(s)");
        OptionBuilder.withType(File.class);
        OptionBuilder.hasArg();
        OPT_PDF_DEST = OptionBuilder.create("p");
    }

     private enum Opt {
        recursive(OPT_RECURSIVE),
        stylesheet(OPT_STYLESHEET),
        xmlsource(OPT_XML_SOURCE),
        pdfdestination(OPT_PDF_DEST);

        public final Option option;
        Opt(Option option) {
            this.option = option;
        }

        private static Options options() {
            Options options = new Options();
            for (Opt o : Opt.values()) {
                options.addOption(o.option);
            }
            return options;
        }

        private static String stylesheet(CommandLine commandLine) {
            return commandLine.getOptionValue(stylesheet.option.getOpt());
        }
        private static File xmlsource(CommandLine commandLine) throws ParseException {
            return (File) commandLine.getParsedOptionValue(xmlsource.option.getOpt());
        }
        private static File pdfdest(CommandLine commandLine) throws ParseException {
            return (File) commandLine.getParsedOptionValue(pdfdestination.option.getOpt());
        }
        private static boolean recursive(CommandLine commandLine) {
            return commandLine.hasOption(recursive.option.getOpt());
        }
    }

    /**
     * A simple main method that can be called by the bundle activator.
     * Assumes that the expected command line args are passed in as properties.
     * Translates the properties to "fake" command line args and passes them on to the "real" main.
     */
    public static void main(BundleContext context) throws BundleException {
        try {
            // translate properties to command line args
            List<String> args = new ArrayList<>();
            for (Opt o : Opt.values()) {
                String value = context.getProperty(o.option.getOpt());
                if (value == null) {
                    value = context.getProperty(o.option.getLongOpt());
                }
                if (value != null) args.add("-"+o.option.getLongOpt()+"="+value);
            }

            // call the actual main
            main(args.toArray(new String[args.size()]));

        } catch (Throwable t) {
            System.err.println("error: " + t);
            t.printStackTrace(System.err);

        } finally {
            // stopping bundle 0 will bring down the whole OSGi shebang
            context.getBundle(0).stop();
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] args) {
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(Opt.options(), args);
            P1PDF.Template template = template(line);
            File outputFolder = outputFolder(line);
            for (File xmlFile : filesToProcess(line)) {
                // create pdf file name from xml file name and do transformation
                // REL-663 Output pdf filename should have the _summary token appended
                File pdfFile = new File(outputFolder, xmlFile.getName().replace(".xml", "_summary.pdf"));
                P1PDF.createFromFile(xmlFile, template, pdfFile);
            }

        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            printHelp();
        }
    }

    private static P1PDF.Template template(CommandLine line) throws ParseException {
        String partner = Opt.stylesheet(line);
        if (!templatesMap.containsKey(partner)) {
            throw new ParseException("invalid template name");
        }
        return templatesMap.get(partner);
    }

    private static File outputFolder(CommandLine line) throws ParseException {
        File pdf =  Opt.pdfdest(line);
        if (!pdf.exists()) {
            throw new ParseException("pdf destination directory does not exist");
        }
        if (!pdf.isDirectory()) {
            throw new ParseException("pdf destination must be a directory");
        }
        if (!pdf.canWrite()) {
            throw new ParseException("pdf destination directory must be writable");
        }
        return pdf;
    }

    private static List<File> filesToProcess(CommandLine line) throws ParseException {
        List<File> xmls = new ArrayList<>();
        File xml = Opt.xmlsource(line);
        if (!xml.exists()) {
            throw new ParseException("xml source file or folder does not exist");
        }
        if (!xml.canRead()) {
            throw new ParseException("xml source file or folder must be readable");
        }
        xmls.addAll(getXmlFiles(xml, Opt.recursive(line)));
        if (xmls.size() == 0) {
            throw new ParseException("xml source file is not an xml file or directory does not contain any xml files");
        }
        return xmls;
    }

    private static List<File> getXmlFiles(File xmlsource, boolean recursive) {
        List<File> files = new ArrayList<>();
        if (xmlsource.isFile()) {
            // single file
            files.add(xmlsource);
        } else {
            // read all files from directory
            for (File f : xmlsource.listFiles()) {
                if (f.isDirectory() && recursive) {
                    files.addAll(getXmlFiles(f, true));
                } else {
                    if (f.getName().endsWith("xml")) {
                        files.add(f);
                    }
                }
            }
        }
        return files;
    }

    private static void printHelp() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("p1pdfmaker", Opt.options());
    }

}
