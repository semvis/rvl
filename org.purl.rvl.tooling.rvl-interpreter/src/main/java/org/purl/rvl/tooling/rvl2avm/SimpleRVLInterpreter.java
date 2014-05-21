package org.purl.rvl.tooling.rvl2avm;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdfreactor.schema.owl.Restriction;
import org.ontoware.rdfreactor.schema.rdfs.Property;
import org.purl.rvl.exception.InsufficientMappingSpecificationException;
import org.purl.rvl.java.RDF;
import org.purl.rvl.java.RVL;
import org.purl.rvl.java.gen.rvl.GraphicAttribute;
import org.purl.rvl.java.gen.viso.graphic.Color;
import org.purl.rvl.java.gen.viso.graphic.Containment;
import org.purl.rvl.java.gen.viso.graphic.DirectedLinking;
import org.purl.rvl.java.gen.viso.graphic.GraphicObjectToObjectRelation;
import org.purl.rvl.java.gen.viso.graphic.Labeling;
import org.purl.rvl.java.gen.viso.graphic.Shape;
import org.purl.rvl.java.gen.viso.graphic.UndirectedLinking;
import org.purl.rvl.java.rvl.MappingX;
import org.purl.rvl.java.rvl.PropertyMappingX;
import org.purl.rvl.java.rvl.PropertyToGO2ORMappingX;
import org.purl.rvl.java.rvl.PropertyToGraphicAttributeMappingX;
import org.purl.rvl.java.rvl.SubMappingRelationX;
import org.purl.rvl.java.viso.graphic.GraphicObjectX;
import org.purl.rvl.java.viso.graphic.ShapeX;
import org.purl.rvl.tooling.process.OGVICProcess;
import org.purl.rvl.tooling.util.AVMUtils;
import org.purl.rvl.tooling.util.RVLUtils;

/**
 * @author Jan Polowinski
 *
 */
public class SimpleRVLInterpreter  extends RVLInterpreterBase {
	

	
	private final static Logger LOGGER = Logger.getLogger(SimpleRVLInterpreter.class .getName()); 

	public SimpleRVLInterpreter() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.purl.rvl.tooling.rvl2avm.RVLInterpreterBase#interpretMappings()
	 */
	@Override
	protected void interpretMappingsInternal() {
		
		if (null==modelSet) {
			LOGGER.severe("Cannot interprete mappings, since model set is null.");
			return;
		}
		
		interpretSimpleP2GArvlMappings();
		interpretNormalP2GArvlMappings(); 
		interpretP2GO2ORMappings();
		interpretResourceLabelAsGOLabelForAllCreatedResources();
	}

