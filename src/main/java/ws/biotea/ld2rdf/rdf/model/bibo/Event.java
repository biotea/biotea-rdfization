/**
 * pubmed.endNote.jaxb.generated by http://RDFReactor.semweb4j.org ($Id: CodeGenerator.java 1535 2008-09-09 15:44:46Z max.at.xam.de $) on 13/01/11 08:05 PM
 */
package ws.biotea.ld2rdf.rdf.model.bibo;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.PlainLiteralImpl;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdfreactor.runtime.Base;
import org.ontoware.rdfreactor.runtime.ReactorResult;

/**
 * This class manages access to these properties:
 * <ul>
 *   <li> Organizer </li>
 * </ul>
 *
 * This class was pubmed.endNote.jaxb.generated by <a href="http://RDFReactor.semweb4j.org">RDFReactor</a> on 13/01/11 08:05 PM
 */
public class Event extends Thing {

    /** http://purl.org/NET/c4dm/event.owl#Event */
    @SuppressWarnings("hiding")
	public static final URI RDFS_CLASS = new URIImpl("http://purl.org/NET/c4dm/event.owl#Event", false);

    /** http://purl.org/ontology/bibo/organizer */
    @SuppressWarnings("hiding")
	public static final URI ORGANIZER = new URIImpl("http://purl.org/ontology/bibo/organizer",false);

    /** 
     * All property-URIs with this class as domain.
     * All properties of all super-classes are also available. 
     */
    @SuppressWarnings("hiding")
    public static final URI[] MANAGED_URIS = {
      new URIImpl("http://purl.org/ontology/bibo/organizer",false) 
    };


	// protected constructors needed for inheritance
	
	/**
	 * Returns a Java wrapper over an RDF object, identified by URI.
	 * Creating two wrappers for the same instanceURI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.semweb4j.org
	 * @param classURI URI of RDFS class
	 * @param instanceIdentifier Resource that identifies this instance
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c1] 
	 */
	protected Event ( Model model, URI classURI, org.ontoware.rdf2go.model.node.Resource instanceIdentifier, boolean write ) {
		super(model, classURI, instanceIdentifier, write);
	}

	// public constructors

	/**
	 * Returns a Java wrapper over an RDF object, identified by URI.
	 * Creating two wrappers for the same instanceURI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param instanceIdentifier an RDF2Go Resource identifying this instance
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c2] 
	 */
	public Event ( Model model, org.ontoware.rdf2go.model.node.Resource instanceIdentifier, boolean write ) {
		super(model, RDFS_CLASS, instanceIdentifier, write);
	}


	/**
	 * Returns a Java wrapper over an RDF object, identified by a URI, given as a String.
	 * Creating two wrappers for the same URI is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param uriString a URI given as a String
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 * @throws ModelRuntimeException if URI syntax is wrong
	 *
	 * [Generated from RDFReactor template rule #c7] 
	 */
	public Event ( Model model, String uriString, boolean write) throws ModelRuntimeException {
		super(model, RDFS_CLASS, new URIImpl(uriString,false), write);
	}

	/**
	 * Returns a Java wrapper over an RDF object, identified by a blank node.
	 * Creating two wrappers for the same blank node is legal.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param bnode BlankNode of this instance
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c8] 
	 */
	public Event ( Model model, BlankNode bnode, boolean write ) {
		super(model, RDFS_CLASS, bnode, write);
	}

	/**
	 * Returns a Java wrapper over an RDF object, identified by 
	 * a randomly pubmed.endNote.jaxb.generated URI.
	 * Creating two wrappers results in different URIs.
	 * @param model RDF2GO Model implementation, see http://rdf2go.ontoware.org
	 * @param write if true, the statement (this, rdf:type, TYPE) is written to the model
	 *
	 * [Generated from RDFReactor template rule #c9] 
	 */
	public Event ( Model model, boolean write ) {
		super(model, RDFS_CLASS, model.newRandomUniqueURI(), write);
	}

    ///////////////////////////////////////////////////////////////////
    // typing

