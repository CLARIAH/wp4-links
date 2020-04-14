package iisg.amsterdam.wp4_links;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class Person {
	private String URI;
	private String role;
	private String first_name = null;
	private String last_name = null;
	private String gender = null;
	private final String compound_name_separator = "_";
	private Boolean valid;

	public Person() {
		setValid(false);
	}

	public Person(CharSequence URI, String role) {
		this.URI = URI.toString();
		this.role = role;
		setValid(true);
	}

	public void setFirstName(String first_name){
		this.first_name = first_name;
	}

	public void setLastName(String last_name){
		this.last_name = last_name;
	}

	public void setGender(String gender){
		this.gender = gender;
	}

	public String getURI() {
		return URI;
	}

	public String getRole() {
		return role;
	}

	public String getFirstName() {
		if(first_name!=null){
			String modified_first_name = first_name.replace(" ", compound_name_separator);
			modified_first_name = modified_first_name.replace("-", compound_name_separator);
			return modified_first_name;
		} else {
			return null;
		}		
	}

	public String getLastName() {
		if(last_name!=null) {
			String modified_last_name = last_name.replace(" ", compound_name_separator);
			modified_last_name = modified_last_name.replace("-", compound_name_separator);
			return modified_last_name;
		} else {
			return null;
		}	
	}

	public String getFullName() {
		String fullName = getFirstName() + " " + getLastName();
		return fullName;
	}


	public HashMap<String, String> getPossibleFullNameCombinations(){
		HashMap<String,String> fullNames = new HashMap<String,String>();
		String firstName = getFirstName();
		if(firstName.contains(compound_name_separator)) {
			String[] firstNames = firstName.split(compound_name_separator);
			fullNames = orderedCombination(firstNames, getLastName());
		} else {
			fullNames.put(getFullName(), "1/1");
		}
		return fullNames;
	}


	public HashMap<String,String> orderedCombination(String[] firstNames, String lastName) {
		HashMap<String,String> result = new HashMap<String,String>();
		Integer length = firstNames.length;
		String[] copiedFirstNames = firstNames.clone();
		for (int i=0; i< length; i++){
			String fixed = firstNames[i];
			int count = 1;
			result.put(fixed + " " + lastName, count+ "/" +length);
			copiedFirstNames = ArrayUtils.removeElement(copiedFirstNames, fixed);
			for (String fn: copiedFirstNames){
				count++;
				fixed = fixed + compound_name_separator + fn;
				result.put(fixed + " " + lastName, count + "/" + length);
			}
		}
		return result;
	}



	public String getGender() {
		return gender;
	}

	public void printIdentity() {
		System.out.println("+-------------------------------------------------------------------------------");
		System.out.println("* Person: " + URI);
		System.out.println(" ---> First Name: " + first_name);
		System.out.println(" ---> Last Name: " + last_name);
		System.out.println(" ---> Role: " + role);
		System.out.println(" ---> Gender: " + gender);
		System.out.println("+-------------------------------------------------------------------------------");
	}

	public Boolean hasFirstName() {
		if(first_name != null){
			return true;
		} else {
			return false;
		}
	}

	public Boolean hasLastName() {
		if(last_name != null){
			return true;
		} else {
			return false;
		}
	}

	public Boolean hasFullName() {
		if(first_name != null){
			if(last_name != null) {
				return true;
			}
		}
		return false;
	}

	public Boolean isFemale() {
		if(gender.equals("f")){
			return true;
		}
		return false;
	}

	public Boolean hasCompoundFirstName() {
		if (this.getFirstName() != null) {
			if(this.getFirstName().contains(compound_name_separator)) {
				return true;
			} 
		}
		return false;
	}



	public Boolean hasCompoundLastName() {
		if (this.getLastName() != null) {
			if(this.getLastName().contains(compound_name_separator)) {
				return true;
			} 
		}
		return false;
	}

	public String[] decomposeFirstname() {
		return this.getFirstName().split(compound_name_separator);
	}

	public String[] decomposeLastname() {
		return this.getLastName().split(compound_name_separator);
	}

	public Boolean isValid() {
		return valid;
	}

	public Boolean isValidWithFullName() {
		if(isValid()) {
			if(hasFullName()) {
				return true;
			}
		}
		return false;
	}

	public void setValid(Boolean valid) {
		this.valid = valid;
	}


}
