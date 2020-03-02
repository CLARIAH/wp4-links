package iisg.amsterdam.wp4_links.processes;

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

import static iisg.amsterdam.wp4_links.Properties.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class ProcessNewbornToPartner {

	// output directory specified by the user + name of the called function
	private String mainDirectoryPath, processName = "";;
	private MyHDT myHDT;
	private int maxLev, MIN_YEAR_DIFF = 14, MAX_YEAR_DIFF = 70;
	Index indexPartner, indexPartnerMother, indexPartnerFather, indexMissingParents;

	public static final Logger lg = LogManager.getLogger(ProcessNewbornToPartner.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);
	Links LINKS;


	public ProcessNewbornToPartner(MyHDT hdt, String directoryPath, Integer maxLevenshtein, Boolean formatRDF) {
		this.mainDirectoryPath = directoryPath;
		this.maxLev = maxLevenshtein;
		this.myHDT = hdt;
		LINKS = new Links("newbornToPartner", mainDirectoryPath, formatRDF);
		linkNewbornToPartner("f");
		linkNewbornToPartner("m");
	}


	public Boolean generatePartnerIndex(String gender) {
		long startTime = System.currentTimeMillis();
		int cntPMF =0, cntP =0, cntPM =0, cntPF =0, cntNone =0, cntInserts=0 ;
		IteratorTripleString it;
		String rolePartner, rolePartnerMother, rolePartnerFather; 
		if(gender == "f") {
			rolePartner = ROLE_BRIDE;
			rolePartnerMother = ROLE_BRIDE_MOTHER;
			rolePartnerFather = ROLE_BRIDE_FATHER;
			processName = "Brides";
			indexPartner = new Index("bride", mainDirectoryPath);
			indexPartnerMother = new Index("brideMother", mainDirectoryPath);
			indexPartnerFather = new Index("brideFather", mainDirectoryPath);
			indexMissingParents = new Index("missingParentsBride", mainDirectoryPath);
		} else {
			rolePartner = ROLE_GROOM;
			rolePartnerMother = ROLE_GROOM_MOTHER;
			rolePartnerFather = ROLE_GROOM_FATHER;
			processName = "Grooms";
			indexPartner = new Index("groom", mainDirectoryPath);
			indexPartnerMother = new Index("groomMother", mainDirectoryPath);
			indexPartnerFather = new Index("groomFather", mainDirectoryPath);
			indexMissingParents = new Index("missingParentsGroom", mainDirectoryPath);
		}
		LOG.outputConsole("START: Generating Dictionary for " + processName);
		try {
			indexPartner.openIndex();
			indexPartnerMother.openIndex();
			indexPartnerFather.openIndex();
			indexMissingParents.createDB();
			// iterate over all brides or grooms of marriage events
			it = myHDT.dataset.search("", rolePartner, "");
			while(it.hasNext()) {
				TripleString ts = it.next();
				String marriageEvent = ts.getSubject().toString();
				String eventID = myHDT.getIDofEvent(marriageEvent);
				Person partner = myHDT.getPersonInfo(marriageEvent, rolePartner);
				Person partnerMother = myHDT.getPersonInfo(marriageEvent, rolePartnerMother);
				Person partnerFather = myHDT.getPersonInfo(marriageEvent, rolePartnerFather);	
				if(partner.isValidWithFullName()) {
					Boolean validMother = partnerMother.isValidWithFullName();
					Boolean validFather = partnerFather.isValidWithFullName();
					switch (validMother + "-" + validFather) {
					case "true-true":
						indexPartner.addPersonToIndex(partner, eventID);
						indexPartnerMother.addPersonToIndex(partnerMother, eventID);
						indexPartnerFather.addPersonToIndex(partnerFather, eventID);
						cntInserts++;
						cntPMF ++;
						break;
					case "true-false":
						indexPartner.addPersonToIndex(partner, eventID);
						indexPartnerMother.addPersonToIndex(partnerMother, eventID);
						indexMissingParents.addSingleValueToMyDB(eventID, "Fa");
						cntInserts++;
						cntPM++;
						break;
					case "false-true":
						indexPartner.addPersonToIndex(partner, eventID);
						indexPartnerFather.addPersonToIndex(partnerFather, eventID);
						indexMissingParents.addSingleValueToMyDB(eventID, "Mo");
						cntInserts++;
						cntPF++;
						break;
					case "false-false":
						cntP++;
						break;
					default:
						LOG.logError("generatePartnerIndex", "Something has gone wrong processing event: " + eventID);
					}			
					if(cntInserts % 20 == 0) {
						indexPartner.flushDictionary();
						indexPartnerMother.flushDictionary();
						indexPartnerFather.flushDictionary();
						if(cntInserts % 10000 == 0) {
							LOG.outputConsole("Generating Dictionary: number of indexed " + processName + " is: " + cntInserts);
						}
					}						
				} else {
					cntNone++;
					LOG.logDebug("generatePartnerIndex", "Partner is not valid of marriage event: " + marriageEvent );
				}
			}
		} catch (NotFoundException e) {
			LOG.logError("generatePartnerIndex", "Error in iterating over partners of marriage events");
			LOG.logError("", e.toString());
			return false;
		} finally {
			indexPartner.closeStream();
			indexPartnerMother.closeStream();
			indexPartnerFather.closeStream();
			Boolean numbersAddUp = false;
			if(cntPMF + cntPM + cntPF == cntInserts) {
				numbersAddUp = true;
			}
			LOG.outputTotalRuntime("Generating Dictionary for " + processName, startTime);
			LOG.outputConsole("--> count exist (Partner + Mother + Father): " + cntPMF);
			LOG.outputConsole("--> count exist (Partner + Mother): " + cntPM);
			LOG.outputConsole("--> count exist (Partner + Father): " + cntPF);
			LOG.outputConsole("--> count total inserts to index: " + cntInserts + " - Numbers Add Up? " + numbersAddUp);
			LOG.outputConsole("--> count exist (Partner): " + cntP);
			LOG.outputConsole("--> count certificates without any full names: " + cntNone);
		}
		return true;
	}


	public void linkNewbornToPartner(String gender) {
		Boolean success = generatePartnerIndex(gender);
		if(success == true) {	
			indexPartner.createTransducer(maxLev);
			indexPartnerMother.createTransducer(maxLev);
			indexPartnerFather.createTransducer(maxLev);
			try {
				int cntPMF =0, cntP =0, cntPM =0, cntPF =0, cntNone =0, cntLinks=0 ;
				String rolePartner, rolePartnerMother, rolePartnerFather; 
				if(gender == "f") {
					rolePartner = ROLE_BRIDE;
					rolePartnerMother = ROLE_BRIDE_MOTHER;
					rolePartnerFather = ROLE_BRIDE_FATHER;
				} else {
					rolePartner = ROLE_GROOM;
					rolePartnerMother = ROLE_GROOM_MOTHER;
					rolePartnerFather = ROLE_GROOM_FATHER;
				}
				// iterate through the birth certificates to link it to the marriage dictionaries
				IteratorTripleString it = myHDT.dataset.search("", ROLE_NEWBORN, "");
				while(it.hasNext()) {	
					TripleString ts = it.next();		
					String birthEvent = ts.getSubject().toString();	
					int birthEventYear = myHDT.getEventDate(birthEvent);
					Person newborn = myHDT.getPersonInfo(birthEvent, ROLE_NEWBORN);
					if(newborn.isValidWithFullName()) {
						if(newborn.getGender().equals(gender)) {
							// start linking here
							ArrayList<Candidate> candidatesNewborn;
							HashMap<String, Candidate> candidatesMotherMap, candidatesFatherMap; 
							Person newbornMother = myHDT.getPersonInfo(birthEvent, ROLE_MOTHER);
							Person newbornFather = myHDT.getPersonInfo(birthEvent, ROLE_FATHER);
							Boolean validMother = newbornMother.isValidWithFullName();
							Boolean validFather = newbornFather.isValidWithFullName();
							
							switch (validMother + "-" + validFather) {
							case "true-true":
								// find all marriage events that have a bride/groom that match the newborn's full name
								candidatesNewborn = indexPartner.searchFullNameInTransducer(newborn);	
								if(!candidatesNewborn.isEmpty()) {
									candidatesMotherMap = indexPartnerMother.searchFullNameInTransducerAndDB(newbornMother);
									candidatesFatherMap = indexPartnerFather.searchFullNameInTransducerAndDB(newbornFather);
									Set<String> candidatesMotherEvents = candidatesMotherMap.keySet();
									Set<String> candidatesFatherEvents = candidatesFatherMap.keySet();
									for(Candidate cand: candidatesNewborn) {
										ArrayList<String> eventsIDPartner = indexPartner.getListFromDB(cand.term());
										eventsIDPartner.retainAll(candidatesMotherEvents);
										eventsIDPartner.retainAll(candidatesFatherEvents);
										for(String remainingEvent: eventsIDPartner) {
											String marriageEventURI = myHDT.getEventURIfromID("marriage", remainingEvent);
											int yearDifference = checkTimeConsistencyBirthToMarriage(birthEventYear, marriageEventURI);
											if(yearDifference > -1) { // if it fits the time line
												int levDistanceNewborn = cand.distance();
												int levDistanceMother = candidatesMotherMap.get(remainingEvent).distance();
												int levDistanceFather = candidatesFatherMap.get(remainingEvent).distance();
												String levDistance = levDistanceNewborn + "-" + levDistanceMother + "-" + levDistanceFather;
												Person partner = myHDT.getPersonInfo(marriageEventURI, rolePartner);
												Person partnerMother = myHDT.getPersonInfo(marriageEventURI, rolePartnerMother);
												Person partnerFather = myHDT.getPersonInfo(marriageEventURI, rolePartnerFather);
												SingleMatch matchMain = new SingleMatch(newborn, birthEvent, partner, marriageEventURI, levDistance, "main-mother-father", yearDifference);
												SingleMatch matchMother = new SingleMatch(newbornMother, birthEvent, partnerMother, marriageEventURI, levDistance, "main-mother-father", yearDifference);
												SingleMatch matchFather = new SingleMatch(newbornFather, birthEvent, partnerFather, marriageEventURI, levDistance, "main-mother-father", yearDifference);
												LINKS.saveLinks(matchMain);
												LINKS.saveLinks(matchMother);
												LINKS.saveLinks(matchFather);
											}
										}
									}
								}
								cntLinks++;
								cntPMF ++;
								break;
							case "true-false":
								cntLinks++;
								cntPM++;
								break;
							case "false-true":
								cntLinks++;
								cntPF++;
								break;
							case "false-false":
								cntP++;
								break;
							default:
								LOG.logError("generatePartnerIndex", "Something has gone wrong processing event: " + birthEvent);
							}			
							if(cntLinks % 10000 == 0) {
								LOG.outputConsole("Linking Newborns to " + processName + ": " + cntLinks);
							}
						}			
					}
				} 
			} catch (Exception e) {
				LOG.logError("linkNewbornToPartner", "Error in linking newborn to partners in process " + processName);
				LOG.logError("linkNewbornToPartner", e.getLocalizedMessage());
			} finally {
				LINKS.closeRDF();
				
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
	public int checkTimeConsistencyBirthToMarriage(int birthYear, String candidateMarriageEvent) {
		int marriageYear = myHDT.getEventDate(candidateMarriageEvent);
		int diff = marriageYear - birthYear;
		if(diff >= MIN_YEAR_DIFF && diff < MAX_YEAR_DIFF) {
			return diff;
		} else {
			return -1;
		}
	}



}
