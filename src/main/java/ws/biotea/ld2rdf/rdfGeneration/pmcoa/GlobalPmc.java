package ws.biotea.ld2rdf.rdfGeneration.pmcoa;  

import ws.biotea.ld2rdf.util.ResourceConfig;
import ws.biotea.ld2rdf.util.Conversion;

public class GlobalPmc {
	private String uriStyle;
	private boolean initialized;	
	private String pmcID;
	
	//Biotea base URI
	public final static String BASE_URL = ResourceConfig.BIOTEA_URL;
	//dataset pattern
	public final static String BIOTEA_PMC_DATASET = ResourceConfig.BIOTEA_URL + ResourceConfig.getDataset();
	//Software agent
	public final static String RDF4PMC_AGENT = ResourceConfig.BIOTEA_RDFIZATOR;
	//others
	public final static String listOfAuthorsEditorsAndTranslators = "listOfAuthorsEditorsAndTranslators";	
	public final static String listOfEditorsAndTranslators = "listOfEditorsAndTranslators";	
	public final static String listOfEditors = "listOfEditors";
	public final static String listOfTranslators= "listOfTranslators";
	public final static String listOfAuthors = "listOfAuthors";
	// Additional URIs, fixed
	public final static String pmcURI = "http://www.ncbi.nlm.nih.gov/pmc/articles/PMC"; 	
	public final static String NLM_JOURNAL_CATALOG = "http://www.ncbi.nlm.nih.gov/nlmcatalog?term=";
	public final static String pubMedURI = "http://www.ncbi.nlm.nih.gov/pubmed/";
	public final static String doiURI = "http://dx.doi.org/";	
	
	//annotations pattern
    public static String BASE_URL_AO;
    //patterns
    public static String PUBMED_DOCUMENT;
    public static String DOI_DOCUMENT;
	String PMC_DOCUMENT;
	String CHEM_STRUCT_FIG_LINK;
	String INLINE_FORM_FIG_LINK;
	public static String BASE_URL_ISSUE;
	public static String BASE_URL_JOURNAL_ISSN;
	public static String BASE_URL_JOURNAL_NAME;
	public static String BASE_URL_PUBLISHER_ID;
	public static String BASE_URL_PUBLISHER_NAME;
	String BASE_URL_PERSON_PMC;
	public static String BASE_URL_PERSON_PUBMED;
	public static String BASE_URL_PERSON_DOI;
	String BASE_URL_ORGANIZATION_PMC;
	String BASE_URL_GROUP_PMC;
	public static String BASE_URL_ORGANIZATION_PUBMED;
	//final String BASE_URL_GROUP_PUBMED;
	public static String BASE_URL_ORGANIZATION_DOI;
	//final String BASE_URL_GROUP_DOI;
	String BASE_URL_REF;
	String BASE_URL_PROCEEDINGS_PMC;
	String BASE_URL_CONFERENCE_PMC;
	public static String BASE_URL_PROCEEDINGS_PUBMED;
	public static String BASE_URL_CONFERENCE_PUBMED;
	public static String BASE_URL_PROCEEDINGS_DOI;
	public static String BASE_URL_CONFERENCE_DOI;
	String BASE_URL_LIST_PMC;
	public static String BASE_URL_LIST_PUBMED;
	public static String BASE_URL_LIST_DOI;
	String BASE_URL_SECTION;
	String BASE_URL_EXT_FIGURE;
	String BASE_URL_EXT_TABLE;
	String BASE_URL_REF_LIST;
	String BASE_URL_APPENDIX;
	String BASE_URL_PARAGRAPH;
	
	/**
	 * Private constructor. 
	 */
	public GlobalPmc(String pmcID) {
		this.initialized = false;
		this.pmcID = pmcID;
		if (ResourceConfig.USE_BIO2RDF) {
			this.uriStyle = ResourceConfig.bio2rdf;
		} else {
			this.uriStyle = "Biotea";
		}
		this.initPatterns();
	}
	static {
		initStaticPatterns();
	}
	/**
	 * @return the uriStyle
	 */
	public String getUriStyle() {
		return uriStyle;
	}
	
