package org.purl.rvl.java.rvl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.QueryResultTable;
import org.ontoware.rdf2go.model.QueryRow;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;
import org.purl.rvl.java.exception.InsufficientMappingSpecificationExecption;
import org.purl.rvl.java.gen.rvl.GraphicAttribute;
import org.purl.rvl.java.gen.rvl.Property_to_Graphic_AttributeMapping;
import org.purl.rvl.java.gen.rvl.Valuemapping;


public class PropertyToGraphicAttributeMapping extends
		Property_to_Graphic_AttributeMapping implements MappingIF {
	
	private static final long serialVersionUID = 5391124674649010787L;
	static final String NL =  System.getProperty("line.separator");
	
	Map<Node, Node> explicitlyMappedValues;

	public PropertyToGraphicAttributeMapping(Model model, boolean write) {
		super(model, write);
	}

	public PropertyToGraphicAttributeMapping(Model model,
			Resource instanceIdentifier, boolean write) {
		super(model, instanceIdentifier, write);
		// TODO Auto-generated constructor stub
	}

	public PropertyToGraphicAttributeMapping(Model model, URI classURI,
			Resource instanceIdentifier, boolean write) {
		super(model, classURI, instanceIdentifier, write);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Creates and returns a map of URIs of all explicitly mapped values 
	 * and their target graphic values.
	 * @return
	 */
	public Map<Node, Node> getExplicitlyMappedValues(){
		
		if (null == explicitlyMappedValues) {
			
			// TODO: evtl. check already here if VM exist at all with hasValueMapping() for blank nodes the toSPARQL() issued an exception

			explicitlyMappedValues = new HashMap<Node, Node>();
			
			// get all subjects and the sv/tv table via SPARQL
					String querySubjectsAndSVtoTVMapForGivenProperty = "" +
							"SELECT DISTINCT ?sv ?tv " +
							"WHERE { " +
						    	toSPARQL() + " <" + VALUEMAPPING + "> ?vm ." + 
						    "	?vm <" + ValueMapping.SOURCEVALUE + "> ?sv . " +
						    "	?vm <" + ValueMapping.TARGETVALUE + "> ?tv . " + 
							"} ";
			//System.out.println(querySubjectsAndSVtoTVMapForGivenProperty);
			
			QueryResultTable explMapResults = model.sparqlSelect(querySubjectsAndSVtoTVMapForGivenProperty);
			for(QueryRow row : explMapResults) {
				explicitlyMappedValues.put(row.getValue("sv"),row.getValue("tv"));
			}
		}

		return explicitlyMappedValues;
		
		
		/*
		// TRIAL WITH COMPLEX SPARQL QUERY: leads to concurrent modification exception wehen GOs are edited:
		// get mapping and source property
		Property_to_Graphic_AttributeMapping p2gam = Property_to_Graphic_AttributeMapping.getInstance(model, new URIImpl("http://purl.org/rvl/example-mappings/PMWithExplicitValueMappingsAsBlankNodesToColorNamed"));
		System.out.println(p2gam.getAllLabel_as().firstValue());
		Property sp = p2gam.getAllSourceproperty_as().firstValue();
		System.out.println("sp: " + sp);
		
		// get all subjects and the sv/tv table via SPARQL
		String querySubjectsAndSVtoTVMapForGivenProperty = "" +
				"SELECT DISTINCT ?s ?o ?tv " +
				"WHERE { " +
			    	p2gam.toSPARQL() + " <" + PropertyToGraphicAttributeMapping.VALUEMAPPING + "> ?vm ." + 
	//		    	p2gam.toSPARQL() +  " <" + ValueMapping.TARGETVALUE + "> ?ta. " + 
			    "	?vm <" + ValueMapping.SOURCEVALUE + "> ?o . " +
			    "	?vm <" + ValueMapping.TARGETVALUE + "> ?tv . " + 
			    "	?s <" + sp + "> ?o . " +
				"} ";
				
		System.out.println(querySubjectsAndSVtoTVMapForGivenProperty);
		
		Random r = new Random();

		QueryResultTable explMapResults = model.sparqlSelect(querySubjectsAndSVtoTVMapForGivenProperty);
		for(QueryRow row : explMapResults) {
			System.out.println(row);
			// create a new GO
			GraphicObject go = new GraphicObject(model, "http://purl.org/rvl/example-avm/GO_for_" + r.nextInt(), false);
			// relate it to the resource
			// TODO
			// set target attribute TODO: generic TODO: so many casts necessary???
			Color tv = Color.getInstance(model, (Resource)row.getValue("tv"));
//			go.setColornamed(tv);
			goSet.add(go);	
			//System.out.println(go);
		}
		
		*/
	}

	public String toString(){
		
		String s = "";
		
		// try to get the string description from the (manual) PropertyMapping class, which is not in the super-class hierarchy
		PropertyMapping pm = (PropertyMapping) this.castTo(PropertyMapping.class);
		s += pm.toString();
		
		//targetAttribute is specific to P2GAM
		GraphicAttribute tga = this.getAllTargetattribute_as().firstValue();
		String tgaString = tga.getAllLabel_as().count()>0 ? tga.getAllLabel_as().firstValue() : tga.toString();
		s += "     Target graphic attribute: " + tgaString + NL ;
		
		if(this.hasValuemapping()) {

			Map<Node, Node> svUriTVuriMap = this.getExplicitlyMappedValues();	
			
			if(!svUriTVuriMap.isEmpty()){
				s += "     (value mappings not yet calculated ... showing only explicit ones:)" + NL;
				
				for (Entry<Node, Node> entry : svUriTVuriMap.entrySet()) {
					Node sv = entry.getKey();
					Node tv = entry.getValue();
					s += "       " + sv + " --> " + tv + NL;
				}
				
			}
			else {
				s += "     (value mappings not yet calculated ...)" + NL;
				
				s += "     Value mappings:" + NL;
				ClosableIterator<Valuemapping> vmIterator = this.getAllValuemapping_as().asClosableIterator();
				while (vmIterator.hasNext()) {
					ValueMapping vm = (ValueMapping) vmIterator.next().castTo(ValueMapping.class);
					s += "" + vm + NL;
				}
			}
		}
		else {
			s += "     (with no explicit value mappings)" + NL;
		}
		
		/*
		// seems to cause an exception, but not on every machine?! "java.lang.UnsupportedOperationException: Variable (Singleton) cannot be used for SPARQL queries"
		s += "Explicit (simple 1-1) VMs:" + NL;
		Map<Node, Node> map = getExplicitlyMappedValues();
		Set<Entry<Node, Node>> set = map.entrySet();
		for (Iterator<Entry<Node, Node>> iterator = set.iterator(); iterator.hasNext();) {
			Entry<Node, Node> svURItvURIPair = (Entry<Node, Node>) iterator.next();
			s+= "	" + svURItvURIPair.getKey() + " --> " + svURItvURIPair.getValue() + NL;
		}
		*/
		return s;
	}

	/* (non-Javadoc)
	 * @see org.purl.rvl.java.rvl.MappingIF#isDisabled()
	 */
	public boolean isDisabled() {
		if (this.hasDisabled()) {
			return this.getAllDisabled_as().firstValue();
		} else return false;
	}

	public GraphicAttribute getTargetAttribute() throws InsufficientMappingSpecificationExecption {
		if (hasTargetattribute()) {
			return this.getAllTargetattribute_as().firstValue();
		} else 
			throw new InsufficientMappingSpecificationExecption();
	}

}
