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
		Configurator.setRootLevel(Level.ERROR); 

		LOG.outputConsole("PROGRAM STARTED!!");
		LOG.outputConsole("––––––––––––––––");
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
			System.out.println("Show some useful help...");
		}

		LOG.outputConsole("");
		LOG.outputConsole("–––––––––––––––––");
		LOG.outputTotalRuntime("PROGRAM", startTime, true);	
	}



}
