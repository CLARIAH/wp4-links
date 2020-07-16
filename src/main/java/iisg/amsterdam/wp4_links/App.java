package iisg.amsterdam.wp4_links;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

/**

The MIT License (MIT)

Copyright (c) 2014, 2015, 2016 Dylon Edwards <dylon.devo+github@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE

 **/



public class App 
{
	@Parameter(names = "--function")
	String function = null;

	@Parameter(names = "--maxLev")
	int maxLev = 99;

	@Parameter(names = "--inputData")
	String inputData = null;

	@Parameter(names = "--outputDir")
	String outputDir = null;

	@Parameter(names = "--format")
	String format = "RDF"; // or "CSV"

	@Parameter(names = "--help", help = true)
	boolean help;

	@Parameter(names = "--debug")
	String debug = "error";


	public static final Logger lg = LogManager.getLogger(App.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);

	public static void main(String[] argv) {
		App main = new App();
		JCommander.newBuilder()
		.addObject(main)
		.build()
		.parse(argv);
		main.run();
	}

	public void run() {

		// default option is to show only errors 
		// BasicConfigurator.configure();
		Configurator.setRootLevel(Level.ERROR);
		
		LOG.outputConsole("PROGRAM STARTED!!");
		LOG.outputConsole("-----------------");
		LOG.outputConsole("");
		long startTime = System.currentTimeMillis();


		if(help == false) {
			// show only error and warning logs if user enters: --debug warn
			if(debug.equals("warn")) { 
				Configurator.setRootLevel(Level.ERROR);
			}
			// show all type of logs if user enters: --debug all
			if(debug.equals("all")) { 
				Configurator.setRootLevel(Level.DEBUG);
			}
			Controller cntrl = new Controller(function, maxLev, inputData, outputDir, format);
			cntrl.runProgram();
		} else { 
			// do not run program and show some help message if user enter: --help	
			System.out.println("The following parameters can be provided as input for the linkage tool: \n" 
					+ "--function (required): One of the 6 following functionalities: [showDatasetStats, convertToHDT, linkNewbornToPartner, linkPartnerToPartner, linkSiblings, linkMarriageParentsToMarriageParents]\n" 
					+ "--inputData (required for all functions): Path to the HDT dataset\n" 
					+ "--outputDir (required for all functions, except for \"showDatasetStats\"): Path to the directory for saving the indices and the links\n" 
					+ "--maxLev (required for all functions, except \"showDatasetStats\" and \"convertToHDT\"): Integer between 0 and 5, specifying the maximum Levenshtein distance allowed\n" 
					+ "--format (optional for all functions, except \"showDatasetStats\" and \"convertToHDT\"): One of the two Strings: RDF (default) or CSV, specifying the desired format to save the links between certificates\n"  
					+ "--debug (optional for all functions): One of the two Strings: error (default, showing only errors in console that occurred in the matching), all (showing every warning in console"
					+ "\n \n"
					+ "The current version has six functionalities, specified by the user using --function [functionalityName]: \n"
					+ "--function convertToHDT: convert an RDF file to an HDT file\n" 
					+ "--function showDatasetStats: show in console some general stats about the input HDT dataset\n" 
					+ "--function linkNewbornToPartner: link newborns in Birth Certificates to brides/grooms in Marriage Certificates\n" 
					+ "--function linkPartnerToPartner: link parents of brides/grooms in Marriage Certificates to brides and grooms in Marriage Certificates\n" 
					+ "--function linkSiblings: link parents of newborns in Birth Certificates to parents of newborns in Birth Certificates (for detecting siblings)\n" 
					+ "--function linkMarriageParentsToMarriageParents: link parents of brides/grooms in Marriage Certificates to parents of brides/grooms in Marriage Certificates (for detecting siblings)");
		}

		LOG.outputConsole("");
		LOG.outputConsole("-----------------");
		LOG.outputTotalRuntime("PROGRAM", startTime, true);	
	}



}
