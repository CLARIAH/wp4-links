package iisg.amsterdam.wp4_links;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

import iisg.amsterdam.wp4_links.utilities.LoggingUtilities;

import static iisg.amsterdam.wp4_links.Properties.*;

public class MyHDT {

	public HDT dataset;

	public static final Logger lg = LogManager.getLogger(MyHDT.class);
	LoggingUtilities LOG = new LoggingUtilities(lg);

	/**
	 * Constructor
	 * 
	 * @param hdt_file_path
	 *            Path of the HDT file
	 */
	public MyHDT(String filePath) {
		try {
			LOG.outputConsole("START: Loading HDT Dataset");
			long startTime = System.currentTimeMillis();
			dataset = HDTManager.loadIndexedHDT(filePath, null);
			IteratorTripleString it;
			try {
				it = dataset.search("", "", "");
				LOG.outputTotalRuntime("Loading HDT Dataset", startTime, true);
				LOG.outputConsole("--- 	Estimated number of triples in dataset: " + it.estimatedNumResults() + " ---");
			} catch (NotFoundException e) {
				LOG.logError("MyHDT_Constructor", "Error estimating triples in HDT dataset");
				e.printStackTrace();
			}		
		} catch (IOException e) {
			LOG.logError("MyHDT_Constructor", "Error loading HDT dataset");
			e.printStackTrace();
		}	
	}


	public MyHDT(String inputRDF, String outputDir) {
		String baseURI = "file://" + inputRDF;
		String outputHDT = outputDir + "/dataset.hdt";
		RDFNotation notation = null;
		try {
			notation =  RDFNotation.guess(inputRDF);
		} catch (IllegalArgumentException e) {
			LOG.logError("MyHDT Constructor", "Could not guess notation for "+inputRDF+" - Trying NTriples...");
			notation = RDFNotation.NTRIPLES;
		}
		
		HDT hdt;
		try {
			hdt = HDTManager.generateHDT(inputRDF, baseURI, notation, new HDTSpecification(), null);
			hdt.saveToHDT(outputHDT, null);
			LOG.outputConsole("HDT saved to file in: " + outputHDT);
			hdt = HDTManager.indexedHDT(hdt, null);
			LOG.outputConsole("Index generated and saved in: " + outputHDT);
			hdt.close();
		} catch (IOException | ParserException e) {
			LOG.logError("MyHDT Constructor", "Problem generating HDT file");
			LOG.logError("MyHDT Constructor", e.getLocalizedMessage());
		} 
	}


	// ===== Statistics =====

	/**
	 * Returns the exact number of triples in this HDT file
	 * @param output
	 *            true for displaying the number of triples in the console
	 */
	public int getExactNumberOfTriples(){
		int counterTriples = 0;
		try {
			IteratorTripleString it = dataset.search("", "", "");
			while(it.hasNext()) {
				it.next();
				counterTriples++;
			}
		} catch (NotFoundException e) {
			LOG.logError("getExactNumberOfTriples", "Error counting triples in HDT dataset");
			e.printStackTrace();
		}
		return counterTriples;
	}



	/**
	 * Returns the number of registrations
	 * 
	 * @param certificate_type
	 *            "Birth_Certificate", 
	 *            "Marriage_Certificate" or
	 *            "Death_Certificate"  
	 */
	public int getNumberOfSubjects(String object){
		int counterRegistrations = 0;
		try {
			IteratorTripleString it = dataset.search("", RDF_TYPE, object);	
			while(it.hasNext()) {
				it.next();
				counterRegistrations++;		
			}
		} catch (NotFoundException e) {
			LOG.logError("getNumberOfSubjects", "Error counting subjects of object '" + object + "' in HDT dataset");
			e.printStackTrace();
		}
		return counterRegistrations;
	}




	// ===== RDF Utility Functions =====

