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
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class ProcessMarriageParentsToMarriageParents {
	// output directory specified by the user + name of the called function
	private String mainDirectoryPath, processName = "";;
	private MyHDT myHDT;
	private int maxLev, MAX_YEAR_DIFF = 50;
	private int indexingUpdateInterval = 1000, linkingUpdateInterval = 3000;
	Index indexMotherBride, indexFatherBride, indexMotherGroom, indexFatherGroom;

	public static final Logger lg = LogManager.getLogger(ProcessMarriageParentsToMarriageParents.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);
	Links LINKS;


	public ProcessMarriageParentsToMarriageParents(MyHDT hdt, String directoryPath, Integer maxLevenshtein, Boolean formatRDF) {
		this.mainDirectoryPath = directoryPath;
		this.maxLev = maxLevenshtein;
		this.myHDT = hdt;
		String resultsFileName = "mar_parent-to-mar_parent-maxLev-" + maxLevenshtein;
		LINKS = new Links(resultsFileName, mainDirectoryPath, formatRDF);
		linkParentsToParents();
	}

	public Boolean generateParentsIndex() {
		long startTime = System.currentTimeMillis();
		int cntInserts=0, cntAll =0 ;
		IteratorTripleString it;
		processName = "Parents";
		indexMotherBride = new Index("mother-bride", mainDirectoryPath);
		indexFatherBride = new Index("father-bride", mainDirectoryPath);
		indexMotherGroom = new Index("mother-groom", mainDirectoryPath);
		indexFatherGroom = new Index("father-groom", mainDirectoryPath);
		LOG.outputConsole("START: Generating Dictionary for " + processName);
		try {
			indexMotherBride.openIndex();
			indexFatherBride.openIndex();
			indexMotherGroom.openIndex();
			indexFatherGroom.openIndex();
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
					Person brideMother = myHDT.getPersonInfo(marriageEvent, ROLE_BRIDE_MOTHER);
					Person brideFather = myHDT.getPersonInfo(marriageEvent, ROLE_BRIDE_FATHER);
					Person groomMother = myHDT.getPersonInfo(marriageEvent, ROLE_GROOM_MOTHER);
					Person groomFather = myHDT.getPersonInfo(marriageEvent, ROLE_GROOM_FATHER);
					if(brideMother.isValidWithFullName() && brideFather.isValidWithFullName()) {				
						indexMotherBride.addPersonToIndex(brideMother, eventID);
						indexFatherBride.addPersonToIndex(brideFather, eventID);
						cntInserts++;
					}
					if(groomMother.isValidWithFullName() && groomFather.isValidWithFullName()) {				
						indexMotherGroom.addPersonToIndex(groomMother, eventID);
						indexFatherGroom.addPersonToIndex(groomFather, eventID);
						cntInserts++;
					}
					if(cntInserts % 20 == 0) {
						indexMotherBride.flushDictionary();
						indexFatherBride.flushDictionary();
						indexMotherGroom.flushDictionary();
						indexFatherGroom.flushDictionary();
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
			LOG.logError("generateParentsIndex", "Error in iterating over parents of marriage events");
			LOG.logError("", e.toString());
			return false;
		} finally {
			indexMotherBride.closeStream();
			indexFatherBride.closeStream();
			indexMotherGroom.closeStream();
			indexFatherGroom.closeStream();
			LOG.outputTotalRuntime("Generating Dictionary for " + processName, startTime, true);
		}
		return true;
	}


	public void linkParentsToParents() {
		Boolean success = generateParentsIndex();
		if(success == true) {
			indexMotherBride.createTransducer(maxLev);
			indexFatherBride.createTransducer(maxLev);
			indexMotherGroom.createTransducer(maxLev);
			indexFatherGroom.createTransducer(maxLev);
			String marriageEvent = null;
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
						marriageEvent = ts.getSubject().toString();	
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
							matchParents(mother, father, familyCode, marriageEvent, marriageEventYear, indexMotherBride, indexFatherBride, "21");	
							matchParents(mother, father, familyCode, marriageEvent, marriageEventYear, indexMotherGroom, indexFatherGroom, "22");	
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
				LOG.logError("linkPartnerToPartner", "Error in linking partners to partners in process " + processName +
						" certificate " + marriageEvent);
				e.printStackTrace();
			} finally {
				LINKS.closeStream();
			}
		}
	}



	public void matchParents(Person mother, Person father, String familyCode, String sourceMarriageEvent, int marriageYear, Index indexMother, Index indexFather, String indexCode) {
		if(mother.isValidWithFullName() && father.isValidWithFullName()) {
			// start linking here
			
//			if(sourceMarriageEvent.equals("https://iisg.amsterdam/links/marriage/786120")) {
//				System.out.println("Check here");
//			}
			
			HashMap<String, Candidate> candidatesMother, candidatesFather;
			candidatesMother = indexMother.searchFullNameInTransducerAndDB(mother);
			if(!candidatesMother.isEmpty()) {
				candidatesFather = indexFather.searchFullNameInTransducerAndDB(father);
				if(!candidatesFather.isEmpty()) {
					Set<String> candidatesMotherEvents = candidatesMother.keySet();
					Set<String> candidatesFatherEvents = candidatesFather.keySet();
					candidatesMotherEvents.retainAll(candidatesFatherEvents);			
					for(String remainingEvent: candidatesMotherEvents) {
						String marriageEventURI = myHDT.getEventURIfromID(remainingEvent);
						if(!marriageEventURI.equals(sourceMarriageEvent)) {
							int yearDifference = checkTimeConsistencyMarriageParentToMarriageParent(marriageYear, marriageEventURI);
							if(yearDifference > -1) { // if it fits the time line
								int levDistanceMother = candidatesMother.get(remainingEvent).distance();
								int levDistanceFather = candidatesFather.get(remainingEvent).distance();
								String levDistance =  levDistanceMother + "-" + levDistanceFather;
								
								//Person motherTarget, fatherTarget;
								Person egoTarget, egoTargetPartner, egoSource, egoSourcePartner;
								if(indexCode == "21") {
									//motherTarget = myHDT.getPersonInfo(marriageEventURI, ROLE_BRIDE_MOTHER);
									//fatherTarget = myHDT.getPersonInfo(marriageEventURI, ROLE_BRIDE_FATHER);
									egoTarget = myHDT.getPersonInfo(marriageEventURI, ROLE_BRIDE);
									egoTargetPartner = myHDT.getPersonInfo(marriageEventURI, ROLE_GROOM);
								} else {
									//motherTarget = myHDT.getPersonInfo(marriageEventURI, ROLE_GROOM_MOTHER);
									//fatherTarget = myHDT.getPersonInfo(marriageEventURI, ROLE_GROOM_FATHER);
									egoTarget = myHDT.getPersonInfo(marriageEventURI, ROLE_GROOM);
									egoTargetPartner = myHDT.getPersonInfo(marriageEventURI, ROLE_BRIDE);
								}
								
								if(familyCode == "21") {
									egoSource = myHDT.getPersonInfo(sourceMarriageEvent, ROLE_BRIDE);
									egoSourcePartner = myHDT.getPersonInfo(sourceMarriageEvent, ROLE_GROOM);
								} else {
									egoSource = myHDT.getPersonInfo(sourceMarriageEvent, ROLE_GROOM);
									egoSourcePartner = myHDT.getPersonInfo(sourceMarriageEvent, ROLE_BRIDE);
								}
															
								String linkType = "", linkURI = "";
								if(egoSource.isValidWithFullName()  && egoTarget.isValidWithFullName()) {
									if(egoSource.getFirstName().equals(egoTarget.getFirstName())) {
										if(egoSourcePartner.isValidWithFullName() && egoTargetPartner.isValidWithFullName()) {
											if(egoSourcePartner.getFirstName().equals(egoTargetPartner.getFirstName())) {
												linkType = "duplicate-certificate";
												linkURI = LINK_IDENTICAL;
											} else {
												linkType = "ego-married-again";
												linkURI = LINK_IDENTICAL;
											}
										}
									} else {
										linkType = "siblings";
										linkURI = LINK_SIBLINGS;
									}
								}
								String code = familyCode + "," + indexCode;
								SingleMatch matchEgo = new SingleMatch(egoSource, sourceMarriageEvent, egoTarget, marriageEventURI, levDistance, linkType, code, yearDifference);
								LINKS.saveLinks(matchEgo, linkURI);
							}
						}
					}
				}
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
	public int checkTimeConsistencyMarriageParentToMarriageParent(int marriageYear, String marriageCertificateURI) {
		int marriageYear2 = myHDT.getEventDate(marriageCertificateURI);
		int diff = Math.abs(marriageYear - marriageYear2);
		if(diff < MAX_YEAR_DIFF) {
			return diff;
		} else {
			return -1;
		}
	}




}
