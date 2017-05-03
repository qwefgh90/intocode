package io.github.qwefgh90.repogarden.victims.model;

import java.util.List;

/**
 * A java module which is a part of Victim model.
 * @author cheochangwon
 *
 */
public class JavaModule {
	String groupId;
	String artifactId;
	List<String> version;
	List<String> fixedin;
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public List<String> getVersion() {
		return version;
	}
	public void setVersion(List<String> version) {
		this.version = version;
	}
	public List<String> getFixedin() {
		return fixedin;
	}
	public void setFixedin(List<String> fixedin) {
		this.fixedin = fixedin;
	}
	@Override
	public String toString() {
		return "JavaModule [groupId=" + groupId + ", artifactId=" + artifactId
				+ ", version=" + version + ", fixedin=" + fixedin + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((fixedin == null) ? 0 : fixedin.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		JavaModule other = (JavaModule) obj;
		if (artifactId == null) {
			if (other.artifactId != null)
				return false;
		} else if (!artifactId.equals(other.artifactId))
			return false;
		if (fixedin == null) {
			if (other.fixedin != null)
				return false;
		} else if (!fixedin.equals(other.fixedin))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
	public JavaModule(String groupId, String artifactId, List<String> version,
			List<String> fixedin) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.fixedin = fixedin;
	}
	public JavaModule() {
		super();
	}
	
}

/*
    - groupId: "org.apache.camel"
      artifactId: "camel-snakeyaml"
      version:
        - "<=2.17.4"
        - "<=2.18.1,2.18"
      fixedin:
        - ">=2.17.5,2.17"
        - ">=2.18.2,2.18"

*/