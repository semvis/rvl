package org.purl.rvl.tooling.util;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.QueryResultTable;
import org.ontoware.rdf2go.model.QueryRow;
import org.ontoware.rdfreactor.schema.rdfs.Resource;
import org.purl.rvl.java.gen.viso.graphic.DirectedLinking;
import org.purl.rvl.java.gen.viso.graphic.UndirectedLinking;
import org.purl.rvl.java.viso.graphic.Color;
import org.purl.rvl.java.viso.graphic.GraphicObject;

public class AVMUtils {
	
	private final static Logger LOGGER = Logger.getLogger(AVMUtils.class.getName()); 
	
	public static void listAllColors(Model model) {
		System.out.println("List of all colors in the model:");
		System.out.println();
		
		ClosableIterator<? extends org.purl.rvl.java.gen.viso.graphic.Color> goIt = 
				org.purl.rvl.java.gen.viso.graphic.Color.getAllInstances_as(model).asClosableIterator();
		while (goIt.hasNext()) {
			Color color = (Color) goIt.next().castTo(Color.class);
			LOGGER.info(color.toString());
		}	
	}
	
	/**
	 * List all Graphic Objects in the model
	 */
	static void listAllGOs(Model model) {	
		
		System.out.println("List of all Graphic Objects in the model:");
		System.out.println();
		
		ClosableIterator<? extends org.purl.rvl.java.gen.viso.graphic.GraphicObject> goIt = 
				org.purl.rvl.java.gen.viso.graphic.GraphicObject.getAllInstances_as(model).asClosableIterator();
		while (goIt.hasNext()) {
			GraphicObject go = (GraphicObject) goIt.next().castTo(GraphicObject.class);
			System.out.println(go);
		}		
	}
	
	/**
	 * List all Resources in the model
	 */
	static void listAllResourcesWithTheirTypes(Model model){	
		
		System.out.println("List of all rdfreactor ... Resources in the model:");
		System.out.println();
		
		ClosableIterator<? extends Resource> resIt = 
			Resource.getAllInstance_as(model).asClosableIterator();
		while (resIt.hasNext()) {
			Resource res = (Resource) resIt.next();
	
			LOGGER.info(res.toString());
			//LOGGER.info("Types:" + go.getAllType_as().asArray()[0].asURI());
			
			for (org.ontoware.rdfreactor.schema.rdfs.Class type : res.getAllType_as().asList()) {
				try {
					LOGGER.info("T: " + type.asURI());
				} catch (ClassCastException e) {
					LOGGER.severe("evtl. blanknote");
				}
			}
		}		
	}
	
	/**
	 * Get only the GraphicObjects that need to be displayed. Remove objects
	 * playing the role of connectors for example.
	 * 
	 * @return
	 */
	public static Set<GraphicObject> getRelevantGraphicObjects(Model model) {

		Set<GraphicObject> gos = new HashSet<GraphicObject>();

		// get all subjects and the sv/tv table via SPARQL
		String query = "" + 
				"SELECT DISTINCT ?go " + 
				"WHERE { " +
				"	?go a " + GraphicObject.RDFS_CLASS.toSPARQL() + " ." +
//				"	?someRelation " + DirectedLinking.STARTNODE.toSPARQL() + " ?go ." +
				"	FILTER NOT EXISTS { ?someRelation " + DirectedLinking.LINKINGCONNECTOR .toSPARQL() + " ?go . }" +
				"} ";
		System.out.println(query);

		QueryResultTable explMapResults = model.sparqlSelect(query);
		for (QueryRow row : explMapResults) {
			System.out.println(row.getValue("go"));
			gos.add((GraphicObject) GraphicObject.getInstance(model, row
					.getValue("go").asURI()).castTo(GraphicObject.class));
		}

		return gos;
	}

}