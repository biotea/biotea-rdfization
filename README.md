# biotea-rdfization
Refactorization for the RDFization code at https://github.com/alexgarciac/biotea.
RDF for metadata and content (optional) in PubMed and PMC NXML articles, compatible with Bio2RDF.

## Dependencies
All but one dependency are configured with Maven. There is a local dependency to https://github.com/biotea/biotea-utilities.

## Input
* Input files should follow the [JATS](https://jats.nlm.nih.gov/) DTDs
* In order to be able to process input articles in batch, those should be located in a folder as the one provided at [inputToProcess](https://github.com/biotea/biotea-rdfization/tree/master/src/main/resources/inputToProcess).
* Only files ending in .nxml will be process
* Make sure you delete the test file there named DELETE_ME_PMC3879346.nxml

## Output
One or two (if -sections run option is used) will be generated per each .nxml file in you input folder.
* DATASET-PREFIX_ARTICLE-ID.rdf containing all the metadata, e.g., authors, title, journal, etc.
* DATASET-PREFIX_ARTICLE-ID_sections.rdf, containing all the text content, i.e., section titles and paragraphs

## How run this project using the batch option
* Clone [biotea-utilities](https://github.com/biotea/biotea-utilities)
* Clone this repository
* In your IDE, create a dependency from this project to [biotea-utilities](https://github.com/biotea/biotea-utilities)
* Modify configuration files, i.e., config.properties, in [biotea-utilities](https://github.com/biotea/biotea-utilities) resources folder. Most of the time you only need to change the following properties:
  * biotea.dataset.prefix
  * biotea.dataset
  * biotea.base
* Make sure you include the [biotea-utilities](https://github.com/biotea/biotea-utilities) resources folder in your classpath
* The main class is ws.biotea.ld2rdf.rdfGeneration.batch.PMCOABatchApplication, two parameters are mandatory, one is optional
  * -in <input-dir> --mandatory, should have all *.nxml to be processed (remember it should follow the structure and include all the files provided at [inputToProcess](https://github.com/biotea/biotea-rdfization/tree/master/src/main/resources/inputToProcess)
  * -out <output-dir> --mandatory
  * sections --optional, if present text content will be generated in addition to metadata.

### Example
For instance, if you want to RDFize PMC articles following the [Bio2RDF URL model](https://github.com/bio2rdf/bio2rdf-scripts/wiki/RDFization-Guide) you need this configuration:
* biotea.dataset.prefix=pmc
* biotea.dataset=dataset/pmc
* biotea.base=bio2rdf.org


If you want to include section run the batch process as:
* java ws.biotea.ld2rdf.rdfGeneration.batch.PMCOABatchApplication -in <inputToProcess> -out <output_folder> -sections


If you do not want to include sections then use:
* java ws.biotea.ld2rdf.rdfGeneration.batch.PMCOABatchApplication -in <inputToProcess> -out <output_folder>


For the sample input file DELETE_ME_PMC3879346.nxml provided at [inputToProcess](https://github.com/biotea/biotea-rdfization/tree/master/src/main/resources/inputToProcess), the RDF generated is availabel at [PMC3879346.rdf]
(https://github.com/biotea/biotea-rdfization/blob/master/src/main/resources/output/PMC3879346.rdf) and [PMC3879346_sections.rdf
](https://github.com/biotea/biotea-rdfization/blob/master/src/main/resources/output/PMC3879346_sections.rdf)