	/**
	 * Return an existing instance of this class in the model. No statements are written.
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 * @return an instance of Event  or null if none existst
	 *
	 * [Generated from RDFReactor template rule #class0] 
	 */
	public static Event  getInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getInstance(model, instanceResource, Event.class);
	}

	/**
	 * Create a new instance of this class in the model. 
	 * That is, create the statement (instanceResource, RDF.type, http://purl.org/NET/c4dm/event.owl#Event).
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #class1] 
	 */
	public static void createInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.createInstance(model, RDFS_CLASS, instanceResource);
	}

	/**
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 * @return true if instanceResource is an instance of this class in the model
	 *
	 * [Generated from RDFReactor template rule #class2] 
	 */
	public static boolean hasInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.hasInstance(model, RDFS_CLASS, instanceResource);
	}

	/**
	 * @param model an RDF2Go model
	 * @return all instances of this class in Model 'model' as RDF resources
	 *
	 * [Generated from RDFReactor template rule #class3] 
	 */
	public static ClosableIterator<org.ontoware.rdf2go.model.node.Resource> getAllInstances(Model model) {
		return Base.getAllInstances(model, RDFS_CLASS, org.ontoware.rdf2go.model.node.Resource.class);
	}

	/**
	 * @param model an RDF2Go model
	 * @return all instances of this class in Model 'model' as a ReactorResult,
	 * which can conveniently be converted to iterator, list or array.
	 *
	 * [Generated from RDFReactor template rule #class3-as] 
	 */
	public static ReactorResult<? extends Event> getAllInstances_as(Model model) {
		return Base.getAllInstances_as(model, RDFS_CLASS, Event.class );
	}

    /**
	 * Remove rdf:type Event from this instance. Other triples are not affected.
	 * To delete more, use deleteAllProperties
	 * @param model an RDF2Go model
	 * @param instanceResource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #class4] 
	 */
	public static void deleteInstance(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.deleteInstance(model, RDFS_CLASS, instanceResource);
	}

	/**
	 * Delete all (this, *, *), i.e. including rdf:type
	 * @param model an RDF2Go model
	 * @param resource
	 */
	public static void deleteAllProperties(Model model,	org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.deleteAllProperties(model, instanceResource);
	}

    ///////////////////////////////////////////////////////////////////
    // property access methods


    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@1e0ab12 has at least one value set 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return true if this property has at least one value
	 *
	 * [Generated from RDFReactor template rule #get0has-static] 
     */
	public static boolean hasbiboOrganizer(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.has(model, instanceResource, ORGANIZER);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@1e0ab12 has at least one value set 
     * @return true if this property has at least one value
	 *
	 * [Generated from RDFReactor template rule #get0has-dynamic] 
     */
	public boolean hasbiboOrganizer() {
		return Base.has(this.model, this.getResource(), ORGANIZER);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@1e0ab12 has the given value (maybe among other values).  
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be checked
     * @return true if this property contains (maybe among other) the given value
	 *
	 * [Generated from RDFReactor template rule #get0has-value-static] 
     */
	public static boolean hasbiboOrganizer(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value ) {
		return Base.hasValue(model, instanceResource, ORGANIZER);
	}

    /**
     * Check if org.ontoware.rdfreactor.generator.java.JProperty@1e0ab12 has the given value (maybe among other values).  
	 * @param value the value to be checked
     * @return true if this property contains (maybe among other) the given value
	 *
	 * [Generated from RDFReactor template rule #get0has-value-dynamic] 
     */
	public boolean hasbiboOrganizer( org.ontoware.rdf2go.model.node.Node value ) {
		return Base.hasValue(this.model, this.getResource(), ORGANIZER);
	}

     /**
     * Get all values of property Organizer as an Iterator over RDF2Go nodes 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ClosableIterator of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get7static] 
     */
	public static ClosableIterator<org.ontoware.rdf2go.model.node.Node> getAllbiboOrganizer_asNode(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_asNode(model, instanceResource, ORGANIZER);
	}
	
    /**
     * Get all values of property Organizer as a ReactorResult of RDF2Go nodes 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ListE of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get7static-reactor-result] 
     */
	public static ReactorResult<org.ontoware.rdf2go.model.node.Node> getAllbiboOrganizer_asNode_(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_as(model, instanceResource, ORGANIZER, org.ontoware.rdf2go.model.node.Node.class);
	}

    /**
     * Get all values of property Organizer as an Iterator over RDF2Go nodes 
     * @return a ClosableIterator of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get8dynamic] 
     */
	public ClosableIterator<org.ontoware.rdf2go.model.node.Node> getAllbiboOrganizer_asNode() {
		return Base.getAll_asNode(this.model, this.getResource(), ORGANIZER);
	}

    /**
     * Get all values of property Organizer as a ReactorResult of RDF2Go nodes 
     * @return a ListE of RDF2Go Nodes
	 *
	 * [Generated from RDFReactor template rule #get8dynamic-reactor-result] 
     */
	public ReactorResult<org.ontoware.rdf2go.model.node.Node> getAllbiboOrganizer_asNode_() {
		return Base.getAll_as(this.model, this.getResource(), ORGANIZER, org.ontoware.rdf2go.model.node.Node.class);
	}
     /**
     * Get all values of property Organizer     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ClosableIterator of $type
	 *
	 * [Generated from RDFReactor template rule #get11static] 
     */
	public static ClosableIterator<Agent> getAllbiboOrganizer(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll(model, instanceResource, ORGANIZER, Agent.class);
	}
	
    /**
     * Get all values of property Organizer as a ReactorResult of Agent 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
     * @return a ReactorResult of $type which can conveniently be converted to iterator, list or array
	 *
	 * [Generated from RDFReactor template rule #get11static-reactorresult] 
     */
	public static ReactorResult<Agent> getAllbiboOrganizer_as(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		return Base.getAll_as(model, instanceResource, ORGANIZER, Agent.class);
	}

    /**
     * Get all values of property Organizer     * @return a ClosableIterator of $type
	 *
	 * [Generated from RDFReactor template rule #get12dynamic] 
     */
	public ClosableIterator<Agent> getAllbiboOrganizer() {
		return Base.getAll(this.model, this.getResource(), ORGANIZER, Agent.class);
	}

    /**
     * Get all values of property Organizer as a ReactorResult of Agent 
     * @return a ReactorResult of $type which can conveniently be converted to iterator, list or array
	 *
	 * [Generated from RDFReactor template rule #get12dynamic-reactorresult] 
     */
	public ReactorResult<Agent> getAllbiboOrganizer_as() {
		return Base.getAll_as(this.model, this.getResource(), ORGANIZER, Agent.class);
	}
 
    /**
     * Adds a value to property Organizer as an RDF2Go node 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #add1static] 
     */
	public static void addbiboOrganizer( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.add(model, instanceResource, ORGANIZER, value);
	}
	
    /**
     * Adds a value to property Organizer as an RDF2Go node 
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #add1dynamic] 
     */
	public void addbiboOrganizer( org.ontoware.rdf2go.model.node.Node value) {
		Base.add(this.model, this.getResource(), ORGANIZER, value);
	}
    /**
     * Adds a value to property Organizer from an instance of Agent 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #add3static] 
     */
	public static void addbiboOrganizer(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, Agent value) {
		Base.add(model, instanceResource, ORGANIZER, value);
	}
	
    /**
     * Adds a value to property Organizer from an instance of Agent 
	 *
	 * [Generated from RDFReactor template rule #add4dynamic] 
     */
	public void addbiboOrganizer(Agent value) {
		Base.add(this.model, this.getResource(), ORGANIZER, value);
	}
  

    /**
     * Sets a value of property Organizer from an RDF2Go node.
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be set
	 *
	 * [Generated from RDFReactor template rule #set1static] 
     */
	public static void setbiboOrganizer( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.set(model, instanceResource, ORGANIZER, value);
	}
	
    /**
     * Sets a value of property Organizer from an RDF2Go node.
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set1dynamic] 
     */
	public void setbiboOrganizer( org.ontoware.rdf2go.model.node.Node value) {
		Base.set(this.model, this.getResource(), ORGANIZER, value);
	}
    /**
     * Sets a value of property Organizer from an instance of Agent 
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set3static] 
     */
	public static void setbiboOrganizer(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, Agent value) {
		Base.set(model, instanceResource, ORGANIZER, value);
	}
	
    /**
     * Sets a value of property Organizer from an instance of Agent 
     * First, all existing values are removed, then this value is added.
     * Cardinality constraints are not checked, but this method exists only for properties with
     * no minCardinality or minCardinality == 1.
	 * @param value the value to be added
	 *
	 * [Generated from RDFReactor template rule #set4dynamic] 
     */
	public void setbiboOrganizer(Agent value) {
		Base.set(this.model, this.getResource(), ORGANIZER, value);
	}
  


    /**
     * Removes a value of property Organizer as an RDF2Go node 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove1static] 
     */
	public static void removebiboOrganizer( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, org.ontoware.rdf2go.model.node.Node value) {
		Base.remove(model, instanceResource, ORGANIZER, value);
	}
	
    /**
     * Removes a value of property Organizer as an RDF2Go node
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove1dynamic] 
     */
	public void removebiboOrganizer( org.ontoware.rdf2go.model.node.Node value) {
		Base.remove(this.model, this.getResource(), ORGANIZER, value);
	}
    /**
     * Removes a value of property Organizer given as an instance of Agent 
     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove3static] 
     */
	public static void removebiboOrganizer(Model model, org.ontoware.rdf2go.model.node.Resource instanceResource, Agent value) {
		Base.remove(model, instanceResource, ORGANIZER, value);
	}
	
    /**
     * Removes a value of property Organizer given as an instance of Agent 
	 * @param value the value to be removed
	 *
	 * [Generated from RDFReactor template rule #remove4dynamic] 
     */
	public void removebiboOrganizer(Agent value) {
		Base.remove(this.model, this.getResource(), ORGANIZER, value);
	}
  
    /**
     * Removes all values of property Organizer     * @param model an RDF2Go model
     * @param resource an RDF2Go resource
	 *
	 * [Generated from RDFReactor template rule #removeall1static] 
     */
	public static void removeAllbiboOrganizer( Model model, org.ontoware.rdf2go.model.node.Resource instanceResource) {
		Base.removeAll(model, instanceResource, ORGANIZER);
	}
	
    /**
     * Removes all values of property Organizer	 *
	 * [Generated from RDFReactor template rule #removeall1dynamic] 
     */
	public void removeAllbiboOrganizer() {
		Base.removeAll(this.model, this.getResource(), ORGANIZER);
	}

	/*EXTENSION*/
	public static final String EVENT_NS = "http://purl.org/NET/c4dm/event.owl#";
	public static final URI EVENT_PLACE = new URIImpl(EVENT_NS + "place", false);
	
	public void addPlace(Model model, String place) {
		Node node = new PlainLiteralImpl(place);
		Statement stm = model.createStatement(this.asResource(), EVENT_PLACE, node);
	    model.addStatement(stm); //name
	}
 }