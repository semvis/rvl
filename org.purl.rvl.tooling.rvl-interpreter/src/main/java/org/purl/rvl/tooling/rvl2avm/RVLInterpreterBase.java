package org.purl.rvl.tooling.rvl2avm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.QueryResultTable;
import org.ontoware.rdf2go.model.QueryRow;
import org.ontoware.rdf2go.model.node.URI;
import org.purl.rvl.java.gen.rvl.Property_to_Graphic_AttributeMapping;
import org.purl.rvl.java.gen.rvl.Property_to_Graphic_Object_to_Object_RelationMapping;
import org.purl.rvl.java.rvl.PropertyToGO2ORMappingX;
import org.purl.rvl.java.rvl.PropertyToGraphicAttributeMappingX;
import org.purl.rvl.java.viso.graphic.GraphicObjectX;
import org.purl.rvl.tooling.process.OGVICProcess;
import org.purl.rvl.tooling.util.AVMUtils;

public abstract class RVLInterpreterBase {
	
	//protected Model model;
	protected Model modelAVM;
	protected ModelSet modelSet;
	protected Model modelData;
	protected Model modelMappings;
	protected Model modelVISO;
	protected Map<org.ontoware.rdf2go.model.node.Resource,GraphicObjectX> resourceGraphicObjectMap; 
	protected Random random;
	
	protected OGVICProcess ogvicProcess = OGVICProcess.getInstance();

	
	private final static Logger LOGGER = Logger.getLogger(RVLInterpreterBase.class .getName()); 
	static final String NL =  System.getProperty("line.separator");

	public RVLInterpreterBase() {
		super();
	}
	

	public void init(
			//Model model,
			Model modelAVM, ModelSet modelSet) {
		//this.model = model;
		this.modelAVM = modelAVM;
		this.modelSet = modelSet;
		this.modelData = modelSet.getModel(OGVICProcess.GRAPH_DATA);
		this.modelMappings = modelSet.getModel(OGVICProcess.GRAPH_MAPPING);
		this.modelVISO = modelSet.getModel(OGVICProcess.GRAPH_VISO);
		this.random = new Random();
		this.resourceGraphicObjectMap = new HashMap<org.ontoware.rdf2go.model.node.Resource, GraphicObjectX>();
	}


	/**
	 * Interpret all supported RVL mappings 
	 */
	public void interpretMappings() {
		LOGGER.info("Starting mapping interpretation ... ");
		LOGGER.info("Interpreting mappings using " + this.getClass().getName());
		interpretMappingsInternal();
	}

	abstract protected void interpretMappingsInternal();


	/**
	 * Creates a GraphicObjectX for a Resource or returns the existing GraphicObjectX, if already created before
	 * @param resource
	 * @return the GraphicObjectX representing the resource
	 */
	protected GraphicObjectX createOrGetGraphicObject(org.ontoware.rdf2go.model.node.Resource resource) {
		
		if (resourceGraphicObjectMap.containsKey(resource)) {
			
			LOGGER.finest("Found existing GO for " + resource);
			return resourceGraphicObjectMap.get(resource);
		} 
		else {
			
			GraphicObjectX go = new GraphicObjectX(modelAVM,"http://purl.org/rvl/example-avm/GO_" + random.nextInt(), true);
			
			// add to cache
			go = go.tryReplaceWithCashedInstanceForSameURI(go);
			
			go.setRepresents(resource);
			
			resourceGraphicObjectMap.put(resource, go);
			
			LOGGER.finer("Newly created GO for " + resource);
			
			return go;
		}
	}