	/**
	 * Interprets the P2GO2OR mappings.
	 * TODO: Implement other GR than linking
	 */
	protected void interpretP2GO2ORMappings() {
		
		// get all P2GO2OR mappings to linking and create n-ary linking relations
		//Set<PropertyToGO2ORMappingX> setOfMappingsToLinking = getAllP2GOTORMappingsTo(DirectedLinking.RDFS_CLASS); // 
		
		Set<PropertyToGO2ORMappingX> mappings = getAllP2GOTORMappings();
		
		LOGGER.info(NL + "Found " + mappings.size() + " PGOTOR mappings (enabled and disabled mappings).");
		
		// for each mapping
		for (Iterator<PropertyToGO2ORMappingX> iterator = mappings
				.iterator(); iterator.hasNext();) {
			
			PropertyToGO2ORMappingX p2go2orm = (PropertyToGO2ORMappingX) iterator.next();
			
			// skip disabled
			if (p2go2orm.isDisabled()) {
				LOGGER.info("Ignored disabled P2GO2OR mapping " + p2go2orm.asURI());
				continue;
			}
			
			LOGGER.info("Interpret P2GOTOR mapping " + p2go2orm.asURI() );
			try {
				LOGGER.info(p2go2orm.toStringDetailed() );
			} catch (Exception e) {}
			
			
			try {
				
				if (p2go2orm.getTargetGraphicRelation().equals(DirectedLinking.RDFS_CLASS) || p2go2orm.getTargetGraphicRelation().equals(UndirectedLinking.RDFS_CLASS)) {
					interpretMappingToLinking(p2go2orm);
				}
//				else if (p2go2orm.getTargetGraphicRelation().equals(UndirectedLinking.RDFS_CLASS)) {
//					LOGGER.info("Ignored Mapping to Undirected Linking. Undirected Linking not yet implemented");
//				}
				else if (p2go2orm.getTargetGraphicRelation().equals(Containment.RDFS_CLASS)) {
					interpretMappingToContainment(p2go2orm);
					//LOGGER.info("Ignored Mapping to Containment. Containment not yet implemented");
				}
				else  {
					LOGGER.info("Ignored mapping to " + p2go2orm.getTargetGraphicRelation() + ". Graphic relation not yet implemented");
				}
				
			} catch (InsufficientMappingSpecificationException e) {
				LOGGER.severe("Could not interpret P2GOTOR mapping " +  p2go2orm.asURI() + ". " + e.getMessage());
			}

		}
		
		LOGGER.fine("The size of the Resource-to-GraphicObjectX map is " + resourceGraphicObjectMap.size()+".");
	}

	
	@SuppressWarnings("unused")
	protected void interpretMappingToLinking(PropertyToGO2ORMappingX p2go2orm) throws InsufficientMappingSpecificationException {

		Iterator<Statement> stmtSetIterator = RVLUtils.findRelationsOnInstanceOrClassLevel(
				modelSet,
				OGVICProcess.GRAPH_DATA,
				(PropertyMappingX) p2go2orm.castTo(PropertyMappingX.class),
				true,
				null,
				null
				).iterator();
		
		int processedGraphicRelations = 0;	
		
		if(null==stmtSetIterator) {
			LOGGER.severe("Statement iterator was null, no linking relations could be interpreted for " + p2go2orm.asURI());
			return;
		}
		
		while (stmtSetIterator.hasNext() && processedGraphicRelations < OGVICProcess.MAX_GRAPHIC_RELATIONS_PER_MAPPING) {
			
			Statement statement = (Statement) stmtSetIterator.next();
						
			try {
				Resource subject = statement.getSubject();
				Resource object = statement.getObject().asResource();
				
				LOGGER.finest("Subject label " + AVMUtils.getGoodLabel(subject,modelAVM));
				LOGGER.finest("Object label " + AVMUtils.getGoodLabel(object,modelAVM));
	
				LOGGER.fine("Statement to be mapped : " + statement);

				// For each statement, create a startNode GO representing the subject (if not exists)
			    GraphicObjectX subjectNode = createOrGetGraphicObject(subject);
		    	LOGGER.finest("Created GO for subject: " + subject.toString());
				
				// For each statement, create an endNode GO representing the object (if not exists)
		    	//Node object = statement.getObject();
				
				GraphicObjectX objectNode = createOrGetGraphicObject(object);
		    	LOGGER.finest("Created GO for object: " + object.toString());
		    	
				// create a connector and add default color
				//GraphicObjectX connector = new GraphicObjectX(modelAVM, true);
				GraphicObjectX connector = new GraphicObjectX(modelAVM,"http://purl.org/rvl/example-avm/GO_Connector_" + random.nextInt(), true);
				//connector.setLabel(statement.getPredicate()); 
		    	connector.setLabel(AVMUtils.getGoodLabel(statement.getPredicate(), modelAVM) + "     (actually the label of the connector representing this) "); // statement contains evtl. used subproperty
				
				// generic graphic relation needed for submappings 
				// (could also be some super class of directed linking, undirected linking, containment ,...)
				Resource rel = null;
				
				// directed linking
				if (p2go2orm.getTargetGraphicRelation().equals(DirectedLinking.RDFS_CLASS)) {
					
			    	// create the directed linking relation
			    	//DirectedLinking dlRel = new DirectedLinking(modelAVM, true);
			    	DirectedLinking dlRel = new DirectedLinking(modelAVM,"http://purl.org/rvl/example-avm/GR_" + random.nextInt(), true);
			    	dlRel.setLabel(AVMUtils.getGoodLabel(p2go2orm.getTargetGraphicRelation(), modelAVM));
			    	
					// configure the relation
					if(p2go2orm.isInvertSourceProperty()) {
						dlRel.setEndnode(subjectNode);
						dlRel.setStartnode(objectNode);
						subjectNode.addLinkedfrom(dlRel);
						objectNode.addLinkedto(dlRel);
					} else {
						dlRel.setStartnode(subjectNode);
						dlRel.setEndnode(objectNode);
						subjectNode.addLinkedto(dlRel);
						objectNode.addLinkedfrom(dlRel);
					}
					
					dlRel.setLinkingconnector(connector);
					rel=dlRel;
					

				} else { // undirected linking
					
					// create the undirected linking relation
			    	//UndirectedLinking udlRel = new UndirectedLinking(modelAVM, true);
					UndirectedLinking udlRel = new UndirectedLinking(modelAVM,"http://purl.org/rvl/example-avm/GR_" + random.nextInt(), true);
			    	udlRel.setLabel(AVMUtils.getGoodLabel(p2go2orm.getTargetGraphicRelation(), modelAVM));
			    	
					// configure the relation
					udlRel.addLinkingnode(subjectNode);
					udlRel.addLinkingnode(objectNode);
					subjectNode.addLinkedwith(udlRel);
					objectNode.addLinkedwith(udlRel);
					
					udlRel.setLinkingconnector(connector);
					rel=udlRel;
				}
				
				// submappings
				if(p2go2orm.hasSub_mapping()){
					
					if(null != rel) {
						applySubmappings(p2go2orm,statement,rel); // DirectedLinking etc need to be subclasses of (n-ary) GraphicRelation
					} else {
						LOGGER.warning("Submapping existed, but could not be applied, since no parent graphic relation was provided.");
					}
				}
				
			}
			catch (Exception e) {
				LOGGER.warning("Problem creating GOs: " + e.getMessage());
			}
			
			processedGraphicRelations++;	
		}
		
	}
	
