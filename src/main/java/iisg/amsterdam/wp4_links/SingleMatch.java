package iisg.amsterdam.wp4_links;

public class SingleMatch {
	
	private Person sourcePerson, targetPerson;
	private String sourceCertificateID, targetCertificateID;
	private int yearDifference;
	private String levDistance, matchedNames;
	
	public SingleMatch(Person sourcePerson, String sourceCertificateID, Person targetPerson, String targetCertificateID, String levDist, String matchedNames, int yearDiff) {
		setSourcePerson(sourcePerson);
		setSourceCertificateID(sourceCertificateID);
		setTargetPerson(targetPerson);
		setTargetCertificateID(targetCertificateID);
		setLevDistance(levDist);
		setMatchedNames(matchedNames);
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
		return sourceCertificateID;
	}

	public void setSourceCertificateID(String sourceCertificateID) {
		this.sourceCertificateID = sourceCertificateID;
	}

	public String getTargetCertificateID() {
		return targetCertificateID;
	}

	public void setTargetCertificateID(String targetCertificateID) {
		this.targetCertificateID = targetCertificateID;
	}
	
	

}


// certificate1 ID , certificate2 ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
// newborn ID , partner ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
// mother ID , mother ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
// father ID , father ID , yearDifference, numberOfMatchesNames, distanceIndividual1, distanceIndividual2, distanceIndividual3