	/**
	 * Get all the mappings that require no calculation, because they only have explicit 1-1-value-mappings
	 */
	protected Set<PropertyToGraphicAttributeMappingX> getAllP2GAMappingsWithExplicitMappings(){
		
		Set<PropertyToGraphicAttributeMappingX> mappingSet = new HashSet<PropertyToGraphicAttributeMappingX>();

		String queryString = "" +
				"SELECT DISTINCT ?p2gam " +
				"WHERE { " +
				"    ?p2gam a <" + PropertyToGraphicAttributeMappingX.RDFS_CLASS + "> . " +
				"    ?p2gam <" + PropertyToGraphicAttributeMappingX.VALUEMAPPING + "> ?vm . " +
				"	{ " +
				"	SELECT ?vm  (COUNT(?sv) AS ?svCount) " +
				"       WHERE " +
				"       { " +
				"	 		  ?vm <" + PropertyToGraphicAttributeMappingX.SOURCEVALUE + "> ?sv  " +
				"       } " +
				"        GROUP BY ?vm " +
				"	} " +
				"    FILTER (?svCount = 1 ) " +
				"} " ;
		
		//LOGGER.info("All mappings with explicit value mappings (VMs with only 1 source value)");
		//LOGGER.info(queryString);
		
		QueryResultTable results = modelMappings.sparqlSelect(queryString);
//		for(QueryRow row : results) {LOGGER.info(row); }
//		for(String var : results.getVariables()) { LOGGER.info(var); }
		
		for(QueryRow row : results) {
			Property_to_Graphic_AttributeMapping p2gam = Property_to_Graphic_AttributeMapping.getInstance(modelMappings, row.getValue("p2gam").asResource());
			mappingSet.add((PropertyToGraphicAttributeMappingX)p2gam.castTo(PropertyToGraphicAttributeMappingX.class));
			//LOGGER.info(row.getValue("p2gam"));
		}
		
		return mappingSet;
	}
	
	
	/**
	 * Get all the mappings that require calculation, because they have not only explicit 1-1-value-mappings
	 * TODO: this curently gets all mappings, including the 1-1, therefore it should actually only be called when it is clear that
	 *  the 1-1 case does not apply. 
	 */
	protected Set<PropertyToGraphicAttributeMappingX> getAllP2GAMappingsWithSomeValueMappings(){
		
		Set<PropertyToGraphicAttributeMappingX> mappingSet = new HashSet<PropertyToGraphicAttributeMappingX>();

		String queryString = "" +
				"SELECT DISTINCT ?p2gam " +
				"WHERE { " +
				"    ?p2gam a <" + PropertyToGraphicAttributeMappingX.RDFS_CLASS + "> . " +
				"    ?p2gam <" + PropertyToGraphicAttributeMappingX.VALUEMAPPING + "> ?vm . " +
//				"	{ " +
//				"	SELECT ?vm  (COUNT(?sv) AS ?svCount) " +
//				"       WHERE " +
//				"       { " +
//				"	 		  ?vm <" + PropertyToGraphicAttributeMappingX.SOURCEVALUE + "> ?sv  " +
//				"       } " +
//				"        GROUP BY ?vm " +
//				"	} " +
//				"    FILTER (!(?svCount = 1 )) " +
				"} " ;
		
		
		QueryResultTable results = modelMappings.sparqlSelect(queryString);
		for(QueryRow row : results) {
			try {
			//Property_to_Graphic_AttributeMapping p2gam = Property_to_Graphic_AttributeMapping.getInstance(model, (URI)row.getValue("p2gam"));
			Property_to_Graphic_AttributeMapping p2gam = Property_to_Graphic_AttributeMapping.getInstance(modelMappings, row.getValue("p2gam").asResource());
			mappingSet.add((PropertyToGraphicAttributeMappingX)p2gam.castTo(PropertyToGraphicAttributeMappingX.class));
			}
			catch (Exception e) {
				LOGGER.warning("P2GAM could not be added to the mapping set.");
				continue;
			}
		}
		
		return mappingSet;
	}
	