	// cloned from linking, much duplicated code
	@SuppressWarnings("unused")
	protected void interpretMappingToContainment(PropertyToGO2ORMappingX p2go2orm) throws InsufficientMappingSpecificationException {

		Iterator<Statement> stmtSetIterator = RVLUtils.findRelationsOnInstanceOrClassLevel(
				modelSet,
				OGVICProcess.GRAPH_DATA,
				(PropertyMappingX) p2go2orm.castTo(PropertyMappingX.class),
				true,
				null,
				null
				).iterator();
		
		int processedGraphicRelations = 0;	
		
		if(null==stmtSetIterator) {
			LOGGER.severe("Statement iterator was null, no containment relations could be interpreted for " + p2go2orm.asURI());
			return;
		}
		
		while (stmtSetIterator.hasNext() && processedGraphicRelations < OGVICProcess.MAX_GRAPHIC_RELATIONS_PER_MAPPING) {
			
			Statement statement = (Statement) stmtSetIterator.next();
						
			try {
				Resource subject = statement.getSubject();
				Resource object = statement.getObject().asResource();
				//Node object = statement.getObject();
				
				LOGGER.finest("Subject label " + AVMUtils.getGoodLabel(subject,modelAVM));
				LOGGER.finest("Object label " + AVMUtils.getGoodLabel(object,modelAVM));
				LOGGER.fine("Statement to be mapped : " + statement);

				// For each statement, create a container GO representing the subject (if not exists)
			    GraphicObjectX subjectContainer = createOrGetGraphicObject(subject);

				// For each statement, create a containee GO representing the object (if not exists)
				GraphicObjectX objectContainee = createOrGetGraphicObject(object);
				
		    	LOGGER.finest("Created GO for subject: " + subject.toString());
		    	LOGGER.finest("Created GO for object: " + object.toString());

				// generic graphic relation needed for submappings 
				// (could also be some super class of containment ,...)
				Resource rel = null;
									
		    	// create the containment relation
		    	//Containment dlRel = new Containment(modelAVM, true);
		    	Containment containmentRel = new Containment(modelAVM,"http://purl.org/rvl/example-avm/GR_" + random.nextInt(), true);
		    	containmentRel.setLabel(AVMUtils.getGoodLabel(p2go2orm.getTargetGraphicRelation(), modelAVM));
		    	
				// configure the relation
				if(!p2go2orm.isInvertSourceProperty()) {
					containmentRel.setContainmentcontainer(subjectContainer);
					containmentRel.setContainmentcontainee(objectContainee);
					subjectContainer.addContains(containmentRel);
					objectContainee.addContainedby(containmentRel);
				} else {
					containmentRel.setContainmentcontainee(subjectContainer);
					containmentRel.setContainmentcontainer(objectContainee);
					subjectContainer.addContainedby(containmentRel);
					objectContainee.addContains(containmentRel);
				}
				
				rel=containmentRel;
					
				// submappings
				if(p2go2orm.hasSub_mapping()){
					
					if(null != rel) {
						applySubmappings(p2go2orm,statement,rel); // Containment etc need to be subclasses of (n-ary) GraphicRelation
					} else {
						LOGGER.warning("Submapping existed, but could not be applied, since no parent graphic relation was provided.");
					}
				}
				
			}
			catch (Exception e) {
				LOGGER.warning("Problem creating GOs: " + e.getMessage());
			}
			
			processedGraphicRelations++;	
		}
		
	}



