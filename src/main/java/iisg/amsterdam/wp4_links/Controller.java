package iisg.amsterdam.wp4_links;


import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iisg.amsterdam.wp4_links.processes.*;
import iisg.amsterdam.wp4_links.utilities.FileUtilities;
import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

import static iisg.amsterdam.wp4_links.Properties.*;

public class Controller {

	final String[] FUNCTIONS = {"showDatasetStats", "convertToHDT", "within_b_m", 
			"between_b_m", "between_m_m"};

	private String function,inputDataset,outputDirectory;
	private int maxLev;
	private boolean outputFormatRDF = true; 



	public static final Logger lg = LogManager.getLogger(Controller.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);
	FileUtilities FILE_UTILS = new FileUtilities();


	public Controller(String function, int maxlev, String inputDataset, String outputDirectory, String outputFormat) {
		this.function = function;
		this.maxLev = maxlev;
		this.inputDataset = inputDataset;
		this.outputDirectory = outputDirectory;
		if(!outputFormat.equals("RDF")) {
			outputFormatRDF = false;
		}
	}


	public void runProgram() {
		if(checkInputFunction() == true) {
			switch (function) {
			case "showdatasetstats":
				if(checkInputDataset() == true) {
					outputDatasetStatistics();
				}
				break;
			case "converttohdt":
				if(checkInputDataset() == true) {
					if(checkInputDirectoryOutput() == true)
						convertToHDT();
				}
				break;
			case "within_b_m":
				if(checkAllUserInputs() == true) {
					long startTime = System.currentTimeMillis();
					LOG.outputConsole("START: Within Births-Marriages (i.e. newborn --> partner)");
					Within_B_M();
					LOG.outputTotalRuntime("Within Births-Marriages (i.e. newborn --> partner)", startTime, true);
				}
				break;
			case "between_b_m":
				if(checkAllUserInputs() == true) {
					long startTime = System.currentTimeMillis();
					LOG.outputConsole("START: Between Births-Marriages (i.e. newborn parents --> partners)");
					Between_B_M();
					LOG.outputTotalRuntime("Between Births-Marriages (i.e. newborn parents --> partners)", startTime, true);
				}
				break;
			case "between_m_m":
				if(checkAllUserInputs() == true) {
					long startTime = System.currentTimeMillis();
					LOG.outputConsole("START: Between Marriages-Marriages (i.e. newly-weds' parents --> newly-weds)");
					Between_M_M();
					LOG.outputTotalRuntime("Between Marriages-Marriages (i.e. newly-weds' parents --> newly-weds)", startTime, true);
				}
				break;
//			case "linksiblings":
//				if(checkAllUserInputs() == true) {
//					long startTime = System.currentTimeMillis();
//					LOG.outputConsole("START: Link Siblings");
//					linkSiblings();
//					LOG.outputTotalRuntime("Link Siblings", startTime, true);
//				}
//				break;
//			case "linkmarriageparentstomarriageparents":
//				if(checkAllUserInputs() == true) {
//					long startTime = System.currentTimeMillis();
//					LOG.outputConsole("START: Link Marriage Parents To Marriage Parents");
//					linkMarriageParentsToMarriageParents();
//					LOG.outputTotalRuntime("Link Marriage Parents To Marriage Parents", startTime, true);
//				}
//				break;
			default:
				LOG.logError("runProgram", "User input is correct, but no corresponding function exists (error in code)");
				break;
			}
		}
	}


	public Boolean checkAllUserInputs() {
		Boolean validInputs = true;
		validInputs = validInputs & checkInputMaxLevenshtein();
		validInputs = validInputs & checkInputDataset();
		validInputs = validInputs & checkInputDirectoryOutput();
		checkOutputFormatRDF();
		return validInputs;
	}


	public Boolean checkInputFunction() {
		if(function == null) {
			LOG.logError("checkInputFunction", 
					"Missing user input for parameter: --function",
					"Choose one of the following options: " + Arrays.toString(FUNCTIONS));
		} else {
			function = function.toLowerCase();
			for (String f: FUNCTIONS) {
				if(function.equalsIgnoreCase(f)) {
					LOG.logDebug("checkInputFunction", 
							"User have chosen function: " + function);
					return true;
				}
			}
			LOG.logError("checkInputFunction", 
					"Incorrect user input for parameter: --function",
					"Choose one of the following options: " + Arrays.toString(FUNCTIONS));
		}
		return false;
	}


	public Boolean checkInputMaxLevenshtein() {
		if(maxLev == 99) {
			LOG.logError("checkInputMaxLevenshtein", 
					"Missing user input for parameter: --maxlev",
					"Specify a 'maximum Levenshtein distance' from 0 to 5");
		} else {
			if(maxLev > 5) {
				LOG.logError("checkInputMaxLevenshtein", 
						"Invalid user input for parameter: --maxlev",
						"Specify a 'maximum Levenshtein distance' from 0 to 5");
			} else {
				LOG.logDebug("checkInputMaxLevenshtein", 
						"User have chosen max levenshtein equals to: " + maxLev);
				return true;
			}
		}
		return false;
	}


	public Boolean checkInputDataset() {
		if(FILE_UTILS.checkIfFileExists(inputDataset) == true) {
			LOG.logDebug("checkInputFileInput", "The following dataset is set as input dataset: " + inputDataset);
			return true;
		} else {
			LOG.logError("checkInputFileInput", "Invalid or Missing user input for parameter: --inputData", "A valid HDT file is required as input after parameter: --inputData");
			return false;
		}
	}