	/**
	 * Returns the actual value of a typed literal
	 * (e.g. returns the String "John" from the input "John"^^xsd:string)
	 * 
	 * @param typed_literal         
	 */
	public String getStringValueFromLiteral(String typed_literal) {
		try {
			if (typed_literal != null) {
				typed_literal = typed_literal.substring(typed_literal.indexOf('"')+ 1);
				typed_literal = typed_literal.substring(0, typed_literal.indexOf('"'));
			}
		} catch (Exception e) {
			LOG.logError("getStringValueFromLiteral", "Error in converting Literal to String for input string: " + typed_literal);
			LOG.logError("getStringValueFromLiteral", e.getLocalizedMessage());
		}
		return typed_literal;
	}

	/**
	 * Converts a Java String to xsd:String
	 * (e.g. returns the String "John"^^xsd:string from the input Java String "John")
	 * 
	 * @param typed_literal         
	 */
	public String convertStringToTypedLiteral(String literal) {
		// "johannes franciescus"^^<http://www.w3.org/2001/XMLSchema#string>
		try {
			if (literal != null) {
				literal = '"' + literal + '"' + "^^<http://www.w3.org/2001/XMLSchema#string>";
			}
		} catch (Exception e) {
			LOG.logError("convertStringToTypedLiteral", "Error in converting String to Literal for input string: " + literal);
			LOG.logError("convertStringToTypedLiteral", e.getLocalizedMessage());
		}
		return literal;
	}

	/**
	 * Converts a Java String to xsd:String
	 * (e.g. returns the Integer "123"^^xsd:int from the input Java String "123")
	 * 
	 * @param typed_literal         
	 */
	public String convertStringToTypedInteger(String literal) {
		try {
			if (literal != null) {
				literal = '"' + literal + '"' + "^^<http://www.w3.org/2001/XMLSchema#int>";
			}
		} catch (Exception e) {
			LOG.logError("convertStringToTypedInteger", "Error in converting String to Integer for input string: " + literal);
			LOG.logError("convertStringToTypedInteger", e.getLocalizedMessage());
		}
		return literal;
	}


	public void closeDataset() {
		try {
			dataset.close();
		} catch (Exception e) {
			LOG.logError("closeDataset", "Error while closing HDT file");
			LOG.logError("closeDataset", e.getLocalizedMessage());
		}
	}


	// ===== Dataset Specific Functions =====

	/**
	 * Returns the ID provided in the original CSV file of a certain life event
	 * 
	 * @param eventURI
	 * 		the URI of a life event
	 */
	public String getIDofEvent(String eventURI) {
		// Of course the more correct way would be to query the HDT file and get the registration ID from the event URI
		String[] bits = eventURI.split("/");
		return bits[bits.length-1];
	}