	/**
	 * Sets the color of the connector according to evtl. existent submappings
	 * 
	 * @param p2go2orm
	 * @param mainStatement
	 * @param dlRel
	 */
	protected void applySubmappings(PropertyToGO2ORMappingX p2go2orm, Statement mainStatement, Resource dlRel) {
		
		// TODO derive GO by onRole settings and the mainStatement? or just check if correct?

		Set<SubMappingRelationX> subMappingRelations = p2go2orm.getSubMappings();
		
		for (Iterator<SubMappingRelationX> iterator = subMappingRelations.iterator(); iterator
				.hasNext();) {

			SubMappingRelationX smr = (SubMappingRelationX) iterator
					.next();
			
			LOGGER.finer("Applying submapping to GO with the role " + smr.getOnRole());
			
			URI roleURI = smr.getOnRole().asURI();
			URI triplePartURI = smr.getOnTriplePart().asURI();
			
			// modelAVM.findStatements(dlRel,role,Variable.ANY); does not work somehow -> Jena mapping problems

			GraphicObjectX goToApplySubmapping = RVLUtils.getGOForRole(modelAVM, dlRel, roleURI); 
			// TODO this is a simplification: multiple GOs may be affected, not only one
				
			MappingX subMapping = smr.getSubMapping();
			
			if (subMapping.isDisabled()) {
				//LOGGER.info("The referenced submapping was disabled. Will ignore it");
				LOGGER.info("The referenced submapping was disabled but will still be used. TODO: implement 3 status ENABLED (default), DISABLED, USE-ONLY-AS-SUBMAPPING");
				//continue;
			}

			// TODO can also be another P2GO2OR-mapping
			PropertyToGraphicAttributeMappingX p2gam = 
					(PropertyToGraphicAttributeMappingX) subMapping.castTo(PropertyToGraphicAttributeMappingX.class);
			
			// check if already cached in the extra java object cache for resource (rdf2go itself is stateless!)
			p2gam = p2gam.tryReplaceWithCashedInstanceForSameURI(p2gam);
			
			//System.out.println(p2gam);
			
			try {
				applyMappingToGraphicObject(mainStatement, triplePartURI, goToApplySubmapping, p2gam);
				// this does not use the cashed mappings somehow:
				//goToApplySubmapping.setLabel(roleURI + " with an applied submapping: " + smr.toStringSummary());
				
			} catch (InsufficientMappingSpecificationException e) {
				
				LOGGER.warning("Submapping could not be applied. Reason: " + e.getMessage());
			}
			
		}
		
	}

