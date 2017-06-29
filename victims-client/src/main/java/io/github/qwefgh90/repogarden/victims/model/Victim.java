package io.github.qwefgh90.repogarden.victims.model;

import java.util.List;

/**
 * A java model for victims-cve-db
 * https://github.com/victims/victims-cve-db
 * @author cheochangwon
 *
 */
public class Victim {
	String cve;
	String title;
	String description;
	String cvss_v2;
	List<String> references;
	List<JavaModule> affected;
	public String getCve() {
		return cve;
	}
	public void setCve(String cve) {
		this.cve = cve;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCvss_v2() {
		return cvss_v2;
	}
	public void setCvss_v2(String cvss_v2) {
		this.cvss_v2 = cvss_v2;
	}
	public List<String> getReferences() {
		return references;
	}
	public void setReferences(List<String> references) {
		this.references = references;
	}
	public List<JavaModule> getAffected() {
		return affected;
	}
	public void setAffected(List<JavaModule> affected) {
		this.affected = affected;
	}
	@Override
	public String toString() {
		return "Victim [cve=" + cve + ", title=" + title + ", description="
				+ description + ", cvss_v2=" + cvss_v2 + ", references="
				+ references + ", affected=" + affected + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((affected == null) ? 0 : affected.hashCode());
		result = prime * result + ((cve == null) ? 0 : cve.hashCode());
		result = prime * result + ((cvss_v2 == null) ? 0 : cvss_v2.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((references == null) ? 0 : references.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Victim other = (Victim) obj;
		if (affected == null) {
			if (other.affected != null)
				return false;
		} else if (!affected.equals(other.affected))
			return false;
		if (cve == null) {
			if (other.cve != null)
				return false;
		} else if (!cve.equals(other.cve))
			return false;
		if (cvss_v2 == null) {
			if (other.cvss_v2 != null)
				return false;
		} else if (!cvss_v2.equals(other.cvss_v2))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (references == null) {
			if (other.references != null)
				return false;
		} else if (!references.equals(other.references))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}
	public Victim(String cve, String title, String description, String cvss_v2,
			List<String> references, List<JavaModule> affected) {
		super();
		this.cve = cve;
		this.title = title;
		this.description = description;
		this.cvss_v2 = cvss_v2;
		this.references = references;
		this.affected = affected;
	}
	public Victim() {
		super();
	}
	
	
}

/* sample1
cve: 2017-3159
title: "Apache Camel's Snakeyaml unmarshalling operation is vulnerable to Remote Code Execution attacks"
description: >
    Apache Camel's camel-snakeyaml component is vulnerable to Java object de-serialization vulnerability. De-serializing untrusted data can lead to security flaws.
cvss_v2: 7.5
references:
    - http://camel.apache.org/security-advisories.data/CVE-2017-3159.txt.asc?version=1&modificationDate=1486565167000&api=v2
    - https://www.cvedetails.com/cve/CVE-2017-3159/
affected:
    - groupId: "org.apache.camel"
      artifactId: "camel-snakeyaml"
      version:
        - "<=2.17.4"
        - "<=2.18.1,2.18"
      fixedin:
        - ">=2.17.5,2.17"
        - ">=2.18.2,2.18"

*/
