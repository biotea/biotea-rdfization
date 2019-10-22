package ws.biotea.ld2rdf.rdfGeneration.batch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ontoware.rdf2go.model.Syntax;

import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.pmcoa.PmcOpenAccessHelper;
import ws.biotea.ld2rdf.rdfGeneration.pmcoa.PmcOpenAccessText2PlainText;
import ws.biotea.ld2rdf.util.ResourceConfig;

/**
 * @author Leyla Garcia
 *
 */
public class PMCOABatchApplication {	
	//Poolthread sutff
    protected int poolSize;
    protected int maxPoolSize;
    protected long keepAliveTime;
    protected ThreadPoolExecutor threadPool = null;
    protected final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    private Logger logger = Logger.getLogger(PMCOABatchApplication.class);

    /**
     * Default constructor, it defines an initial pool with 5 threads, a maximum of 10 threads,
     * and a keepAlive time of 300 seconds.
     */
	public PMCOABatchApplication() {
		this(10, 10, 300);
	}
	
	/**
     * Constructor with parameters, it enables to define the initial pool size, maximum pool size,
     * and keep alive time in seconds; it initializes the ThreadPoolExecutor.
     * @param poolSize Initial pool size
     * @param maxPoolSize Maximum pool size
     * @param keepAliveTime Keep alive time in seconds
     */
    protected PMCOABatchApplication(int poolSize, int maxPoolSize, long keepAliveTime) {
    	this.poolSize = poolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, queue);        
    }
	
	/**
     * Run a task with the thread pool and modifies the waiting queue list as needed.
     * @param task
     */
    protected void runTask(Runnable task) {
        threadPool.execute(task);
        logger.debug("Task count: " + queue.size());
    }

    /**
     * Shuts down the ThreadPoolExecutor.
     */
    public void shutDown() {
        threadPool.shutdown();
    }

    /**
     * Informs whether or not the threads have finished all pending executions.
     * @return
     */
    public boolean isTerminated() {
    	//this.handler.getLogger().debug("Task count: " + queue.size());
        return this.threadPool.isTerminated();
    }
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ModelRuntimeException 
	 */
	public static void main(String[] args) throws IOException {	
		long startTime = System.currentTimeMillis();		
		
		String usage = ("Usage: -in <xml papers dir> -out <output dir> -sections -references" + 
				"\n-sections (optional) If present, paper sections will be rdfized; overrided by value in config.properties" +
				"\n-text (optional) If present, input will be transform to plain text" + 
				"\n-references (optional) If present, metadata for references will be generated, otherwise only links; overrided by value in config.properties" +
				"\n-format (optional) Either JSON-LD or XML, XML by default corresponding to RDF/XML");
		
		if (args == null) {
			System.out.println(usage);
			System.exit(0);
		}
		PropertyConfigurator.configure(PMCOABatchApplication.class.getResourceAsStream("/log4j.properties"));
		
		int initPool = 10, maxPool = 10, keepAlive = 300;
		String inputDir = null, outputDir = null;
		boolean sections = false, references = false, plainText = false;
		Integer limit = null;
		Syntax format = Syntax.RdfXml;
		//boolean addIssuedDate = false;
		for (int i = 0; i < args.length; i++) {
			String str = args[i];
			if (str.equalsIgnoreCase("-in")) {
				inputDir = args[++i];
			} else if (str.equalsIgnoreCase("-out")) {
				outputDir = args[++i];
			} else if (str.equalsIgnoreCase("-sections")) {
				sections = true;
			} else if (str.equalsIgnoreCase("-references")) {
				references = true;
			} else if (str.equalsIgnoreCase("-text")) {
				plainText = true;
			} else if (str.equalsIgnoreCase("-format")) {
				String fmt = args[++i];
				if (fmt.equalsIgnoreCase("JSON-LD")) {
					format = Syntax.JsonLd;
				}
			} else if (str.equalsIgnoreCase("-maxPool")) {
				maxPool = Integer.parseInt(args[++i]);
			} else if (str.equalsIgnoreCase("-keepAlive")) {
				keepAlive = Integer.parseInt(args[++i]);
			}
		}
		
		if ((inputDir == null) || (outputDir == null)) {
			System.out.println(usage);			
			System.out.println("The request cannot be parsed, please check the usage");
			System.exit(0);
		}
		
		System.out.println("Execution variables: " +
			"\nInput " + inputDir + "\nOutput " + outputDir + 
			"\nSections " + sections +
			"\nReferences " + references + 
			"\nInitPool " + initPool + " MaxPool " + maxPool + " KeepAlive " + keepAlive);		
		
		PMCOABatchApplication handler = new PMCOABatchApplication(initPool, maxPool, keepAlive);
		
		if (plainText) {
			handler.flatDirectory(inputDir, outputDir, sections, limit, format);
		} else {
			String[] configOptions = ResourceConfig.getConfigSuffixes();
			if (configOptions == null) {			
				String bioteaBase = ResourceConfig.getBioteaBase(null);
				String bioteaDataset = ResourceConfig.getBioteaDatasetURL(null, null); 
				boolean useBio2RDF = ResourceConfig.getUseBio2RDF(null);
				handler.rdfizeDirectory(inputDir, outputDir, bioteaBase, bioteaDataset, sections, references, "", useBio2RDF, limit, format);
			} else {
				for (String suffix: configOptions) {
					String bioteaBase = ResourceConfig.getConfigBase(suffix);
					String bioteaDataset = ResourceConfig.getConfigDataset(suffix);
					boolean useBio2RDF = ResourceConfig.getUseBio2RDF(bioteaBase);
					/* If sections param is not specified in the command line, take it from config file */
					if(!sections){
						sections = ResourceConfig.getConfigSections(suffix);
					}
					/* If references param is not specified in the command line, take it from config file */
					if(!references){
						references = ResourceConfig.getConfigReferences(suffix);
					}
					handler.rdfizeDirectory(inputDir, outputDir, bioteaBase, bioteaDataset, 
						sections, references, suffix, useBio2RDF, limit, format
					);
				}
			}
		}		
		
		handler.shutDown();
		while (!handler.isTerminated()); //waiting
		long endTime = System.currentTimeMillis();
		System.out.println("\nTotal time: " + (endTime-startTime));
	}
	
	/**
	 * Process n number of xml files in a directory to the given limit, converts files to RDF.
	 * @param inputDir
	 * @param outputDir
	 * @param sections
	 * @param limit
	 */
	private void rdfizeDirectory(String inputDir, final String outputDir, final String bioteaBase, final String bioteaDataset, 
			final boolean sections, final boolean references, String suffix, boolean useBio2RDF, Integer limit, Syntax format) {
		File dir = new File(inputDir);
		int count = 1;
		for (final File subdir:dir.listFiles()) {			
			if (subdir.isDirectory()) { //only one level
				//this.processDirectory(pipeline, subdir.getAbsolutePath(), outputDir, sections, limit, i);
			} else {	
				if (subdir.getName().endsWith(".nxml")) {
					this.runRDFization(subdir, outputDir, bioteaBase, bioteaDataset, sections, references, suffix, useBio2RDF, format);
				}
				count++;
			}
			if (count % 1000 == 0) {
				System.gc();
			}
		}
//		while (!queue.isEmpty() ) {
//			handler.logger.info("Waiting for " + queue.size() + " jobs");
//        }
	}
	
	private void runRDFization(final File subdir, final String outputDir, final String bioteaBase, final String bioteaDataset, 
			final boolean sections, final boolean references, final String suffix, final boolean useBio2RDF, final Syntax format) {		
		this.runTask(new Runnable() {
            public void run() {
            	try {
            		PmcOpenAccessHelper helper = new PmcOpenAccessHelper(bioteaBase, bioteaDataset);
					helper.rdfizeFile(subdir, outputDir, bioteaBase, bioteaDataset, sections, references, suffix, useBio2RDF, format);
					helper = null;
				} catch (Exception e) {
					logger.error(subdir.getName() + " could not be processed: " + e.getMessage());
					if ((e instanceof DTDException) || (e instanceof ArticleTypeException) || (e instanceof PMCIdException)) {
						
					} else {
						e.printStackTrace();
					}																
				}
            }
        });
	}
	
	/**
	 * Process n number of xml files in a directory to the given limit, converts files to RDF.
	 * @param inputDir
	 * @param outputDir
	 * @param sections
	 * @param limit
	 */
	private void flatDirectory(String inputDir, final String outputDir, final boolean sections, Integer limit, Syntax format) {
		File dir = new File(inputDir);
		int count = 1;
		for (final File subdir:dir.listFiles()) {			
			if (subdir.isDirectory()) { //only one level
				//this.processDirectory(pipeline, subdir.getAbsolutePath(), outputDir, sections, limit, i);
			} else {	
				if (subdir.getName().endsWith(".nxml")) {
					try {
						PmcOpenAccessText2PlainText flatten = new PmcOpenAccessText2PlainText(subdir, new StringBuilder(), "", "", "", sections, false, format);
						flatten.paper2rdf(outputDir, subdir);
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
				count++;
			}
			if (count % 1000 == 0) {
				System.gc();
			}
		}
//		while (!queue.isEmpty() ) {
//			handler.logger.info("Waiting for " + queue.size() + " jobs");
//        }
	}
}