	private void applyMappingToGraphicObject(
			Statement mainStatement, URI triplePartURI, GraphicObjectX goToApplySubmapping,
			PropertyToGraphicAttributeMappingX p2gam) throws InsufficientMappingSpecificationException {
		
		GraphicAttribute tga = p2gam.getTargetAttribute();
		Property sp = p2gam.getSourceProperty();

		// get the subproperties as subjects of the new mapping --> do this in the calculation of value mappings instead

		if (null != tga && p2gam.hasValuemapping()) {
		
			Map<Node, Node> svUriTVuriMap = p2gam.getExplicitlyMappedValues();	
			
			if (null == svUriTVuriMap || svUriTVuriMap.isEmpty()) {
				LOGGER.severe("Could not apply submappings since (explicit or calculated) value mappings were null");
				return;
			} else {
				LOGGER.finer(p2gam.explicitlyMappedValuesToString());
			}
			
			///Node triplePartValue = ...
			//Node property = (Node) model.getProperty(new URIImpl("http://purl.org/rvl/example-data/cites"));
	
			Node sv = null, tv = null;
			
			if (triplePartURI.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject")){
				if (sp.toString().equals(RVL.ID) || sp.toString().equals(RDF.ID) ) {
					sv = mainStatement.getSubject();
					tv = svUriTVuriMap.get(sv);
				} else {
					// maybe not the most specific is mapped ...
					//sv = RDFTool.getSingleValue(model, mainStatement.getSubject().asResource(), sp.asURI());
					
					ClosableIterator<Statement> it = modelSet.findStatements(OGVICProcess.GRAPH_DATA, mainStatement.getSubject().asResource(), sp.asURI(), Variable.ANY);
					while (it.hasNext()) {
						sv = it.next().getObject();
						if (svUriTVuriMap.containsKey(sv)) { 
							tv = svUriTVuriMap.get(sv);
							break;
						}
					}
					
					
				}
					
			} else if (triplePartURI.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#object")){
				if (sp.toString().equals(RVL.ID) || sp.toString().equals(RDF.ID) ) {
					sv = mainStatement.getObject(); // TODO ID actually only fine when URIs!
					tv = svUriTVuriMap.get(sv);
				} else {
					try {
						//sv = RDFTool.getSingleValue(model, mainStatement.getObject().asResource(), sp.asURI());
						
						ClosableIterator<Statement> it = modelSet.findStatements(OGVICProcess.GRAPH_DATA, mainStatement.getObject().asResource(), sp.asURI(), Variable.ANY);
						while (it.hasNext()) {
							sv = it.next().getObject();
							if (svUriTVuriMap.containsKey(sv)) { 
								tv = svUriTVuriMap.get(sv);
								break;
							}
						}
						
					} catch (Exception e) {
						LOGGER.severe("Could not get value for source property " + sp + "for object " + mainStatement.getObject() );
						return;
					}
				}
			} else {
				if (sp.toString().equals(RVL.ID) || sp.toString().equals(RDF.ID) ) {
					sv = mainStatement.getPredicate();
					tv = svUriTVuriMap.get(sv);
				} else {
					//sv = RDFTool.getSingleValue(model, mainStatement.getPredicate().asResource(), sp.asURI());
					
					ClosableIterator<Statement> it = modelSet.findStatements(OGVICProcess.GRAPH_DATA, mainStatement.getPredicate().asResource(), sp.asURI(), Variable.ANY);
					while (it.hasNext()) {
						sv = it.next().getObject();
						if (svUriTVuriMap.containsKey(sv)) { 
							tv = svUriTVuriMap.get(sv);
							break;
						}
					}
				}
				
			}

			
			// if we found a tv for the sv
			if (null != tv && null != sv) {
				applyGraphicValueToGO(tga, tv, sv, goToApplySubmapping);
			} else {
				LOGGER.finest("Source or target value was null, couldn't apply graphic value " + tv + " to the sv " + sv);
			}
				
			
		} else {
			LOGGER.warning("P2GAM with no value mappings at all are not yet supported (defaults needs to be implemented).");
		}
	}
	
	

	
	/**
	 * Interprets the normal P2GA mappings, i.e. those with need for calculating value mappings. 
	 * Creates GO for all affected resources if they don't exist already.
	 */
	protected void interpretNormalP2GArvlMappings() {
		
		Set<PropertyToGraphicAttributeMappingX> setOfP2GAMappings = getAllP2GAMappingsWithSomeValueMappings();
		
		LOGGER.info(NL + "Found " +setOfP2GAMappings.size()+ " normal P2GA mappings.");
		
		// for each normal P2GA mapping
		for (Iterator<PropertyToGraphicAttributeMappingX> iterator = setOfP2GAMappings
				.iterator(); iterator.hasNext();) {
			
			PropertyToGraphicAttributeMappingX p2gam = (PropertyToGraphicAttributeMappingX) iterator.next();
			
			// caching
			p2gam = p2gam.tryReplaceWithCashedInstanceForSameURI(p2gam);
			
			if (p2gam.isDisabled()) {
				LOGGER.info("Ignored disabled normal P2GAM mapping " + p2gam.toStringSummary() );
				continue;
			}
			
			interpretNormalP2GArvlMapping(p2gam);
		}

		LOGGER.fine("The size of the Resource-to-GraphicObjectX map is " + resourceGraphicObjectMap.size()+".");
		
	}

	
	/**
	 * Interprets a normal P2GA mapping with an implicit value mapping and triggers calculating its values. 
	 * ONLY ONE VALUE MAPPING is currently handled! Therefore, explicit value mappings, which are usually more than one are handled by 
	 * interpretSimpleP2GAMappings.
	 * Creates GO for all affected resources if they don't exist already.
	 * @param p2gam 
	 */
	protected void interpretNormalP2GArvlMapping(PropertyToGraphicAttributeMappingX p2gam) {

		LOGGER.info("Interpret P2GAM mapping " + p2gam.toStringSummary() );

		try {
			
			GraphicAttribute tga = p2gam.getTargetAttribute();

		    // get a statement set 
		    Set<Statement> stmtSet = RVLUtils.findRelationsOnInstanceOrClassLevel(
		    		modelSet,
		    		OGVICProcess.GRAPH_DATA,
		    		(PropertyMappingX) p2gam.castTo(PropertyMappingX.class),
		    		false,
		    		null,
		    		null
		    		); 

			// get the mapping table SV->TV
			Map<Node, Node> svUriTVuriMap = p2gam.getCalculatedValues(stmtSet);	
		    
		    
		    // for all statements check whether there is a tv for the sv
		    for (Iterator<Statement> stmtSetIt = stmtSet.iterator(); stmtSetIt
					.hasNext();) {
		    	
		    	Statement statement = (Statement) stmtSetIt.next();
		    	
				// create a GO for each subject of the statement
			    GraphicObjectX go = createOrGetGraphicObject(statement.getSubject());

		    	Node sv = statement.getObject();

				LOGGER.finest("trying to find and apply value mapping for sv " + sv.toString());
				
				// get the target value for the sv
		    	Node tv = svUriTVuriMap.get(sv);
		    	
		    	// if we found a tv for the sv
		    	if (null != tv) {
			    	applyGraphicValueToGO(tga, tv, sv, go);	
		    	}
		    	
			}
		    
		} catch (InsufficientMappingSpecificationException e) {
			LOGGER.warning("No resources will be affected by mapping " + p2gam.asURI() + " (" + e.getMessage() + ")" );
		} 
			
	}
	
