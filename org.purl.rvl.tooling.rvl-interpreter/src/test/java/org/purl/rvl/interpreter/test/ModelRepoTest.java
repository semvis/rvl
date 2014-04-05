package org.purl.rvl.interpreter.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.Reasoning;
import org.ontoware.rdf2go.impl.jena.ModelFactoryImpl;
import org.ontoware.rdf2go.impl.jena.ModelSetImplJena;
import org.ontoware.rdf2go.model.Diff;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.QueryResultTable;
import org.ontoware.rdf2go.model.QueryRow;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.impl.StatementImpl;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.ontoware.rdfreactor.schema.rdfs.Property;
import org.purl.rvl.java.gen.rvl.PropertyMapping;
import org.purl.rvl.tooling.process.ExampleData;
import org.purl.rvl.tooling.process.ExampleMapping;
import org.purl.rvl.tooling.process.OGVICProcess;
import org.purl.rvl.tooling.util.CustomRecordFormatter;

public class ModelRepoTest {
	
	private static final URIImpl GRAPH_RVL = new URIImpl("http://purl.org/rvl/");
	private static final URIImpl GRAPH_MAPPING = new URIImpl("http://purl.org/rvl/example/mapping/");
	private static final URIImpl GRAPH_DATA = new URIImpl("http://purl.org/rvl/example/data/");
	private static final URIImpl GRAPH_MAPPING_ENRICHED_WITH_RVL = new URIImpl("http://purl.org/rvl/example/mapping/enriched/");
	

	private final static Logger LOGGER = Logger.getLogger(ModelRepoTest.class.getName()); 
	private final static Logger LOGGER_RVL_PACKAGE = Logger.getLogger("org.purl.rvl"); 
	
	static final String NL =  System.getProperty("line.separator");
	
	
    static {
    	  	
		LogManager.getLogManager().getLogger(LOGGER_RVL_PACKAGE.getName()).setLevel(Level.FINEST);

		
		// In order to show log entrys of the fine level, we need to create a new handler as well
        ConsoleHandler handler = new ConsoleHandler();
        // PUBLISH this level
        handler.setLevel(Level.FINEST);
        
        CustomRecordFormatter formatter = new CustomRecordFormatter();
        handler.setFormatter(formatter); // out-comment this line to use the normal formatting with method and date
        
        LOGGER_RVL_PACKAGE.setUseParentHandlers(false); // otherwise double output of log entries
        LOGGER_RVL_PACKAGE.addHandler(handler);
		
        }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// we use sesame here, since jena seems not to properly support SPARQL queries on model sets (e.g. named graph queries caused problems)
		//RDF2Go.register( new org.ontoware.rdf2go.impl.jena.ModelFactoryImpl() );
		RDF2Go.register( new org.openrdf.rdf2go.RepositoryModelFactory());
		
		
		/*Properties p = new Properties(); 
		p.setProperty("back-end", "memory");
		ModelSet modelSet = RDF2Go.getModelFactory().createModelSet(p);*/
		
		ModelSet modelSet = RDF2Go.getModelFactory().createModelSet();
		modelSet.open();
		
		// data
		Model dataModel = RDF2Go.getModelFactory().createModel(Reasoning.none);
		dataModel.open();
		readFromAnySyntax(dataModel, ExampleData.SLUB_TEST);
		
		// mapping
		Model mappingModel = RDF2Go.getModelFactory().createModel(Reasoning.rdfs);
		mappingModel.open();
		readFromAnySyntax(mappingModel, ExampleMapping.SLUB );
		LOGGER.finest("mapping model size: " + mappingModel.size());

		/*
		System.out.println("Property mappings in the orig mapping model: ");

		ClosableIterator<Statement> mappingModelIt = mappingModel.iterator();

		while (mappingModelIt.hasNext()) {
			
			Statement pmStmt = (Statement) mappingModelIt.next();
			
			System.out.println(pmStmt);
			
		}*/
		
		// mapping inferred
		Model inferredMappingModel = RDF2Go.getModelFactory().createModel(Reasoning.rdfs);
		inferredMappingModel.open();
		inferredMappingModel.addModel(mappingModel);
		LOGGER.finest("inferred mapping model size: " + inferredMappingModel.size());
		
		// rvl
		Model rvlModel = RDF2Go.getModelFactory().createModel(Reasoning.rdfs);
		rvlModel.open();
		readFromAnySyntax(rvlModel, OGVICProcess.RVL_LOCAL_REL );
		LOGGER.finest("rvl model size: " + rvlModel.size());
		
