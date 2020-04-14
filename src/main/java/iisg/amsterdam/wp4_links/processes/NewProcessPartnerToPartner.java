package iisg.amsterdam.wp4_links.processes;

import static iisg.amsterdam.wp4_links.Properties.LINK_IDENTICAL;
import static iisg.amsterdam.wp4_links.Properties.ROLE_BRIDE;
import static iisg.amsterdam.wp4_links.Properties.ROLE_BRIDE_FATHER;
import static iisg.amsterdam.wp4_links.Properties.ROLE_BRIDE_MOTHER;
import static iisg.amsterdam.wp4_links.Properties.ROLE_GROOM;
import static iisg.amsterdam.wp4_links.Properties.ROLE_GROOM_FATHER;
import static iisg.amsterdam.wp4_links.Properties.ROLE_GROOM_MOTHER;

import java.util.HashMap;

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
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class NewProcessPartnerToPartner {

	// output directory specified by the user + name of the called function
		private String mainDirectoryPath, processName = "";;
		private MyHDT myHDT;
		private int maxLev, MIN_YEAR_DIFF = 14, MAX_YEAR_DIFF = 76;
		private int indexingUpdateInterval = 1000, linkingUpdateInterval = 3000;
		Index indexBride, indexGroom;

		public static final Logger lg = LogManager.getLogger(ProcessPartnerToPartner.class);
		LoggingUtilities LOG = new LoggingUtilities(lg);
		Links LINKS;


		public NewProcessPartnerToPartner(MyHDT hdt, String directoryPath, Integer maxLevenshtein, Boolean formatRDF) {
			this.mainDirectoryPath = directoryPath;
			this.maxLev = maxLevenshtein;
			this.myHDT = hdt;
			String resultsFileName = "partner-to-partner-maxLev-" + maxLevenshtein;
			LINKS = new Links(resultsFileName, mainDirectoryPath, formatRDF);
			linkPartnerToPartner();
		}


		public Boolean generateCoupleIndex() {
			long startTime = System.currentTimeMillis();
			int cntInserts=0, cntAll =0 ;
			IteratorTripleString it;
			processName = "Couples";
			indexBride = new Index("bride", mainDirectoryPath);
			indexGroom = new Index("groom", mainDirectoryPath);
			LOG.outputConsole("START: Generating Dictionary for " + processName);
			try {
				indexBride.openIndex();
				indexGroom.openIndex();
				// iterate over all brides or grooms of marriage events
				it = myHDT.dataset.search("", ROLE_BRIDE, "");
				long estNumber = it.estimatedNumResults();
				LOG.outputConsole("Estimated number of certificates to be indexed is: " + estNumber);	
				String taskName = "Indexing " + processName;
				ProgressBar pb = null;
				try {
				pb = new ProgressBar(taskName, estNumber, indexingUpdateInterval, System.err, ProgressBarStyle.UNICODE_BLOCK, " cert.", 1);
					while(it.hasNext()) {
						TripleString ts = it.next();
						cntAll++;
						String marriageEvent = ts.getSubject().toString();
						String eventID = myHDT.getIDofEvent(marriageEvent);
						Person bride = myHDT.getPersonInfo(marriageEvent, ROLE_BRIDE);
						Person groom = myHDT.getPersonInfo(marriageEvent, ROLE_GROOM);
						if(bride.isValidWithFullName() && groom.isValidWithFullName()) {				
							indexBride.addPersonToIndex(bride, eventID);
							indexGroom.addPersonToIndex(groom, eventID);
							cntInserts++;
						}			
						if(cntInserts % 20 == 0) {
							indexBride.flushDictionary();
							indexGroom.flushDictionary();
						}
						if(cntAll % 10000 == 0) {
							pb.stepBy(10000);
							//LOG.outputConsole("Generating Dictionary: number of indexed " + processName + " is: " + cntInserts);
						}						
					} pb.stepTo(estNumber);
				} finally {
					pb.close();
				}
			} catch (NotFoundException e) {
				LOG.logError("generateCoupleIndex", "Error in iterating over partners of marriage events");
				LOG.logError("", e.toString());
				return false;
			} finally {
				indexBride.closeStream();
				indexGroom.closeStream();
				LOG.outputTotalRuntime("Generating Dictionary for " + processName, startTime, true);
			}
			return true;
		}


		public void linkPartnerToPartner() {
			Boolean success = generateCoupleIndex();
			if(success == true) {	
				indexBride.createTransducer(maxLev);
				indexGroom.createTransducer(maxLev);
				try {
					int cntAll =0 ;
					String familyCode = "";
					// iterate through the marriage certificates to link it to the marriage dictionaries
					IteratorTripleString it = myHDT.dataset.search("", ROLE_BRIDE, "");
					long estNumber = it.estimatedNumResults();
					LOG.outputConsole("Estimated number of certificates to be linked is: " + estNumber);	
					String taskName = "Linking " + processName;
					ProgressBar pb = null;
					try {
					pb = new ProgressBar(taskName, estNumber, linkingUpdateInterval, System.err, ProgressBarStyle.UNICODE_BLOCK, " cert.", 1); 
						while(it.hasNext()) {	
							TripleString ts = it.next();	
							cntAll++;
							String marriageEvent = ts.getSubject().toString();	
							int marriageEventYear = myHDT.getEventDate(marriageEvent);
							Person mother, father;			
							for(int i=0; i<2; i++) {
								mother = null; father = null;   
								if(i==0) {
									mother = myHDT.getPersonInfo(marriageEvent, ROLE_BRIDE_MOTHER);
									father = myHDT.getPersonInfo(marriageEvent, ROLE_BRIDE_FATHER);
									familyCode = "21";
								} else {
									mother = myHDT.getPersonInfo(marriageEvent, ROLE_GROOM_MOTHER);
									father = myHDT.getPersonInfo(marriageEvent, ROLE_GROOM_FATHER);;
									familyCode = "22";
								}
								if(mother.isValidWithFullName() && father.isValidWithFullName()) {
									// start linking here
									HashMap<String, Candidate> candidatesBride, candidatesGroom;
									candidatesBride = indexBride.searchFullNameInTransducerAndDB(mother);
									if(!candidatesBride.isEmpty()) {
										candidatesGroom = indexGroom.searchFullNameInTransducerAndDB(father);
										if(!candidatesGroom.isEmpty()) {
											HashMap<String, String> candidateEvents = indexBride.getIntersectedCandidateEvents(candidatesBride, candidatesGroom);		
											for(String remainingEvent: candidateEvents.keySet()) {
												String marriageEventAsCoupleURI = myHDT.getEventURIfromID(remainingEvent);
												int yearDifference = checkTimeConsistencyMarriageToMarriage(marriageEventYear, marriageEventAsCoupleURI);
												if(yearDifference > -1) { // if it fits the time line
													String meta[] = candidateEvents.get(remainingEvent).split(",");
													//int levDistanceMother = candidatesBride.get(remainingEvent).distance();
													// int levDistanceFather = candidatesGroom.get(remainingEvent).distance();
													// String levDistance =  levDistanceMother + "-" + levDistanceFather;
//													String matchedNamesBride[] = candidatesBride.get(remainingEvent).term().split(indexBride.eventID_matchedNames_separator);
//													String matchedNamesGroom[] = candidatesGroom.get(remainingEvent).term().split(indexGroom.eventID_matchedNames_separator);
//													String matchedNames = matchedNamesBride[1] + "," + matchedNamesGroom[1];
													Person bride = myHDT.getPersonInfo(marriageEventAsCoupleURI, ROLE_BRIDE);
													Person groom = myHDT.getPersonInfo(marriageEventAsCoupleURI, ROLE_GROOM);	
													String levDistance = meta[0];
													String matchedNames = meta[1];
													SingleMatch matchBride = new SingleMatch(mother, marriageEvent, bride, marriageEventAsCoupleURI, levDistance, matchedNames, familyCode, yearDifference);												
													SingleMatch matchGroom = new SingleMatch(father, marriageEvent, groom, marriageEventAsCoupleURI, levDistance, matchedNames, familyCode, yearDifference);
													LINKS.saveLinks(matchBride, matchGroom, LINK_IDENTICAL);
												}
											}
										}
									}
								}								
							}
							if(cntAll % 10000 == 0) {
								pb.stepBy(10000);
								//LOG.outputConsole("Linking Couples to " + processName + ": " + cntLinks);
							}
						} pb.stepTo(estNumber); 
					} finally {
						pb.close();
					}
				} catch (Exception e) {
					LOG.logError("linkPartnerToPartner", "Error in linking partners to partners in process " + processName);
					e.printStackTrace();
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
		public int checkTimeConsistencyMarriageToMarriage(int marriageAsParentsYear, String marriageAsCouple) {
			int marriageAsCoupleYear = myHDT.getEventDate(marriageAsCouple);
			int diff = marriageAsParentsYear - marriageAsCoupleYear;
			if(diff >= MIN_YEAR_DIFF && diff < MAX_YEAR_DIFF) {
				return diff;
			} else {
				return -1;
			}
		}

	
}
