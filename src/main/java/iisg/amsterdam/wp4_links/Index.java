package iisg.amsterdam.wp4_links;

import static iisg.amsterdam.wp4_links.Properties.DIRECTORY_NAME_DATABASE;
import static iisg.amsterdam.wp4_links.Properties.DIRECTORY_NAME_DICTIONARY;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;

import com.github.liblevenshtein.transducer.Candidate;

import iisg.amsterdam.wp4_links.utilities.FileUtilities;
import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

public class Index {

	private String indexID;
	private String directoryDB;
	private String directoryDictionary;
	private MyDB db;
	private BufferedOutputStream streamDictionary;
	private MyTransducer myTransducer;
	private final String eventID_matchedNames_separator = ":";
	private final int flush_limit = 50;
	private int counterDictionary = 0;

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
		counterDictionary++;
		if(counterDictionary == flush_limit) {
			flushDictionary();
			counterDictionary = 0;
		}
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


	public Boolean addPersonToIndex(Person person, String eventID) {
		try {
			Set<String> fullNameVariations = person.decomposeFirstnameAddLastName();
			int numberOfFirstNames = fullNameVariations.size();
			for(String fullname: fullNameVariations) {
				addToMyDictionary(fullname);
				String value = eventID + eventID_matchedNames_separator + numberOfFirstNames;
				addListValueToMyDB(fullname, value);
			}	
			return true;
		} catch (Exception e) {
			LOG.logError("addPersonToIndex", "Error adding person: " + person + " of eventID: " + eventID + " to index: " + indexID);
		}
		return false;
	}
	
	public Boolean addPersonToIndex(Person person, String eventID, String numberOfIndividuals) {
		try {
			Set<String> fullNameVariations = person.decomposeFirstnameAddLastName();
			int numberOfFirstNames = fullNameVariations.size();
			for(String fullname: fullNameVariations) {
				addToMyDictionary(fullname);
				String value = eventID + eventID_matchedNames_separator + numberOfFirstNames 
						+ eventID_matchedNames_separator + numberOfIndividuals;
				addListValueToMyDB(fullname, value);
			}	
			return true;
		} catch (Exception e) {
			LOG.logError("addPersonToIndex", "Error adding person: " + person + " of eventID: " + eventID + " to index: " + indexID);
		}
		return false;
	}


	public ArrayList<Candidate> searchFullNameInTransducer(String fullName) {
		ArrayList<Candidate> result = new ArrayList<Candidate>();
		try {
			Iterable<Candidate> candidates = myTransducer.transducer.transduce(fullName, myTransducer.maxLevDistance);
			for(Candidate cand: candidates) {
				result.add(cand);
			}		
			return result;
		} catch (Exception e) {
			LOG.logError("searchFullNameInTransducer", "Error searching for full name of person: " + fullName + " in index: " + indexID);
		}
		return result;
	}


	public CandidateList searchForCandidate(Person person, String sourceCertificateID) {
		CandidateList candidateList = new CandidateList(person, sourceCertificateID);
		Set<String> fullNameVariations = person.decomposeFirstnameAddLastName();
		for(String fullNameVariation: fullNameVariations) {
			ArrayList<Candidate> initial_candidates = searchFullNameInTransducer(fullNameVariation);
			for(Candidate cand: initial_candidates) {
				ArrayList<String> candidateCertificatesIDList = getListFromDB(cand.term()); 
				for(String candCertificateID: candidateCertificatesIDList) {		
					String[] certificate = candCertificateID.split(":");
					if(certificate.length == 3) {
						candidateList.addCandidate(certificate[0], certificate[1], fullNameVariation, certificate[2], cand);
					} else {
						candidateList.addCandidate(certificate[0], certificate[1], "", fullNameVariation, cand);
					}
					
				}
			}
		}
		return candidateList;
	}



	public HashMap<String, String> getIntersectedCandidateEvents(HashMap<String, Candidate> candidatesRole1, HashMap<String, Candidate> candidatesRole2) {
		HashMap<String, String> result = new HashMap<String, String>();
		HashMap<String,String> candidatesRole1Events = separateEventFromMeta(candidatesRole1.keySet());
		HashMap<String,String> candidatesRole2Events = separateEventFromMeta(candidatesRole2.keySet());
		Set<String> candidates1 = candidatesRole1Events.keySet();
		Set<String> candidates2 = candidatesRole2Events.keySet();
		candidates1.retainAll(candidates2);
		for (String cand : candidates1) {
			String matchedNamesRole1 = candidatesRole1Events.get(cand);
			String matchedNamesRole2 = candidatesRole2Events.get(cand);
			String metaNames = matchedNamesRole1 + "-" + matchedNamesRole2;
			int distanceRole1 = candidatesRole1.get(cand+eventID_matchedNames_separator+matchedNamesRole1).distance();
			int distanceRole2 = candidatesRole2.get(cand+eventID_matchedNames_separator+matchedNamesRole2).distance();
			String metaDistances = distanceRole1 + "-" + distanceRole2;
			result.put(cand, metaDistances + "," + metaNames);
		}
		return result;		
	}



	public HashMap<String,String> separateEventFromMeta(Set<String> events){
		HashMap<String, String> result = new HashMap<String, String>();
		for(String eventWithMeta: events) {
			String[] event = eventWithMeta.split(eventID_matchedNames_separator);
			result.put(event[0], event[1]);
		}
		return result;
	}

}
