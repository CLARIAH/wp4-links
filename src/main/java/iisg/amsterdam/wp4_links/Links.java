package iisg.amsterdam.wp4_links;


import iisg.amsterdam.wp4_links.utilities.FileUtilities;
import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

import static iisg.amsterdam.wp4_links.Properties.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Links {

	private String linksID;
	private String directoryLinks;
	private BufferedOutputStream streamLinks;
	private Boolean formatRDF = true;
	private int counterFlush = 0;
	private Set<String> namedGraphs;

	public static final Logger lg = LogManager.getLogger(Links.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);
	FileUtilities FILE_UTILS = new FileUtilities();


	public Links(String ID, String directoryPath, Boolean formatInRDF) {
		this.linksID = ID;
		this.formatRDF = formatInRDF;
		if(formatRDF == true) {
			this.directoryLinks = directoryPath + "/" + DIRECTORY_NAME_RESULTS + "/" + linksID + ".nq";
			namedGraphs = new HashSet<String>();
		} else {
			this.directoryLinks = directoryPath + "/" + DIRECTORY_NAME_RESULTS + "/" + linksID + ".csv";
		}
		openLinks();
	}

	public void openLinks() {
		try {
			streamLinks = FILE_UTILS.createFileStream(directoryLinks);
		} catch (IOException e) {
			LOG.logError("openLinks", "Error when creating the following links file: " + directoryLinks);
			e.printStackTrace();
		}
	}


	public void addToStream(String message) {
		FILE_UTILS.writeToOutputStream(streamLinks, message);
	}


	public String createObjectPropertyTriple(String subj, String pred, String obj) {
		String result = "<" + subj + "> "
				+ "<" + pred + "> "
				+ "<"+ obj + "> .";
		return result;
	}

	public String createDataPropertyTriple(String subj, String pred, String obj) {
		String result = "<" + subj + "> "
				+ "<" + pred + "> "
				+ "\""+ obj + "\" .";
		return result;
	}


	public String createQuad(String subj, String pred, String obj, String graph) {
		String result = "<" + subj + "> "
				+ "<" + pred + "> "
				+ "<" + obj + "> "
				+ "<"+ graph + "> .";
		return result;
	}


	public void saveLinks(SingleMatch match, String identityLink) {
		counterFlush++;
		if(formatRDF == true) {
			// In RDF we save 2 or 3 links per match because we save links between each person in the certificate
			String link = transformMatchToRDF(match, identityLink);
			FILE_UTILS.writeToOutputStream(streamLinks, link);
		} else {
			// In CSV we save 1 link per match because we save links between certificates not the persons
			String link = transformMatchToCSV(match);
			FILE_UTILS.writeToOutputStream(streamLinks, link);
		}
		if(counterFlush == 20) {
			counterFlush = 0;
			flushLinks();
		}
	}
	
	public void saveLinks(SingleMatch match1, SingleMatch match2, String identityLink) {
		if(formatRDF == true) {
			String link1 = transformMatchToRDF(match1, identityLink);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link1);
			String link2 = transformMatchToRDF(match2, identityLink);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link2);
		} else {
			String link = transformMatchToCSV(match1);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link);
		}
		if(counterFlush >= 20) {
			counterFlush = 0;
			flushLinks();
		}
	}
	
	public void saveLinks(SingleMatch match1, SingleMatch match2, SingleMatch match3, String identityLink) {
		if(formatRDF == true) {
			String link1 = transformMatchToRDF(match1, identityLink);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link1);
			String link2 = transformMatchToRDF(match2, identityLink);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link2);
			String link3 = transformMatchToRDF(match3, identityLink);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link3);
		} else {
			String link = transformMatchToCSV(match1);
			counterFlush++;
			FILE_UTILS.writeToOutputStream(streamLinks, link);
		}
		if(counterFlush >= 20) {
			counterFlush = 0;
			flushLinks();
		}
	}


	public String transformMatchToRDF(SingleMatch match, String identityLink) {
		String graph = createNamedGraph(linksID, match.getLevDistance(), match.getMatchedNames());
		namedGraphs.add(linksID + "/" + match.getLevDistance() + "/" + match.getMatchedNames());
		String link = createQuad(match.getSourcePerson().getURI(), identityLink, match.getTargetPerson().getURI(), graph);
		return link;
	}


	public String transformMatchToCSV(SingleMatch match) {
		String link = match.getSourceCertificateID() + "," + match.getTargetCertificateID() + "," + match.getLevDistance() + "," + match.getMatchType()  + "," + match.getMatchedNames() + "," + match.getYearDifference();
		return link;
	}


	public String createNamedGraph(String processName, String levDistance, String matchedNames) {
		String graph = PREFIX_IISG + "graph/" +  processName + "/" + levDistance + "/" + matchedNames ;
		return graph;
	}



	public void writeGraphsMetadata() {
		for(String ng: namedGraphs) {	
			String meta[] = ng.split("/");
			int numberOfMatchedInvididuals = (meta[2].split("-")).length;
			String ngURI = createNamedGraph(meta[0], meta[1], meta[2]);
			addToStream(createDataPropertyTriple(ngURI, META_GRAPH_TITLE, meta[0]));
			addToStream(createDataPropertyTriple(ngURI, META_LEVENSHTEIN, meta[1]));
			addToStream(createDataPropertyTriple(ngURI, META_MATCHED_INVIDIUALS, meta[2]));
			String description = "A link in this graph is detected with a Levenshtein distance of "
					+ meta[1] + " respectively on the "
					+ numberOfMatchedInvididuals + " following individuals "
					+ meta[2] + ".";
			addToStream(createDataPropertyTriple(ngURI, META_MATCHED_INVIDIUALS, description));
			flushLinks();
		}
	}


	



	public Boolean flushLinks() {
		try {
			streamLinks.flush();
			return true;
		} catch (IOException e) {
			LOG.logError("flushLinks", "Error when flushing links file: " + linksID);
			e.printStackTrace();
			return false;
		}
	}

	public Boolean closeStream() {
		try {
			if(formatRDF == true) {
				writeGraphsMetadata();
			}
			streamLinks.close();
			return true;
		} catch (IOException e) {
			LOG.logError("closeStream", "Error when closing links file: " + linksID);
			e.printStackTrace();
			return false;
		}
	}





}
