package ws.biotea.rdfization.util;  

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum OntologyRDFizationPrefix {
	//DOCO
	DOCO("http://purl.org/spar/doco/", "doco"),
	//BIBO
	BIBO("http://purl.org/ontology/bibo/", "bibo"),
	//Content in RDF
	CNT("http://www.w3.org/2011/content#", "cnt")
	;
	
	private String url;
	private String ns;
	
	private OntologyRDFizationPrefix(String url, String ns) {
		this.url = url;
		this.ns = ns;
	}
	public String getURL() {
		return (this.url);
	}
	public String getNS() {
		return (this.ns);
	}
	public String getPrefix() {
		return "PREFIX " + this.ns + ":<" + this.url + "> ";
	}
	public static OntologyRDFizationPrefix getByNS(String ns) throws NoSuchElementException {
		for (OntologyRDFizationPrefix prefix: OntologyRDFizationPrefix.values()) {
			if (prefix.getNS().equals(ns)) {
				return prefix;
			}
		}
		throw new NoSuchElementException("The prefix with NS " + ns + " cannot be resolved.");
	}
	public static String convertToNSAndTerm(String uri) throws NoSuchElementException {
		for (OntologyRDFizationPrefix prefix: OntologyRDFizationPrefix.values()) {
			if (uri.startsWith(prefix.getURL())) {
				String term = uri.substring(prefix.getURL().length());
				return prefix.getNS() + ":" + term;
			}
		}
		throw new NoSuchElementException("The prefix for a URI " + uri + " cannot be resolved.");
	}

	public static String prefixes_DOCO_BIBO() {	
		return OntologyRDFizationPrefix.DOCO.getPrefix() +
		OntologyRDFizationPrefix.BIBO.getPrefix() +
		OntologyRDFizationPrefix.CNT.getPrefix()
		;
	}
	public static Map<String, String> prefixesMap_DOCO_BIBO() {	
		Map<String, String> map = new HashMap<String, String>();
		map.put(OntologyRDFizationPrefix.DOCO.getNS(), OntologyRDFizationPrefix.DOCO.getURL());
		map.put(OntologyRDFizationPrefix.BIBO.getNS(), OntologyRDFizationPrefix.BIBO.getURL());
		map.put(OntologyRDFizationPrefix.CNT.getNS(), OntologyRDFizationPrefix.CNT.getURL());
		return map;
	}
}
