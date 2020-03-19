package iisg.amsterdam.wp4_links;

public class SingleMatch {
	
	private Person sourcePerson, targetPerson;
	private String sourceCertificateID, targetCertificateID;
	private int yearDifference;
	private String levDistance, matchedNames, matchType;
	
	public SingleMatch(Person sourcePerson, String sourceCertificateID, Person targetPerson, String targetCertificateID, String levDist, String matchedNames, String matchType, int yearDiff) {
		setSourcePerson(sourcePerson);
		setSourceCertificateID(sourceCertificateID);
		setTargetPerson(targetPerson);
		setTargetCertificateID(targetCertificateID);
		setLevDistance(levDist);
		setMatchedNames(matchedNames);
		setMatchType(matchType);
		setYearDifference(yearDiff);
	}

	public Person getSourcePerson() {
		return sourcePerson;
	}

	public void setSourcePerson(Person source) {
		this.sourcePerson = source;
	}

	public Person getTargetPerson() {
		return targetPerson;
	}

	public void setTargetPerson(Person target) {
		this.targetPerson = target;
	}

	public int getYearDifference() {
		return yearDifference;
	}

	public void setYearDifference(int yearDifference) {
		this.yearDifference = yearDifference;
	}

	public String getLevDistance() {
		return levDistance;
	}

	public void setLevDistance(String levDistance) {
		this.levDistance = levDistance;
	}

	public String getMatchedNames() {
		return matchedNames;
	}

	public void setMatchedNames(String matchedNames) {
		this.matchedNames = matchedNames;
	}

	public String getSourceCertificateID() {
		String[] bits = sourceCertificateID.split("/");
		return bits[bits.length-1];
		//return sourceCertificateID;
	}

	public void setSourceCertificateID(String sourceCertificateID) {
		this.sourceCertificateID = sourceCertificateID;
	}

	public String getTargetCertificateID() {
		String[] bits = targetCertificateID.split("/");
		return bits[bits.length-1];
		//return targetCertificateID;
	}

	public void setTargetCertificateID(String targetCertificateID) {
		this.targetCertificateID = targetCertificateID;
	}

	public String getMatchType() {
		return matchType;
	}

	public void setMatchType(String matchType) {
		this.matchType = matchType;
	}
	
	

}


// certificate1 ID , certificate2 ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
// newborn ID , partner ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
// mother ID , mother ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
// father ID , father ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
