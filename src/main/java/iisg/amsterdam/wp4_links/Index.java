package iisg.amsterdam.wp4_links;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

import com.github.liblevenshtein.transducer.Candidate;

import iisg.amsterdam.wp4_links.utilities.FileUtilities;
import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

import static iisg.amsterdam.wp4_links.Properties.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import java.util.HashMap;

public class Index {


	private String indexID;
	private String directoryDB;
	private String directoryDictionary;
	private MyDB db;
	private BufferedOutputStream streamDictionary;
	private MyTransducer myTransducer;

	//	private Boolean indexStatus = false;


	public static final Logger lg = LogManager.getLogger(Index.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);
	FileUtilities FILE_UTILS = new FileUtilities();


	public Index(String ID, String directoryPath) {
		this.indexID = ID;
		this.directoryDB = directoryPath + "/" + DIRECTORY_NAME_DATABASE + "/" + indexID;
		this.directoryDictionary = directoryPath + "/" + DIRECTORY_NAME_DICTIONARY + "/" + indexID + ".txt";
	}


	public void openIndex() {
		createDB();
		createDictionary();
	}


	public void createDB() {
		db = new MyDB(directoryDB);
		try {
			db.openMyDB(true);
		} catch (RocksDBException e) {
			LOG.logError("createDB", "Error when creating the following DB: " + indexID + " in the following directory " + directoryDB);
			e.printStackTrace();
		}
	}


	public void createDictionary() {
		try {
			streamDictionary = FILE_UTILS.createFileStream(directoryDictionary);
		} catch (IOException e) {
			LOG.logError("createDictionary", "Error when creating the following dictionary text file: " + directoryDictionary);
			e.printStackTrace();
		}
	}

	public void createTransducer(int maxLev) {
		myTransducer = new MyTransducer(directoryDictionary, maxLev);
	}

	public void addSingleValueToMyDB(String key, String value) {
		db.addSingleValueToDB(key, value);
	}

	public void addListValueToMyDB(String key, String value) {
		db.addListValueToDB(key, value);
	}


	public String getSingleValueFromDB(String key) {
		return db.getSingleValueFromDB(key);
	}

	public ArrayList<String> getListFromDB(String key) {
		return db.getListFromDB(key);
	}

	public void addToMyDictionary(String message) {
		FILE_UTILS.writeToOutputStream(streamDictionary, message);
	}


	public Boolean flushDictionary() {
		try {
			streamDictionary.flush();
			return true;
		} catch (IOException e) {
			LOG.logError("flushDictionary", "Error when flushing dictionary of index: " + indexID);
			e.printStackTrace();
			return false;
		}
	}

	public Boolean closeStream() {
		try {
			streamDictionary.close();
			return true;
		} catch (IOException e) {
			LOG.logError("closeDictionary", "Error when closing dictionary of index: " + indexID);
			e.printStackTrace();
			return false;
		}
	}


	// here I can change this function to also decompose the first name into several entries
	public Boolean addPersonToIndex(Person person, String eventID) {
		try {
			String fullName = person.getFullName();
			addToMyDictionary(fullName);
			addListValueToMyDB(fullName, eventID);
			return true;
		} catch (Exception e) {
			LOG.logError("addPersonToIndex", "Error adding person: " + person + " of eventID: " + eventID + " to index: " + indexID);
		}
		return false;
	}


	public ArrayList<Candidate> searchFullNameInTransducer(Person person) {
		ArrayList<Candidate> result = new ArrayList<Candidate>();
		try {
			if(person.hasFullName()) {			
				String fullName = person.getFullName();
				Iterable<Candidate> candidates = myTransducer.transducer.transduce(fullName, myTransducer.maxLevDistance);
				for(Candidate cand: candidates) {
					result.add(cand);
				}		
				return result;
			}
		} catch (Exception e) {
			LOG.logError("searchFullNameInTransducer", "Error searching for full name of person: " + person + " in index: " + indexID);
		}
		return result;
	}
	
	public HashMap<String, Candidate> findCandidatesInDB(ArrayList<Candidate> candidates) {
		 HashMap<String, Candidate> results = new HashMap<String, Candidate>();
		for(Candidate cand: candidates) {
			ArrayList<String> allCandEventsID = getListFromDB(cand.term()); 
			for(String canEventID: allCandEventsID) {
				results.put(canEventID, cand);
			}
		}
		return results;
	}
	

	
	public HashMap<String, Candidate> searchFullNameInTransducerAndDB(Person person) {
		return findCandidatesInDB(searchFullNameInTransducer(person));
	}



}