	/**
	 * Returns an Object of the Java Class Person with their personal details (first name, last name, and gender) extracted from the HDT
	 * 
	 * @param event_uri
	 * 		URI referring to the event in which this individual is part of   
	 * @param role
	 * 		role of this person in this event with its acronym 
	 */
	public Person getPersonInfo(String eventURI, String role) {	
		try {
			IteratorTripleString it = dataset.search(eventURI, role, "");
			while(it.hasNext()) {
				TripleString ts = it.next();
				Person p = new Person(ts.getObject(), role);
				p.setFirstName(getFirstNameFromHDT(ts.getObject()));
				p.setLastName(getLastNameFromHDT(ts.getObject()));
				p.setGender(getGenderFromHDT(ts.getObject()));
				return p;
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return new Person();
	}

	/**
	 * Returns the object (?o) of the statement (URI, foaf:firstName, ?o)
	 * 
	 * @param URI
	 * 		URI referring to a certain person         
	 */
	public String getFirstNameFromHDT(CharSequence URI)
	{
		String first_name = null; 
		try {
			IteratorTripleString it = dataset.search(URI, GIVEN_NAME, "");
			if(it.hasNext()){
				TripleString ts = it.next();
				first_name = ts.getObject().toString();
				first_name = getStringValueFromLiteral(first_name);
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return first_name;
	}

	/**
	 * Returns the object (?o) of the statement (URI, foaf:lastName, ?o)
	 * 
	 * @param URI
	 * 		URI referring to a certain person         
	 */
	public String getLastNameFromHDT(CharSequence URI)
	{
		String last_name = null; 
		try {
			IteratorTripleString it = dataset.search(URI, FAMILY_NAME, "");
			if(it.hasNext()){
				TripleString ts = it.next();
				last_name = ts.getObject().toString();
				last_name = getStringValueFromLiteral(last_name);
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return last_name;
	}

	/**
	 * Returns the object (?o) of the statement (URI, foaf:gender, ?o)
	 * 
	 * @param URI
	 * 		URI referring to a certain person         
	 */
	public String getGenderFromHDT(CharSequence URI)
	{
		String gender = null; 
		try {
			IteratorTripleString it = dataset.search(URI, GENDER, "");
			if(it.hasNext()){
				TripleString ts = it.next();
				gender = ts.getObject().toString();
				gender = getStringValueFromLiteral(gender);
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return gender;
	}


	public int countNewbornsByGender(String gender) {
		int result = 0;
		try {
			IteratorTripleString it = dataset.search("", ROLE_NEWBORN, "");
			while(it.hasNext()){
				TripleString ts = it.next();
				String newborn = ts.getObject().toString();
				if(getGenderFromHDT(newborn).equals(gender)) {
					result++;
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
		return result;	
	}


	/**
	 * Returns the object (?o) of the statement (URI, iisg-vocab:event_date, ?o)
	 * 
	 * @param URI
	 * 		URI referring to a certain event         
	 */
	public int getEventDate(String eventURI) {
		try {
			IteratorTripleString it = dataset.search(eventURI, DATE, "");
			while(it.hasNext()) {
				TripleString ts = it.next();
				String[] bits = ts.getObject().toString().split("-");
				return Integer.parseInt(bits[0].replace("\"", ""));
			}
		} catch (NotFoundException e) {
			//e.printStackTrace();
		}
		return 0;
	}


	/**
	 * Returns the event URI from its ID provided in the original CSV file
	 * @param eventID
	 * 		the ID of this event
	 */
	public String getEventURIfromID(String eventID) {
		try {
			String typedEventID = convertStringToTypedInteger(eventID);
			IteratorTripleString it = dataset.search("", REGISTRATION_ID, typedEventID);
			while(it.hasNext()) {
				TripleString ts = it.next();
				String certificateURI = ts.getSubject().toString();
				if (certificateURI != null) {
					IteratorTripleString it2 = dataset.search(certificateURI, REGISTER_EVENT, "");
					while(it2.hasNext()) {
						TripleString ts2 = it2.next();
						String eventURI = ts2.getObject().toString();
						return eventURI;
					}
				} else {
					LOG.logError("getEventURIfromID", "The following eventID is not found in the dataset: " + eventID);
				}
			}
		} catch (NotFoundException e) {
			LOG.logError("getEventURIfromID", "The following eventID is not found in the dataset: " + eventID);
			e.printStackTrace();
		}
		return null;
	}
	
	
	public String getEventURIfromID(String eventID, String prov) {
		
		return PREFIX_IISG  + "event/" + eventID;

		// Of course the more correct way would be to query the HDT file and get the event URI from the registration ID		
		//return PREFIX_IISG + typeEvent + "/" + eventID;
		//return PREFIX_IISG  + "event/" + eventID;
	}


	/**
	 * Returns an Object of the Java Class Person with their personal details (first name, last name, and gender) extracted from the HDT
	 * 
	 * @param event_uri
	 * 		URI referring to the event in which this individual is part of   
	 * @param role
	 * 		role of this person in this event with its acronym (e.g. {"N", "https://iisg.amsterdam/links_zeeland/vocab/newborn"}) 
	 */
	//	public Person getPersonFromEvent(String event_uri, String[] role) {
	//		Person p = null;
	//		try {
	//			IteratorTripleString it = dataset.search(event_uri, role[1], "");
	//			while(it.hasNext()) {
	//				TripleString ts = it.next();
	//				p = new Person(ts.getObject(), role[0]);
	//			}
	//		} catch (NotFoundException e) {
	//			e.printStackTrace();
	//		}
	//		return p;
	//	}

}
