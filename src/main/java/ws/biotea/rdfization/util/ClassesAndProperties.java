package ws.biotea.rdfization.util;

public enum ClassesAndProperties {
	CNT_PROP_CONTENT(OntologyRDFizationPrefix.CNT.getNS(), OntologyRDFizationPrefix.CNT.getURL(), "chars");

	String value;
	String ns;
	String url;
	private ClassesAndProperties(String ns, String url, String value) {
		this.value = value;
		this.ns = ns;
		this.url = url;
	}
	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}	

	public String getNSValue() {
		return this.ns + ":" + this.value;
	}
	public String getURLValue() {
		return this.url + this.value;
	}
}