	// may be moved to GraphicObjectX class
	private void applyGraphicValueToGO(GraphicAttribute tga,
			Node tv, Node sv, GraphicObjectX go) {
		
		if (null != tga && null != tv && null != sv && null != go ) {
			
			LOGGER.finest("Setting tv " + tv + " for sv " + sv);
			
			// if we are mapping to named colors
		    if(tga.asURI().toString().equals("http://purl.org/viso/graphic/color_named")) {
		    	Color color = Color.getInstance(modelVISO, tv.asURI());
		    	go.setColornamed(color);
		    	LOGGER.finer("Set color named to " + color + " for sv " + sv);
		    }
		    
			// if we are mapping to lightness
		    if(tga.asURI().toString().equals("http://purl.org/viso/graphic/color_hsl_lightness")) {
		    	go.setColorhsllightness(tv);
		    	LOGGER.finer("Set color hsl lightness to " + tv.toString() + " for sv " + sv);
		    }
		    
			// if we are mapping to named shapes
		    if(tga.asURI().toString().equals("http://purl.org/viso/graphic/shape_named")) {
		    	Shape shape = ShapeX.getInstance(modelVISO, tv.asURI());
		    	go.setShapenamed(shape);
		    	LOGGER.finer("Set shape to " + shape + " for sv " + sv + NL);
		    }
		    
			// if we are mapping to width
		    if(tga.asURI().toString().equals("http://purl.org/viso/graphic/width")) {
		    	Float width = new Float(tv.asLiteral().getValue());
		    	go.setWidth(width);
		    	LOGGER.finer("Set width to float value " + width + " for sv " + sv + NL);
		    }
		    
			// if we are mapping to labeling_attachedBy
		    if(tga.asURI().equals(Labeling.LABELINGATTACHEDBY)) {
		    	GraphicObjectToObjectRelation attachementRelation = GraphicObjectToObjectRelation.getInstance(modelVISO, tv.asURI());
		    	Labeling nAryLabeling = new Labeling(modelAVM, true);
		    	nAryLabeling.setLabelingattachedBy(attachementRelation);
		    	go.setLabeledwith(nAryLabeling);
		    	LOGGER.finer("Set labeling attachment to " + attachementRelation + " for sv " + sv + NL);
		    }
		}
		
		else {
			LOGGER.warning("Could not set target value, since one of the required parameters was null.");
		}
	}
	
	