	public String getPmcUri() {
		return GlobalPmc.pmcURI + this.pmcID;
	}
	
	public static String getPmcRdfUri(String pmcId) {
		if (ResourceConfig.USE_BIO2RDF) {
			//documents
			return ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + ":" + pmcId;
		} else {
			return ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "doc/" + ResourceConfig.getDatasetPrefix() + "/" + pmcId;
		}		
	}
	
	public static String getBaseURLAO() {
		if (ResourceConfig.USE_BIO2RDF) {
			//annotations
			return ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:annotation{0}_";//{0} should be replaced by the annotator + "/pubmedOpenAccess/rdf_ao/";
		} else {
			//annotations
			return ResourceConfig.BIOTEA_URL + "annotation{0}/" + ResourceConfig.getDatasetPrefix() + "_resource/";//{0} should be replaced by the annotator + "/pubmedOpenAccess/rdf_ao/";
		}
	}
	
	public String getAnnotationPmcRdfUri(String annotator) {
		if (!this.initialized) {
			this.initPatterns();
		}
		String[] params = {annotator};
		return Conversion.replaceParameter(BASE_URL_AO, params) + pmcID + "/";//GlobalPmc.BASE_URL + "PMC" + pmcId;// + ".rdf";
	}	
	private static void initStaticPatterns() {
		if (ResourceConfig.USE_BIO2RDF) {
			//annotations
			BASE_URL_AO = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:annotation{0}_";//{0} should be replaced by the annotator + "/pubmedOpenAccess/rdf_ao/";
			//documents
			PUBMED_DOCUMENT = BASE_URL + "pubmed:";
			DOI_DOCUMENT = BASE_URL + ResourceConfig.getDatasetPrefix() + "_resource:doi_";
			//JOURNAL (independent, and can be mapped to journal list in NIH)
			BASE_URL_JOURNAL_ISSN = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:journal_issn_";
			BASE_URL_JOURNAL_NAME = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:journal_name_";
			BASE_URL_ISSUE = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:journal_{0}_{1}_issue_"; //then issn:<id>/<issue_id> or name:<journal_name>/<issue_id>
			//PUBLISHER (independent but we cannot map it to a URL)
			BASE_URL_PUBLISHER_ID = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:publisher_" + ResourceConfig.getDatasetPrefix() + "_internal_id_";
			BASE_URL_PUBLISHER_NAME = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:publisher_name_";
			//PERSONS (we do not disambiguate so keep them as related to the document)
			BASE_URL_PERSON_PUBMED = ResourceConfig.BIOTEA_URL + "pubmed_resource:{0}_person_name_";
			BASE_URL_PERSON_DOI = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:doi_{0}_person_name_";
			//ORGANIZATION & GROUP (we do not disambiguate so keep them as related to the document)
			BASE_URL_ORGANIZATION_PUBMED = ResourceConfig.BIOTEA_URL + "pubmed_resource:{0}_organization_name_";
			BASE_URL_ORGANIZATION_DOI = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:doi_{0}_organization_name_";
			//REFERENCES without DOI or pubmedid
			//Lists
			BASE_URL_LIST_PUBMED = ResourceConfig.BIOTEA_URL + "pubmed_resource:{1}_{0}_list1"; //{0} list type, {1} pubmedid
			BASE_URL_LIST_DOI = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:doi_{1}_{0}_list1" ; //{0} list type, {1} doi
			//Proceedings (we do not disambiguate so keep them as related to the document)
			BASE_URL_PROCEEDINGS_PUBMED = ResourceConfig.BIOTEA_URL + "pubmed_resource:{0}_proceedings_title_";
			BASE_URL_PROCEEDINGS_DOI = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:doi_{0}_proceedings_title_";
			BASE_URL_CONFERENCE_PUBMED = ResourceConfig.BIOTEA_URL + "pubmed_resource:{0}_conference_name_";
			BASE_URL_CONFERENCE_DOI = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:doi_{0}_conference_name_";			
		} else {
			//annotations
			BASE_URL_AO = ResourceConfig.BIOTEA_URL + "annotation{0}/" + ResourceConfig.getDatasetPrefix() + "_resource/";//{0} should be replaced by the annotator + "/pubmedOpenAccess/rdf_ao/";
			//documents
			PUBMED_DOCUMENT = BASE_URL + "pubmeddoc/pubmed/";
			DOI_DOCUMENT = BASE_URL + "doidoc/doi/";
			//JOURNAL (independent, and can be mapped to journal list in NIH)
			BASE_URL_JOURNAL_ISSN = ResourceConfig.BIOTEA_URL + "journal/issn/";
			BASE_URL_JOURNAL_NAME = ResourceConfig.BIOTEA_URL + "journal/name/";
			BASE_URL_ISSUE = ResourceConfig.BIOTEA_URL + "issue/journal_resource/{0}:{1}/issue_"; //then issn:<id>/<issue_id> or name:<journal_name>/<issue_id>
			//PUBLISHER (independent but we cannot map it to a URL)
			BASE_URL_PUBLISHER_ID = ResourceConfig.BIOTEA_URL + "publisher/" + ResourceConfig.getDatasetPrefix() + "_internal_id/";
			BASE_URL_PUBLISHER_NAME = ResourceConfig.BIOTEA_URL + "publisher/name/";
			//PERSONS (we do not disambiguate so keep them as related to the document)
			BASE_URL_PERSON_PUBMED = ResourceConfig.BIOTEA_URL + "person/pubmeddoc_resource/{0}/name/";
			BASE_URL_PERSON_DOI = ResourceConfig.BIOTEA_URL + "person/doidoc_resource/{0}/name/";
			//ORGANIZATION & GROUP (we do not disambiguate so keep them as related to the document)
			BASE_URL_ORGANIZATION_PUBMED = ResourceConfig.BIOTEA_URL + "organization/pubmeddoc_resource/{0}/name/";
			//BASE_URL_GROUP_PUBMED = ResourceConfig.BIOTEA_URL + "group/pubmeddoc_resource/{0}/id/"; //then <org_name>_<group_id>
			BASE_URL_ORGANIZATION_DOI = ResourceConfig.BIOTEA_URL + "organization/doidoc_resource/{0}/name/";
			//BASE_URL_GROUP_DOI = ResourceConfig.BIOTEA_URL + "group/doidoc_resource/{0}/id/"; //then <org_name>_<group_id>
			//REFERENCES without DOI or pubmedid
			//Lists
			BASE_URL_LIST_PUBMED = ResourceConfig.BIOTEA_URL + "{0}/pubmeddoc_resource/{1}/list1"; //{0} list type, {1} pubmedid
			BASE_URL_LIST_DOI = ResourceConfig.BIOTEA_URL + "{0}/doidoc_resource/{1}/list1" ; //{0} list type, {1} doi
			//Proceedings (we do not disambiguate so keep them as related to the document)
			BASE_URL_PROCEEDINGS_PUBMED = ResourceConfig.BIOTEA_URL + "proceedings/pubmeddoc_resource/{0}/title/";
			BASE_URL_PROCEEDINGS_DOI = ResourceConfig.BIOTEA_URL + "proceedings/doidoc_resource/{0}/title/";
			BASE_URL_CONFERENCE_PUBMED = ResourceConfig.BIOTEA_URL + "conference/pubmeddoc_resource/{0}/name/";
			BASE_URL_CONFERENCE_DOI = ResourceConfig.BIOTEA_URL + "conference/doidoc_resource/{0}/name/";			
		}
	}
	private void initPatterns() {
		if (!this.initialized) {
			this.initialized = true;
			if (this.uriStyle.equalsIgnoreCase(ResourceConfig.bio2rdf)) {
				//documents
				PMC_DOCUMENT = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + ":" + pmcID;				
				//PERSONS (we do not disambiguate so keep them as related to the document)
				BASE_URL_PERSON_PMC = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_person_name_";
				//ORGANIZATION & GROUP (we do not disambiguate so keep them as related to the document)
				BASE_URL_ORGANIZATION_PMC = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_organization_name_";
				BASE_URL_GROUP_PMC = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_group_id_"; //then <org_name>_<group_id>
				//REFERENCES without DOI or pubmedid
				BASE_URL_REF = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_reference_";
				//Lists
				BASE_URL_REF_LIST = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_{0}_{1}_list1"; // {0} type of list, {1} ref id, for references without DOI or pubmedid
				BASE_URL_LIST_PMC = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_{0}_list1"; //{0} list type
				//Proceedings (we do not disambiguate so keep them as related to the document)
				BASE_URL_PROCEEDINGS_PMC = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_proceedings_title_";
				BASE_URL_CONFERENCE_PMC = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_conference_name_";
				//Sections
				BASE_URL_SECTION = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_section_";
				BASE_URL_PARAGRAPH = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_paragraph_{0}_para_"; //section-name:para-id
				BASE_URL_APPENDIX = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "_resource:" + pmcID + "_appendix_"; //TODO
				//External figures and tables
				BASE_URL_EXT_FIGURE = GlobalPmc.pmcURI + pmcID + "/figure/";
				BASE_URL_EXT_TABLE = GlobalPmc.pmcURI + pmcID + "/table/";
				INLINE_FORM_FIG_LINK = GlobalPmc.pmcURI + pmcID + "/bin/";
				CHEM_STRUCT_FIG_LINK = "http://www.ncbi.nlm.nih.gov/core/lw/2.0/html/tileshop_pmc/tileshop_pmc_inline.html?p=PMC3&id=" + pmcID + "_";
			} else {
				//documents
				PMC_DOCUMENT = ResourceConfig.BIOTEA_URL + ResourceConfig.getDatasetPrefix() + "doc/" + ResourceConfig.getDatasetPrefix() + "/" + pmcID;
				//PERSONS (we do not disambiguate so keep them as related to the document)
				BASE_URL_PERSON_PMC = ResourceConfig.BIOTEA_URL + "person/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/name/";
				//ORGANIZATION & GROUP (we do not disambiguate so keep them as related to the document)
				BASE_URL_ORGANIZATION_PMC = ResourceConfig.BIOTEA_URL + "organization/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/name/";
				BASE_URL_GROUP_PMC = ResourceConfig.BIOTEA_URL + "group/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/id/"; //then <org_name>_<group_id>
				//REFERENCES without DOI or pubmedid
				BASE_URL_REF = ResourceConfig.BIOTEA_URL + "reference/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/";
				//Lists
				BASE_URL_REF_LIST = ResourceConfig.BIOTEA_URL + "{0}/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/{1}:list1"; // {0} type of list, {1} ref id, for references without DOI or pubmedid
				BASE_URL_LIST_PMC = ResourceConfig.BIOTEA_URL + "{0}/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/list1"; //{0} list type
				//Proceedings (we do not disambiguate so keep them as related to the document)
				BASE_URL_PROCEEDINGS_PMC = ResourceConfig.BIOTEA_URL + "proceedings/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/title/";
				BASE_URL_CONFERENCE_PMC = ResourceConfig.BIOTEA_URL + "conference/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/name/";
				//Sections
				BASE_URL_SECTION = ResourceConfig.BIOTEA_URL + "section/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/";
				BASE_URL_PARAGRAPH = ResourceConfig.BIOTEA_URL + "paragraph/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/{0}:para_"; //section-name:para-id
				BASE_URL_APPENDIX = ResourceConfig.BIOTEA_URL + "appendix/" + ResourceConfig.getDatasetPrefix() + "doc_resource/" + pmcID + "/"; //TODO
				//External figures and tables
				BASE_URL_EXT_FIGURE = GlobalPmc.pmcURI + pmcID + "/figure/";
				BASE_URL_EXT_TABLE = GlobalPmc.pmcURI + pmcID + "/table/";
				INLINE_FORM_FIG_LINK = GlobalPmc.pmcURI + pmcID + "/bin/";
				CHEM_STRUCT_FIG_LINK = "http://www.ncbi.nlm.nih.gov/core/lw/2.0/html/tileshop_pmc/tileshop_pmc_inline.html?p=PMC3&id=" + pmcID + "_";
			}
		}		
	}
}
