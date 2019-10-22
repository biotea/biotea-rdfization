package ws.biotea.ld2rdf.rdfGeneration.orcid;

public interface OrcidService {
	/**
	 * Retrieve orcid from a given and a family name of an author, together with
	 * the publication doi.
	 * 
	 * @param doi Publication identifier (doi).
	 * @param givenName Author given name.
	 * @param familyName Author family name.
	 * @return The orcid of the author.
	 */
	String getOrcid(String doi, String givenName, String familyName);
}
