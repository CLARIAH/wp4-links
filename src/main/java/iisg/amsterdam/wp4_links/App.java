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
	int maxLev = 4;

	@Parameter(names = "--fixedLev")
	boolean fixedLev = false; 

	@Parameter(names = "--inputData")
	String inputData = null;

	@Parameter(names = "--outputDir")
	String outputDir = null;

	@Parameter(names = "--format")
	String format = "CSV"; // or "RDF"

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
			Controller cntrl = new Controller(function, maxLev, fixedLev, inputData, outputDir, format);
			cntrl.runProgram();
		} else { 
			// do not run program and show some help message if user enter: --help	
			String input =  "%-18s %15s %n";
			
			System.out.println("Parameters that can be provided as input to the linking tool:");
			System.out.printf(input, "--function:", "(required) One of the functionalities listed below");
			System.out.printf(input, "--inputData:", "(required) Path to the HDT dataset");
			System.out.printf(input, "--outputDir:", "(required) Path to the directory for saving the indices and the links");
			System.out.printf(input, "--maxLev:", "(optional, default = 4) Integer between 0 and 4, indicating the maximum Levenshtein distance per first or last name allowed to accept a link");
			System.out.printf(input, "--fixedLev:", "(optional, default = False) Add this flag without a value (i.e. True) if you want to apply the same maximum Levenshtein distance on all string lengths");
			System.out.printf(input, "--format:", "(optional, default = CSV) One of the two Strings: RDF or CSV, indicating the desired format to save the links between certificates");
			System.out.printf(input, "--debug:", "(optional, default = error) One of the two Strings: error (only display error messages in console), all (show all warning in console)");
			System.out.println("\n");
			
			System.out.println("Functionalities that are supported in the current version: (case insensitive)");
			System.out.printf(input, "ConvertToHDT:", "Convert an RDF dataset to an HDT file");
			System.out.printf(input, "ShowDatasetStats:", "Show in console some general stats about the input HDT dataset");
			System.out.printf(input, "Within_B_M:", "link newborns in Birth Certificates to brides/grooms in Marriage Certificates (reconstructs life course)");
			System.out.printf(input, "Between_B_M:", "link parents of newborns in Birth Certificates to brides and grooms in Marriage Certificates (reconstructs family ties)");
			System.out.println("\n");
			
			System.out.println("Example of a running configuration:");
			System.out.println("--function Between_b_M --inputData myData.hdt --outputDir . --format CSV  --maxLev 3 --fixedLev");
			System.out.println("\nThese arguments respectively indicate that the user wants to:\n "
					+ "\t\t link parents of newborns in Birth Certificates to brides and grooms in Marriage Certificates,\n "
					+ "\t\t described in the dataset myData.hdt (according to CLARIAH's civil registries RDF schema),\n "
					+ "\t\t and save the detected links in the current directory,\n "
					+ "\t\t as a CSV file,\n "
					+ "\t\t allowing a maximum Levenshtein of 3 per name (first name or last name),\n "
					+ "\t\t independently from the length of the name.");
		}

		LOG.outputConsole("");
		LOG.outputConsole("-----------------");
		LOG.outputTotalRuntime("PROGRAM", startTime, true);	
	}



}