	public Boolean checkInputDirectoryOutput() {
		if(FILE_UTILS.checkIfDirectoryExists(outputDirectory)) {
			LOG.logDebug("checkInputDirectoryOutput", "The following directory is set to store results: " + outputDirectory);
			return true;
		} else {
			LOG.logError("checkInputDirectoryOutput", "Invalid or Missing user input for parameter: --outputDir", "A valid directory for storing links is required as input after parameter: --outputDir");
			return false;
		}
	}


	public Boolean checkOutputFormatRDF() {
		if(outputFormatRDF == true) {
			LOG.logDebug("checkOutputFormatRDF", "Output format is set as RDF");
			return true;
		} else {
			LOG.logDebug("checkOutputFormatRDF", "Output format is set as CSV");
			return false;
		}
	}


	public void outputDatasetStatistics() {
		MyHDT myHDT = new MyHDT(inputDataset);
		// birth certificates
		// LOG.outputConsole("--- 	# Birth Registrations: " + numberOfBirthRegistrations + " ---");
		int numberOfBirthEvents = myHDT.getNumberOfSubjects(TYPE_BIRTH_EVENT);
		LOG.outputConsole("--- 	# Birth Events: " + numberOfBirthEvents + " ---");
		// marriage certificates
		// LOG.outputConsole("--- 	# Marriage Registrations: " + numberOfMarriageRegistrations + " ---");
		int numberOfMarriageEvents = myHDT.getNumberOfSubjects(TYPE_MARRIAGE_EVENT);
		LOG.outputConsole("--- 	# Marriage Events: " + numberOfMarriageEvents + " ---");
		// LOG.outputConsole("--- 		DIFF: Registrations - Events= " + diffMarriage);
		// death certificates
		// LOG.outputConsole("--- 	# Death Registrations: " + numberOfDeathRegistrations + " ---");
		int numberOfDeathEvents = myHDT.getNumberOfSubjects(TYPE_DEATH_EVENT);
		LOG.outputConsole("--- 	# Death Events: " + numberOfDeathEvents + " ---");
		// LOG.outputConsole("--- 		DIFF: Registrations - Events= " + diffDeath);
		// individuals
		int numberOfIndividuals = myHDT.getNumberOfSubjects(TYPE_PERSON);
		LOG.outputConsole("--- 	# Individuals: " + numberOfIndividuals + " ---");
		myHDT.closeDataset();
	}


	public void convertToHDT() {
		new MyHDT(inputDataset, outputDirectory);
	}


	public void Within_B_M() {
		String dirName = function + "-maxLev-" + maxLev;
		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, dirName);
		if(processDirCreated == true) {
			String mainDirectory = outputDirectory + "/" + dirName;
			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
				MyHDT myHDT = new MyHDT(inputDataset);	
				new Within_B_M(myHDT, mainDirectory, maxLev, outputFormatRDF);
				myHDT.closeDataset();
			} else {
				LOG.logError("Within_B_M", "Error in creating the three sub output directories");
			}
		} else {
			LOG.logError("Within_B_M", "Error in creating the main output directory");
		}
	}
	
	
	public void Between_B_M() {
		String dirName = function + "-maxLev-" + maxLev;
		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, dirName);
		if(processDirCreated == true) {
			String mainDirectory = outputDirectory + "/" + dirName;
			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
				MyHDT myHDT = new MyHDT(inputDataset);	
				new Between_B_M(myHDT, mainDirectory, maxLev, outputFormatRDF);
				myHDT.closeDataset();
			} else {
				LOG.logError("Between_B_M", "Error in creating the three sub output directories");
			}
		} else {
			LOG.logError("Between_B_M", "Error in creating the main output directory");
		}
	}


	public void Between_M_M() {
		String dirName = function + "-maxLev-" + maxLev;
		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, dirName);
		if(processDirCreated == true) {
			String mainDirectory = outputDirectory + "/" + dirName;
			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
				MyHDT myHDT = new MyHDT(inputDataset);	
				new Between_M_M(myHDT, mainDirectory, maxLev, outputFormatRDF);
				myHDT.closeDataset();
			} else {
				LOG.logError("Between_M_M", "Error in creating the three sub output directories");
			}
		} else {
			LOG.logError("Between_M_M", "Error in creating the main output directory");
		}
	}
	
	
	
	

//	public void linkSiblings() {
//		String dirName = function + "-maxLev" + maxLev;
//		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, dirName);
//		if(processDirCreated == true) {
//			String mainDirectory = outputDirectory + "/" + dirName;
//			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
//			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
//			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
//			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
//				MyHDT myHDT = new MyHDT(inputDataset);	
//				//new ProcessSiblings(myHDT, mainDirectory, maxLev, outputFormatRDF);
//				myHDT.closeDataset();
//			} else {
//				LOG.logError("linkSiblings", "Error in creating the three sub output directories");
//			}
//		} else {
//			LOG.logError("linkSiblings", "Error in creating the main output directory");
//		}
//	}
//	
//	public void linkMarriageParentsToMarriageParents() {
//		String dirName = function + "-maxLev" + maxLev;
//		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, dirName);
//		if(processDirCreated == true) {
//			String mainDirectory = outputDirectory + "/" + dirName;
//			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
//			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
//			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
//			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
//				MyHDT myHDT = new MyHDT(inputDataset);	
//				//new ProcessMarriageParentsToMarriageParents(myHDT, mainDirectory, maxLev, outputFormatRDF);
//				myHDT.closeDataset();
//			} else {
//				LOG.logError("linkMarriageParentsToMarriageParents", "Error in creating the three sub output directories");
//			}
//		} else {
//			LOG.logError("linkMarriageParentsToMarriageParents", "Error in creating the main output directory");
//		}
//	}




}





