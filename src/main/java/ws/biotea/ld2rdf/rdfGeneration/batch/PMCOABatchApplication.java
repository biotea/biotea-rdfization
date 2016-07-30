package ws.biotea.ld2rdf.rdfGeneration.batch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ws.biotea.ld2rdf.rdfGeneration.exception.ArticleTypeException;
import ws.biotea.ld2rdf.rdfGeneration.exception.DTDException;
import ws.biotea.ld2rdf.rdfGeneration.exception.PMCIdException;
import ws.biotea.ld2rdf.rdfGeneration.pmcoa.PmcOpenAccessHelper;

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
		
		String usage = ("Usage: -in <xml papers dir> -out <output dir> -sections " + 
				"\n-sections (optional) If present, paper sections will be rdfized");
		
		if (args == null) {
			System.out.println(usage);
			System.exit(0);
		}
		PropertyConfigurator.configure("log4j.properties");					
		
		int initPool = 10, maxPool = 10, keepAlive = 300;
		String inputDir = null, outputDir = null;
		boolean sections = false;
		Integer limit = null;
		//boolean addIssuedDate = false;
		for (int i = 0; i < args.length; i++) {
			String str = args[i];
			if (str.equalsIgnoreCase("-in")) {
				inputDir = args[++i];
			} else if (str.equalsIgnoreCase("-out")) {
				outputDir = args[++i];
			} else if (str.equalsIgnoreCase("-sections")) {
				sections = true;
			} else if (str.equalsIgnoreCase("-initPool")) {
				initPool = Integer.parseInt(args[++i]);
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
			"\nInitPool " + initPool + " MaxPool " + maxPool + " KeepAlive " + keepAlive);
		System.out.println("sections: " + sections);		
		
		PMCOABatchApplication handler = new PMCOABatchApplication(initPool, maxPool, keepAlive);
		handler.rdfizeDirectory(inputDir, outputDir, sections, limit);
		
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
	public void rdfizeDirectory(String inputDir, final String outputDir, final boolean sections, Integer limit) {
		File dir = new File(inputDir);
		int count = 1;
		for (final File subdir:dir.listFiles()) {			
			if (subdir.isDirectory()) { //only one level
				//this.processDirectory(pipeline, subdir.getAbsolutePath(), outputDir, sections, limit, i);
			} else {	
				if (subdir.getName().endsWith(".nxml")) {
					this.runTask(new Runnable() {
		                public void run() {
		                	try {
		                		PmcOpenAccessHelper helper = new PmcOpenAccessHelper();
								helper.rdfizeFile(subdir, outputDir, sections);
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