	/*
	protected Set<PropertyToGO2ORMappingX> getAllMappingsToLinking() {
		
		Set<PropertyToGO2ORMappingX> mappingSet = new HashSet<PropertyToGO2ORMappingX>();

		String queryString = "" +
				"SELECT DISTINCT ?mapping " +
				"WHERE { " +
				"    ?mapping a <" + PropertyToGO2ORMappingX.RDFS_CLASS + "> . " +
				"    ?mapping <" + PropertyToGO2ORMappingX.TARGETOBJECT_TO_OBJECTRELATION + "> <" + DirectedLinking.RDFS_CLASS + "> . " +
				"} " ;
		
		LOGGER.finer("SPARQL: query all mappings to Directed Linking:" + NL + 
				     queryString);
		
		QueryResultTable results = model.sparqlSelect(queryString);
		//for(QueryRow row : results) {LOGGER.info(row); }
		//for(String var : results.getVariables()) { LOGGER.info(var); }
		
		for(QueryRow row : results) {
			Property_to_Graphic_Object_to_Object_RelationMapping mapping = Property_to_Graphic_Object_to_Object_RelationMapping.getInstance(model, (URI)row.getValue("mapping"));
			mappingSet.add((PropertyToGO2ORMappingX)mapping.castTo(PropertyToGO2ORMappingX.class));
			//LOGGER.info("Found mapping to linking: " + row.getValue("mapping").toString());
		}
		
		return mappingSet;
	}
	
	*/
	
	protected Set<PropertyToGO2ORMappingX> getAllP2GOTORMappingsTo(URI gotor) {
		
		Set<PropertyToGO2ORMappingX> mappingSet = new HashSet<PropertyToGO2ORMappingX>();
		
		// constraining target GOTOR is optional
		String gotorString;
		if (null == gotor) {
			gotorString = " ?tgotor ";
		} else {
			gotorString = gotor.toSPARQL();
		}

		String queryString = "" +
				"SELECT DISTINCT ?mapping " +
				"WHERE { " +
				"    ?mapping a <" + PropertyToGO2ORMappingX.RDFS_CLASS + "> . " +
				"    ?mapping " + PropertyToGO2ORMappingX.TARGETOBJECT_TO_OBJECTRELATION.toSPARQL() + " " + gotorString + " . " +
				"} " ;
		
		LOGGER.finer("SPARQL: query all mappings to " + gotorString + ":" + NL + 
				     queryString);
		
		QueryResultTable results = modelMappings.sparqlSelect(queryString);
		//for(QueryRow row : results) {LOGGER.info(row); }
		//for(String var : results.getVariables()) { LOGGER.info(var); }
		
		for(QueryRow row : results) {
			Property_to_Graphic_Object_to_Object_RelationMapping mapping = Property_to_Graphic_Object_to_Object_RelationMapping.getInstance(modelMappings, (URI)row.getValue("mapping"));
			mappingSet.add((PropertyToGO2ORMappingX)mapping.castTo(PropertyToGO2ORMappingX.class));
			//LOGGER.info("Found mapping to linking: " + row.getValue("mapping").toString());
		}
		
		return mappingSet;
	}
	
	
	protected Set<PropertyToGO2ORMappingX> getAllP2GOTORMappings() {
		
		return getAllP2GOTORMappingsTo(null);
	}
	
	/**
	 * Iterates through all GOs in the GO map and performs a default label mapping on them
	 */
	protected void interpretResourceLabelAsGOLabelForAllCreatedResources(){
		for (Map.Entry<org.ontoware.rdf2go.model.node.Resource,GraphicObjectX> entry : resourceGraphicObjectMap.entrySet()) {
			//LOGGER.info(entry.getKey() + " with value " + entry.getValue());
			// perform the default label mapping, when not already set
		    // TODO this is simply using rdfs:label of the GOs now, not the n-ary graphic labeling!
		    // only rdfreactor resources have labels ...
			GraphicObjectX go = entry.getValue();
			org.ontoware.rdf2go.model.node.Resource resource = entry.getKey();
			if(!go.hasLabels()) {
				performDefaultLabelMapping(go,resource);
			}
		}
	}
	
	/**
	 * Sets the label of a GO to the resources (first) label
	 * @param go
	 * @param resource
	 */
	private void performDefaultLabelMapping(GraphicObjectX go,
			org.ontoware.rdf2go.model.node.Resource resource) {
		
		//LOGGER.finest("Problems getting represented resource, no label generated for GO " + this.asURI());

		try {
			go.setLabel(AVMUtils.getOrGenerateDefaultLabelString(modelData, resource));
		} catch (Exception e) {
			LOGGER.finest("No label could be assigned for resource " + resource + " to GO " + go.asURI().toString() + e.getMessage());
			e.printStackTrace();
		}
	}

}
