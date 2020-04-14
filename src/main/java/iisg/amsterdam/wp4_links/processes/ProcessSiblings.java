package iisg.amsterdam.wp4_links.processes;

import static iisg.amsterdam.wp4_links.Properties.*;

import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import com.github.liblevenshtein.transducer.Candidate;

import iisg.amsterdam.wp4_links.Index;
import iisg.amsterdam.wp4_links.Links;
import iisg.amsterdam.wp4_links.MyHDT;
import iisg.amsterdam.wp4_links.Person;
import iisg.amsterdam.wp4_links.SingleMatch;
import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

public class ProcessSiblings {

	// output directory specified by the user + name of the called function
	private String mainDirectoryPath, processName = "";;
	private MyHDT myHDT;
	private int maxLev, MAX_DIFF = 30;
	Index indexMother, indexFather;

	public static final Logger lg = LogManager.getLogger(ProcessSiblings.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);
	Links LINKS;


	public ProcessSiblings(MyHDT hdt, String directoryPath, Integer maxLevenshtein, Boolean formatRDF) {
		this.mainDirectoryPath = directoryPath;
		this.maxLev = maxLevenshtein;
		this.myHDT = hdt;
		String resultsFileName = "siblings-maxLev-" + maxLevenshtein;
		LINKS = new Links(resultsFileName, mainDirectoryPath, formatRDF);
		linkSiblings();
	}


	public Boolean generateParentsIndex() {
		long startTime = System.currentTimeMillis();
		int cntInserts=0 ;
		IteratorTripleString it;
		processName = "Siblings";
		indexMother = new Index("mother", mainDirectoryPath);
		indexFather = new Index("father", mainDirectoryPath);
		LOG.outputConsole("START: Generating Dictionary for " + processName);
		try {
			indexMother.openIndex();
			indexFather.openIndex();
			// iterate over all brides or grooms of marriage events
			it = myHDT.dataset.search("", ROLE_NEWBORN, "");
			while(it.hasNext()) {
				TripleString ts = it.next();
				String birthEvent = ts.getSubject().toString();
				String eventID = myHDT.getIDofEvent(birthEvent);
				Person mother = myHDT.getPersonInfo(birthEvent, ROLE_MOTHER);
				Person father = myHDT.getPersonInfo(birthEvent, ROLE_FATHER);
				if(mother.isValidWithFullName() && father.isValidWithFullName()) {				
					indexMother.addPersonToIndex(mother, eventID);
					indexFather.addPersonToIndex(father, eventID);
					cntInserts++;
				}			
				if(cntInserts % 20 == 0) {
					indexMother.flushDictionary();
					indexFather.flushDictionary();
					if(cntInserts % 10000 == 0) {
						LOG.outputConsole("Generating Dictionary: number of indexed " + processName + " is: " + cntInserts);
					}
				}						
			}
		} catch (NotFoundException e) {
			LOG.logError("generatePartnerIndex", "Error in iterating over partners of marriage events");
			LOG.logError("", e.toString());
			return false;
		} finally {
			indexMother.closeStream();
			indexFather.closeStream();
			LOG.outputTotalRuntime("Generating Dictionary for " + processName, startTime, true);
			LOG.outputConsole("--> count exist (Bride + Groom): " + cntInserts);
		}
		return true;
	}


	public void linkSiblings() {
		Boolean success = generateParentsIndex();
		if(success == true) {	
			indexMother.createTransducer(maxLev);
			indexFather.createTransducer(maxLev);
			try {
				int cntLinks=0 ;
				// iterate through the marriage certificates to link it to the marriage dictionaries
				IteratorTripleString it = myHDT.dataset.search("", ROLE_NEWBORN, "");
				while(it.hasNext()) {	
					TripleString ts = it.next();	
					String birthEvent = ts.getSubject().toString();
					int birthEventYear = myHDT.getEventDate(birthEvent);
					String eventID = myHDT.getIDofEvent(birthEvent);
					Person mother = myHDT.getPersonInfo(birthEvent, ROLE_MOTHER);
					Person father = myHDT.getPersonInfo(birthEvent, ROLE_FATHER);
					cntLinks++;
					if(mother.isValidWithFullName() && father.isValidWithFullName()) {
						// start linking here
						HashMap<String, Candidate> candidatesMother, candidatesFather;
						candidatesMother = indexMother.searchFullNameInTransducerAndDB(mother);
						if(!candidatesMother.isEmpty()) {
							candidatesFather = indexFather.searchFullNameInTransducerAndDB(father);
							if(!candidatesFather.isEmpty()) {
								Set<String> candidatesBrideEvents = candidatesMother.keySet();
								Set<String> candidatesGroomEvents = candidatesFather.keySet();
								candidatesBrideEvents.retainAll(candidatesGroomEvents);			
								for(String remainingEvent: candidatesBrideEvents) {
									if(!remainingEvent.equals(eventID)) {
										String birthEventURIOfSibling = myHDT.getEventURIfromID(remainingEvent);
										int yearDifference = checkTimeConsistencySiblings(birthEventYear, birthEventURIOfSibling);
										if(yearDifference > -1) { // if it fits the time line
											int levDistanceMother = candidatesMother.get(remainingEvent).distance();
											int levDistanceFather = candidatesFather.get(remainingEvent).distance();
											String levDistance =  levDistanceMother + "-" + levDistanceFather;
											Person motherSiblingCertificate = myHDT.getPersonInfo(birthEventURIOfSibling, ROLE_MOTHER);
											Person fatherSiblingCertificate = myHDT.getPersonInfo(birthEventURIOfSibling, ROLE_FATHER);																		
											SingleMatch matchMother = new SingleMatch(mother, birthEvent, motherSiblingCertificate, birthEventURIOfSibling, levDistance, "Mo-Fa", "siblings", yearDifference);
											SingleMatch matchFather = new SingleMatch(father, birthEvent, fatherSiblingCertificate, birthEventURIOfSibling, levDistance, "Mo-Fa", "siblings", yearDifference);
											LINKS.saveLinks(matchMother, LINK_SIBLINGS);
											LINKS.saveLinks(matchFather, LINK_SIBLINGS);
										}
									}
								}
							}
						}
					}			
					if(cntLinks % 10000 == 0) {
						LOG.outputConsole("Linking Couples to " + processName + ": " + cntLinks);
					}
				}			
			} catch (Exception e) {
				LOG.logError("linkPartnerToPartner", "Error in linking partners to partners in process " + processName);
				LOG.logError("linkPartnerToPartner", e.getLocalizedMessage());
			} finally {
				LINKS.closeStream();
			}
		}
	}


	/**
	 * Given the year of a birth event, check whether this marriage event fits the timeline of a possible match
	 * 
	 * @param birthYear
	 *            year of birth 
	 * @param candidateMarriageEvent
	 *            marriage event URI            
	 */
	public int checkTimeConsistencySiblings(int birthEventYear, String birthEventURIOfSibling) {
		int birthEventOfSiblingYear = myHDT.getEventDate(birthEventURIOfSibling);
		int diff = Math.abs(birthEventYear - birthEventOfSiblingYear);
		if(diff < MAX_DIFF) {
			return diff;
		} else {
			return -1;
		}
	}



}
