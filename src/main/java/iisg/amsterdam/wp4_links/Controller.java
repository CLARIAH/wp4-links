package iisg.amsterdam.wp4_links;


import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iisg.amsterdam.wp4_links.processes.ProcessNewbornToPartner;
import iisg.amsterdam.wp4_links.processes.ProcessPartnerToPartner;
import iisg.amsterdam.wp4_links.processes.ProcessSiblings;
import iisg.amsterdam.wp4_links.utilities.FileUtilities;
import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

import static iisg.amsterdam.wp4_links.Properties.*;

public class Controller {

	final String[] FUNCTIONS = {"showDatasetStats", "linkNewbornToPartner", "linkPartnerToPartner", "linkSiblings"};

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
			case "linknewborntopartner":
				if(checkAllUserInputs() == true) {
					long startTime = System.currentTimeMillis();
					LOG.outputConsole("START: Link Newborn to Partner");
					linkNewbornToPartner();
					LOG.outputTotalRuntime("Link Newborn to Partner", startTime);
				}
				break;
			case "linkpartnertopartner":
				if(checkAllUserInputs() == true) {
					long startTime = System.currentTimeMillis();
					LOG.outputConsole("START: Link Partner to Partner");
					linkPartnerToPartner();
					LOG.outputTotalRuntime("Link Partner to Partner", startTime);
				}
				break;
			case "linksiblings":
				if(checkAllUserInputs() == true) {
					long startTime = System.currentTimeMillis();
					LOG.outputConsole("START: Link Siblings");
					linkSiblings();
					LOG.outputTotalRuntime("Link Siblings", startTime);
				}
				break;
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
		int numberOfBirthRegistrations = myHDT.getNumberOfSubjects(TYPE_BIRTH_REGISTRATION);
		LOG.outputConsole("--- 	# Birth Registrations: " + numberOfBirthRegistrations + " ---");
		int numberOfBirthEvents = myHDT.getNumberOfSubjects(TYPE_BIRTH_EVENT);
		LOG.outputConsole("--- 	# Birth Events: " + numberOfBirthEvents + " ---");
		int diffBirth = numberOfBirthRegistrations - numberOfBirthEvents;
		LOG.outputConsole("--- 		DIFF: Registrations - Events= " + diffBirth);
		// marriage certificates
		int numberOfMarriageRegistrations = myHDT.getNumberOfSubjects(TYPE_MARRIAGE_REGISTRATION);
		LOG.outputConsole("--- 	# Marriage Registrations: " + numberOfMarriageRegistrations + " ---");
		int numberOfMarriageEvents = myHDT.getNumberOfSubjects(TYPE_MARRIAGE_EVENT);
		LOG.outputConsole("--- 	# Marriage Events: " + numberOfMarriageEvents + " ---");
		int diffMarriage = numberOfMarriageRegistrations - numberOfMarriageEvents;
		LOG.outputConsole("--- 		DIFF: Registrations - Events= " + diffMarriage);
		// death certificates
		int numberOfDeathRegistrations = myHDT.getNumberOfSubjects(TYPE_DEATH_REGISTRATION);
		LOG.outputConsole("--- 	# Death Registrations: " + numberOfDeathRegistrations + " ---");
		int numberOfDeathEvents = myHDT.getNumberOfSubjects(TYPE_DEATH_EVENT);
		LOG.outputConsole("--- 	# Death Events: " + numberOfDeathEvents + " ---");
		int diffDeath = numberOfDeathRegistrations - numberOfDeathEvents;
		LOG.outputConsole("--- 		DIFF: Registrations - Events= " + diffDeath);
		// individuals
		int numberOfIndividuals = myHDT.getNumberOfSubjects(TYPE_PERSON);
		LOG.outputConsole("--- 	# Individuals: " + numberOfIndividuals + " ---");		
	}


	public void linkNewbornToPartner() {
		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, function);
		if(processDirCreated == true) {
			String mainDirectory = outputDirectory + "/" + function;
			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
				MyHDT myHDT = new MyHDT(inputDataset);	
				new ProcessNewbornToPartner(myHDT, mainDirectory, maxLev, outputFormatRDF);
			} else {
				LOG.logError("linkNewbornToPartner", "Error in creating the three sub output directories");
			}
		} else {
			LOG.logError("linkNewbornToPartner", "Error in creating the main output directory");
		}
	}
	
	
	public void linkPartnerToPartner() {
		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, function);
		if(processDirCreated == true) {
			String mainDirectory = outputDirectory + "/" + function;
			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
				MyHDT myHDT = new MyHDT(inputDataset);	
				new ProcessPartnerToPartner(myHDT, mainDirectory, maxLev, outputFormatRDF);
			} else {
				LOG.logError("linkPartnerToPartner", "Error in creating the three sub output directories");
			}
		} else {
			LOG.logError("linkPartnerToPartner", "Error in creating the main output directory");
		}
	}
	
	public void linkSiblings() {
		Boolean processDirCreated =  FILE_UTILS.createDirectory(outputDirectory, function);
		if(processDirCreated == true) {
			String mainDirectory = outputDirectory + "/" + function;
			Boolean dictionaryDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DICTIONARY);
			Boolean databaseDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_DATABASE);
			Boolean resultsDirCreated = FILE_UTILS.createDirectory(mainDirectory, DIRECTORY_NAME_RESULTS);
			if(dictionaryDirCreated &&  databaseDirCreated && resultsDirCreated) {
				MyHDT myHDT = new MyHDT(inputDataset);	
				new ProcessSiblings(myHDT, mainDirectory, maxLev, outputFormatRDF);
			} else {
				LOG.logError("linkSiblings", "Error in creating the three sub output directories");
			}
		} else {
			LOG.logError("linkSiblings", "Error in creating the main output directory");
		}
	}



	
}





