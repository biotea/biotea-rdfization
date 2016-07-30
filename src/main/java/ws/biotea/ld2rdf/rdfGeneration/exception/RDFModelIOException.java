package ws.biotea.ld2rdf.rdfGeneration.exception;

public class RDFModelIOException extends Exception {
	private static final long serialVersionUID = 1L;
	public RDFModelIOException() {
		super();
	}
    public RDFModelIOException(String message) {
		super(message);
	} 
    public RDFModelIOException(String message, Throwable cause) {
		super(message, cause);
	}
    public RDFModelIOException(Throwable cause) {
    	super(cause);
    } 
}
