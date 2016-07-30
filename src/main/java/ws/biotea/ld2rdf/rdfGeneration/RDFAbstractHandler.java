package ws.biotea.ld2rdf.rdfGeneration;

import org.apache.log4j.Logger;

public abstract class RDFAbstractHandler implements RDFHandler {	
	protected Logger logger = Logger.getLogger(RDFAbstractHandler.class);
	protected String DATASET_URL;
	protected String BASE_PAPER_URL;
	protected String paperURLId;	
	protected String documentPaperId;
	
	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}
	/**
	 * @return the paperURLId
	 */
	public String getPaperURLId() {
		return paperURLId;
	}
	/**
	 * @param paperURLId the paperURLId to set
	 */
	public void setPaperURLId(String paperURLId) {
		this.paperURLId = paperURLId;
	}

	/**
	 * @return the documentPaperId
	 */
	public String getDocumentPaperId() {
		return documentPaperId;
	}
	/**
	 * @param documentPaperId the documentPaperId to set
	 */
	public void setDocumentPaperId(String documentPaperId) {
		this.documentPaperId = documentPaperId;
	}
}