		Model inferredRVLModel = RDF2Go.getModelFactory().createModel(Reasoning.none);
		inferredRVLModel.open();
		inferredRVLModel.addModel(rvlModel);
		LOGGER.finest("inferred rvl model size: " + inferredRVLModel.size());
		
		// mapping + rvl
		Model mappingsAndRVLModel = RDF2Go.getModelFactory().createModel(Reasoning.rdfs);
		mappingsAndRVLModel.open();
		mappingsAndRVLModel.addModel(inferredRVLModel);
		mappingsAndRVLModel.addModel(mappingModel);
		LOGGER.finest("mappings + rvl model size: " + mappingsAndRVLModel.size());
		
		Model inferredMappingsAndRVLModel = RDF2Go.getModelFactory().createModel(Reasoning.none);
		inferredMappingsAndRVLModel.open();
		inferredMappingsAndRVLModel.addModel(mappingsAndRVLModel);
		LOGGER.finest("inferred mappings + rvl model size: " + inferredMappingsAndRVLModel.size());
		
		Diff diff = rvlModel.getDiff(inferredMappingsAndRVLModel.iterator());
		
		Iterable<Statement> addedIt = diff.getAdded();

		
		// enriched mappings
		Model enrichedMappings = RDF2Go.getModelFactory().createModel(Reasoning.rdfs);
		enrichedMappings.open();
		enrichedMappings.addModel(inferredMappingModel);
		enrichedMappings.addAll(addedIt.iterator());

		int i = 0;
		for (Statement statement : addedIt) {
			i++;
			LOGGER.finest("diff : added statement ("+ i +"): " + statement );
			
		}
		
		Iterable<Statement> removedIt = diff.getRemoved();
		
		for (Statement statement : removedIt) {
			LOGGER.finest("diff : removed statement: " + statement );
		}
		
		LOGGER.finest("enriched mappings model size: " + enrichedMappings.size());
		
		
		
		modelSet.addModel(dataModel, GRAPH_DATA);
		modelSet.addModel(mappingModel, GRAPH_MAPPING);
		modelSet.addModel(rvlModel, GRAPH_RVL);
		modelSet.addModel(enrichedMappings, GRAPH_MAPPING_ENRICHED_WITH_RVL);
		
		
		
		

		
		
		System.out.println("Property mappings in the enriched mapping model: ");
		
		// find stmts filtered
		ClosableIterator<Statement> iteratorPM = mappingsAndRVLModel.findStatements(
				Variable.ANY,
				RDF.type,
				PropertyMapping.RDFS_CLASS
				);

		while (iteratorPM.hasNext()) {
			
			Statement pmStmt = (Statement) iteratorPM.next();
			
			System.out.println(pmStmt);
			
		}
		
		System.out.println("Property mappings in the model sets enriched mapping graph: ");
		
		// find stmts filtered
		ClosableIterator<Statement> iteratorPMInModelSet = modelSet.findStatements(
				GRAPH_MAPPING_ENRICHED_WITH_RVL,
				Variable.ANY,
				RDF.type,
				PropertyMapping.RDFS_CLASS
				);

		while (iteratorPMInModelSet.hasNext()) {
			
			Statement pmStmt = (Statement) iteratorPMInModelSet.next();
			
			System.out.println(pmStmt);
			
		}
		

		//listModelStatements("data model",dataModel);
		//printModelSet(modelSet);
		
		/*
		
		// find stmts filtered
		ClosableIterator<Statement> iterator = modelSet.findStatements(
				GRAPH_DATA,
				Variable.ANY,
				Variable.ANY,
				Variable.ANY
				);

		while (iterator.hasNext()) {
			
			Statement dataStatement = (Statement) iterator.next();
			
			System.out.println(dataStatement);
			
		}
		
		*/
		
