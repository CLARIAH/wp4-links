package iisg.amsterdam.wp4_links.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtilities {

	public static final Logger lg = LogManager.getLogger(FileUtilities.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);


	public Boolean checkIfFileExists(String path) {
		if(path != null) {
			File file = new File(path);
			if(!file.isDirectory()) {
				if(file.exists()) {
					return true;
				} else {
					LOG.logError("checkIfFileExists", "File does not exist");
				}
			} else {
				LOG.logError("checkIfFileExists", "A directory is chosen instead of a file");
			}
		} else {
			LOG.logError("checkIfFileExists", "No file path is specified");
		}
		return false;
	}


	public Boolean checkIfDirectoryExists(String path) {
		if(path != null) {
			File file = new File(path);
			if(file.isDirectory()) {
				return true;
			} else {
				LOG.logError("checkIfDirectoryExists", "Specified path is not a directory");
			}
		} else {
			LOG.logError("checkIfDirectoryExists", "No directory path is specified");
		}
		return false;
	}



	public Boolean createDirectory(String path, String directoryName) {
		try {
			File f = new File(path + "/" + directoryName);
			if(f.isDirectory()) {
				FileUtils.cleanDirectory(f); //clean out directory (this is optional -- but good know)
				FileUtils.forceDelete(f); //delete directory
				FileUtils.forceMkdir(f); //create directory
			} else {
				FileUtils.forceMkdir(f); //create directory
			}		
			return true;
		} catch (IOException e) {
			LOG.logError("createDirectory", "Error creating directory " + path + "/" + directoryName);
			e.printStackTrace();
			return false;
		} 
	}


	public BufferedOutputStream createFileStream(String path) throws IOException {
		try {
			FileOutputStream file = new FileOutputStream(path);
			BufferedOutputStream outStream = new BufferedOutputStream(file);
			LOG.logDebug("createFileStream", "File created successfully at: " + path) ;
			return outStream;
		} catch (IOException ex) {
			LOG.logError("createFileStream", "Error creating file stream");
			ex.printStackTrace();
			return null;
		}
	}



	public Boolean writeToOutputStream(BufferedOutputStream outStream, String message) {
		try {
			outStream.write(message.getBytes());
			outStream.write(System.lineSeparator().getBytes());
			return true;
		} catch (IOException e) {
			LOG.logError("writeToOutputStream", "Cannot write following message: " + message + " to stream: " + outStream);
			e.printStackTrace();
			return false;
		}
	}
	


}