	private void applyGraphicValueToGOsRepresentingNodesRelatedVia(
			GraphicAttribute tga, Node tv, Resource subject, Property inheritedBy) {
		
			Set<Resource> relatedResources = RVLUtils.getRelatedResources(modelSet, subject, inheritedBy);
			
			LOGGER.finest("related resources " + relatedResources.toString() + " will receive same tv (" + tv + ")");
		
			// iterate over set, create GOs and applyGraphicValueToGo ... TODO replace parameter subject!
			
			for (Resource resource : relatedResources) {
				applyGraphicValueToGO(tga, tv, subject, createOrGetGraphicObject(resource));
			}
	}

	/**
	 * Interprets only the simple P2GA mappings, i.e. those without need for calculating value mappings. 
	 * Unlike interpretNormalP2GAMappings, multiple VMs are considered.
	 * Creates GO for all affected resources if they don't exist already.
	 */
	protected void interpretSimpleP2GArvlMappings() {
		
		Set<PropertyToGraphicAttributeMappingX> setOfSimpleP2GAMappings = getAllP2GAMappingsWithExplicitMappings();
		
		LOGGER.info(NL + "Found " +setOfSimpleP2GAMappings.size()+ " simple P2GA mappings.");
		
		// for each simple mapping
		for (Iterator<PropertyToGraphicAttributeMappingX> iterator = setOfSimpleP2GAMappings
				.iterator(); iterator.hasNext();) {
			
			PropertyToGraphicAttributeMappingX p2gam = (PropertyToGraphicAttributeMappingX) iterator.next();
			
			// caching
			p2gam = p2gam.tryReplaceWithCashedInstanceForSameURI(p2gam);
			
			if (p2gam.isDisabled()) {
				LOGGER.info("Ignored disabled simple P2GAM mapping " + p2gam );
				continue;
			}

			LOGGER.info("Interpret simple P2GAM mapping " + p2gam );
			
			// get the mapping table SV->TV
			Map<Node, Node> svUriTVuriMap = p2gam.getExplicitlyMappedValues();	
			
			try {
				GraphicAttribute tga = p2gam.getTargetAttribute();
	
			    Set<Statement> theStatementWithOurObject = RVLUtils.findRelationsOnInstanceOrClassLevel(
			    		modelSet,
			    		OGVICProcess.GRAPH_DATA,
			    		(PropertyMappingX) p2gam.castTo(PropertyMappingX.class),
			    		false,
			    		null,
			    		null
			    		); 

			    for (Iterator<Statement> stmtSetIt = theStatementWithOurObject.iterator(); stmtSetIt
						.hasNext();) {
					Statement statement = (Statement) stmtSetIt.next();
					
					// create a GO for each subject
				    GraphicObjectX go = createOrGetGraphicObject(statement.getSubject());
				    
			    	Node sv = statement.getObject(); 
			    	Resource subject = statement.getSubject();
							
					// get the target value for the sv
			    	Node tv = svUriTVuriMap.get(sv);
			    	
			    	// if we found a tv for the sv
			    	if (null != tv) {
			    		
			    		// apply the target value to the GO itself
			    		
			    		applyGraphicValueToGO(tga, tv, sv, go);
			    		
			    		// handle inheritance of target values via arbitrary relations
			    		
			    		Property inheritedBy = ((PropertyMappingX)p2gam.castTo(PropertyMappingX.class)).getInheritedBy();
						
						// temp only support some and all values from ... // TODO these checks are also done in findRelationsOnClassLevel
						if (null!=inheritedBy && !(inheritedBy.toString().equals(Restriction.SOMEVALUESFROM.toString())
								|| inheritedBy.toString().equals(Restriction.ALLVALUESFROM.toString())	
								|| inheritedBy.toString().equals(RVL.TBOX_RESTRICTION)
								|| inheritedBy.toString().equals(RVL.TBOX_DOMAIN_RANGE)	
								)) {
							LOGGER.fine("Mapped value " + tv + " will be inherited to GOs representing nodes related to " + 
								subject + "("+ AVMUtils.getGoodLabel(subject, modelSet.getModel(OGVICProcess.GRAPH_DATA)) +") via " + inheritedBy);
						
							applyGraphicValueToGOsRepresentingNodesRelatedVia(tga, tv, subject, inheritedBy);
							
						} 
			    	}
			    }
			
			} catch (InsufficientMappingSpecificationException e) {
				LOGGER.warning("No resources will be affected by mapping " + p2gam + " (" + e.getMessage() + ")" );
			} 
			
		}
	}
	

}