		/*
		// sparql-query filtered
		
		LOGGER.finest("Size of model set: " + modelSet.size());
		
		Set<Statement> stmtSetFromSPARQL = 
				findStatementsPreferingThoseUsingASubProperty(
						modelSet,
						RDF.type,
						GRAPH_MAPPING,
						//new URIImpl("http://www.openarchives.org/OAI/2.0/header"),
						null);
		
		for (Iterator<Statement> iterator2 = stmtSetFromSPARQL.iterator(); iterator2
				.hasNext();) {
			Statement statement = (Statement) iterator2.next();
			
			System.out.println(statement.getContext() + ": " + statement);
			
		} */
		
	}
	
	private static void listModelStatements(String context, Model model){
		
		System.out.println("Listing statements in model with context " + context);
		
		ClosableIterator<Statement> iterator = model.iterator();
		
		while (iterator.hasNext()) {
			Statement statement = (Statement) iterator.next();
			
			System.out.println(statement);
			
		}
		
	}
	
	private static void printModelSet(ModelSet modelSet){
		
		System.out.println("Listing statements in ModelSet");
		
		ClosableIterator<Statement> iterator = modelSet.iterator();
		
		while (iterator.hasNext()) {
			Statement statement = (Statement) iterator.next();
			
			
			System.out.println(statement.getContext() + ": " + statement);
			
		}
		
	}
	
	
	private static void readFromAnySyntax(Model model, String fileName) {
		
		File file = new File(fileName);
		readFromAnySyntax(model, file);

	}
	
	
	
	private static void readFromAnySyntax(Model model, File file) {

		try {
			
			String extension = FilenameUtils.getExtension(file.getName());
			
			if (extension.equals("ttl") || extension.equals("n3")) {
				model.readFrom(new FileReader(file),
						Syntax.Turtle);
			} else {
				model.readFrom(new FileReader(file),
						Syntax.RdfXml);
			}
		
			LOGGER.info("Reading file into (some) model: " + file.getPath());
			
		} catch (FileNotFoundException e) {
			LOGGER.info("File could not be read into the model, since it wasn't found: " +  file.getPath());
		} catch (IOException e) {
			LOGGER.info("File could not be read into the model: " +  file.getPath());
			e.printStackTrace();
		}

	}

	private static Set<Statement> findStatementsPreferingThoseUsingASubProperty(
			ModelSet modelSet,
			URI spURI,
			URI fromGraph, 
			org.ontoware.rdf2go.model.node.Resource selectorClass
			) {
		
		Set<Statement> stmtSet = new HashSet<Statement>();
		
		LOGGER.finest("Size of model set: " + modelSet.size());
		
		try {
	
			
			String query = "" + 
					" SELECT " +
					" DISTINCT ?src ?s ?p ?o " + 
					" FROM NAMED " + fromGraph.toSPARQL() + // note: without GRAPH phrase below, only FROM works, not FROM NAMED
					" WHERE { " + 
					" GRAPH ?src { " +
					"";
			if (selectorClass != null) {
				query += 
					" ?s " + RDF.type.toSPARQL()  + " " + selectorClass.toSPARQL() + " . ";
			}
			query += 
					" ?s ?p ?o . " + 
					" ?p " + Property.SUBPROPERTYOF.toSPARQL() + "* " + spURI.toSPARQL() + " " +
					" FILTER NOT EXISTS { " + 
							" ?s ?pp ?o . " + 
					        " ?pp " + Property.SUBPROPERTYOF.toSPARQL() + "+ ?p " +		 
					" FILTER(?pp != ?p) " +
					" } " +
					" FILTER(?s != ?o) " + // TODO: this stops reflexive arcs completely! make optional
					" FILTER isIRI(?s) " + // TODO: this stops blank nodes as subjects ...
					" FILTER isIRI(?o) " + // .. or objects! make optional!
					" } " + 
					" } " + 
					" LIMIT " + OGVICProcess.MAX_GRAPHIC_RELATIONS_PER_MAPPING + " ";
			
			
			/*
			String query = "" + 
					" SELECT DISTINCT ?src ?s ?p ?o " + 
					" FROM NAMED " + fromGraph.toSPARQL() + // note: without GRAPH phrase below, only FROM works, not FROM NAMED
					" WHERE { " + 
					" GRAPH ?src { ";
			query += 
					" ?s ?p ?o . " +
					" } "  + 
					" } " ;
					*/
					
								
			
			LOGGER.fine("Query statements in graph " + fromGraph + " with property (respectively most specific subproperty of) :" + spURI);
			LOGGER.finest("Query: " + query);
			
			
			QueryResultTable explMapResults = modelSet.sparqlSelect(query);
			
			for (QueryRow row : explMapResults) {
				LOGGER.finest("fetched SPARQL result row: " + row);
				try {
					//System.out.println(row.getValue("src").asURI());
					Statement stmt = new StatementImpl(row.getValue("src").asURI(), row.getValue("s").asURI(), row.getValue("p").asURI(), row.getValue("o"));
					LOGGER.finer("added Statement: " + stmt.toString());
					stmtSet.add(stmt);
				} catch (ClassCastException e){
					LOGGER.finer("Skipped statement for linking (blank node casting to URI?): " + e.getMessage());
				}
			}
		
		} catch (UnsupportedOperationException e){
			LOGGER.warning("Problem with query to get statements for linking (blank node?): " + e.getMessage());
		} 
		
		return stmtSet;
	}

}
