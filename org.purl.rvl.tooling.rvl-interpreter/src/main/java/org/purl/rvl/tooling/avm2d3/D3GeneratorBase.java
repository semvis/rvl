package org.purl.rvl.tooling.avm2d3;

import java.beans.Introspector;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.util.RDFTool;
import org.purl.rvl.exception.d3.D3GeneratorException;
import org.purl.rvl.java.VISOGRAPHIC;
import org.purl.rvl.java.gen.viso.graphic.Containment;
import org.purl.rvl.java.gen.viso.graphic.GraphicObjectToObjectRelation;
import org.purl.rvl.java.gen.viso.graphic.Labeling;
import org.purl.rvl.java.viso.graphic.GraphicObjectX;
import org.purl.rvl.tooling.process.OGVICProcess;
import org.purl.rvl.tooling.util.D3Utils;


/**
 * @author Jan Polowinski
 *
 */
public abstract class D3GeneratorBase implements D3Generator {
	
	protected Model modelAVM;
	protected Model modelVISO;
	
	private final static Logger LOGGER = Logger.getLogger(D3GeneratorBase.class .getName());

	static final String NL = System.getProperty("line.separator");

	protected static final float DEFAULT_WIDTH_NODES = 17;
	protected static final float DEFAULT_WIDTH_CONNECTORS = 17;

	protected static final float LABEL_ICON_SIZE_FACTOR = (float) 0.50; 
	
	
	public D3GeneratorBase() {
		super();
	}

	
	/**
	 * @param modelAVM
	 * @param modelVISO
	 */
	public D3GeneratorBase(Model modelAVM, Model modelVISO) {
		super();
		this.modelAVM = modelAVM;
		this.modelVISO = modelVISO;
	}




	/* (non-Javadoc)
	 * @see org.purl.rvl.tooling.avm2d3.D3Generator#writeJSONToFile(java.lang.String)
	 */
	public void writeJSONToFile(String fileContent){
		try {
			String fileName = OGVICProcess.getInstance().getJsonFileNameRel();
			
			FileWriter writer = new FileWriter(fileName);
			writer.write(fileContent);
			writer.flush();
			writer.close();
			LOGGER.info("JSON written to " + fileName);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.purl.rvl.tooling.avm2d3.D3Generator#init(org.ontoware.rdf2go.model.Model)
	 */
	public void init(Model modelAVM) {
		this.modelAVM = modelAVM;
	}
	
	/* (non-Javadoc)
	 * @see org.purl.rvl.tooling.avm2d3.D3Generator#generateJSONforD3()
	 */
	public abstract String generateJSONforD3() throws D3GeneratorException;
	
	/* (non-Javadoc)
	 * @see org.purl.rvl.tooling.avm2d3.D3Generator#getGenJSONFileName()
	 */
	public abstract String getGenJSONFileName();
	
	/* (non-Javadoc)
	 * @see org.purl.rvl.tooling.avm2d3.D3Generator#getDefaultD3GraphicFile()
	 */
	public abstract String getDefaultD3GraphicFile();


	/**
	 * @param map
	 * @param graphicObject
	 */
	protected void putGraphicAttributes(Map map, GraphicObjectX graphicObject) {
		
		//color
		String colorRGBHex = graphicObject.getColorHex();
		
		map.put("color_rgb_hex", colorRGBHex);
		map.put("color_rgb_hex_combined", graphicObject.getColorRGBHexCombinedWithHSLValues());
		
		// shape
		//String shapeD3Name = graphicObject.getShape();
		//String shapeTextValue = graphicObject.getTextValue();
		
		if(graphicObject.hasTextvalue()){
			map.put("shape_text_value", graphicObject.getTextValue());
		} else {
			map.put("shape_d3_name", graphicObject.getShape());
		}
				
		// dimensions
		if (graphicObject.hasWidth()) {
			map.put("width", graphicObject.getWidth());
		} else if (!graphicObject.hasRole(VISOGRAPHIC.ROLE_LINKING_CONNECTOR)){
			map.put("width", getDefaultWidthNodes());
		}

	}
	
	/**
	 * @param map
	 * @param graphicObject
	 */
	protected void putRepresentedResource(Map<String,Object> map, GraphicObjectX graphicObject) {
		try {
			map.put("uri", graphicObject.getRepresentedResource().toString());
		} catch (NullPointerException e) {
			LOGGER.warning("graphic object " + graphicObject + " does not represent a domain resource.");
		}
	}
	
	protected float getDefaultWidthNodes(){
		return DEFAULT_WIDTH_NODES;
	}
	
	protected float getDefaultWidthConnectors(){
		return DEFAULT_WIDTH_CONNECTORS;
	}


	/**
	 * @param startNodeWidth
	 * @param labels
	 * @param nAryLabeling
	 * @return 
	 */
	protected Map<String, Object> generateLabel(float startNodeWidth, Labeling nAryLabeling) {
		
		Map<String,Object> labelJSON = new HashMap<String, Object>();
		
			
			// defaults
			
			String defaultLabelPositionIcon = "topRight";
			String defaultLabelPositionText = "centerRight";
			
			final GraphicObjectX label = (GraphicObjectX) nAryLabeling
					.getAllLabelinglabel_as().firstValue()
					.castTo(GraphicObjectX.class);
	
			// setting graphic attributes that are valid for any kind of label
			
			labelJSON.put("color_rgb_hex_combined", label.getColorRGBHexCombinedWithHSLValues());
			
			if (label.hasWidth()) {
				labelJSON.put("width", label.getWidth() + ""); 
			} else {
				// TODO text label width should not be the same as for icon labels
				labelJSON.put("width", startNodeWidth*LABEL_ICON_SIZE_FACTOR+""); 
			}
			
			// text label or icon label?
			
			String labelTextValue = label.getTextValue();
			
			if (null != labelTextValue) {
				
				// create text label
				labelJSON.put("type", "text_label");
				labelJSON.put("text_value", D3Utils.shortenLabel(labelTextValue));
				//label1.put("text_value_full", labelTextValue + " (ID: " + startNode.getRepresentedResource() + ")");
				labelJSON.put("text_value_full", labelTextValue);
				
			} else {
				
				// create icon label
				labelJSON.put("type", "icon_label");
				labelJSON.put("shape_d3_name", label.getShape());
			}
			
			GraphicObjectToObjectRelation attachementRelation = nAryLabeling.getAllLabelingattachedBy_as().firstValue();
			Resource position = nAryLabeling.getAllLabelingposition_as().firstValue();
			
			if (null!=position) {
				
				labelJSON.put("position", Introspector.decapitalize(RDFTool.getShortName(position.toString())));
				
			} else if (null!=attachementRelation) {
			
				if (attachementRelation.asURI().equals(Containment.RDFS_CLASS)) {
					
					labelJSON.put("position", "centerCenter");
					
				//} else if (attachementRelation.asURI().equals(Superimposition.RDFS_CLASS)) {}

				} else {
					
					// default label positioning
					
					if (null!=labelTextValue) {
						labelJSON.put("position", defaultLabelPositionText);
					} else {
						labelJSON.put("position", defaultLabelPositionIcon);
					}
				}
			} else {
				
				// default label positioning
				
				if (null!=labelTextValue) {
					labelJSON.put("position", defaultLabelPositionText);
				} else {
					labelJSON.put("position", defaultLabelPositionIcon);
				}	
			}
			
			// ... other positions ...
		
		
		// handle labeling of labels
		putLabels(label, startNodeWidth, labelJSON); // TODO width OK?
			
		return labelJSON;
	}


	/**
	 * @param labelingBase
	 * @param labelingBaseWidth
	 * @param labelingBaseJSONnode
	 */
	protected void putLabels(GraphicObjectX labelingBase, float labelingBaseWidth, Map<String, Object> labelingBaseJSONnode) {
		if (labelingBase.hasLabeledwith()) {
			
			// label objects
			
			List<Map<String,Object>> labels = new LinkedList<Map<String,Object>>();
			labelingBaseJSONnode.put("labels", labels);
	
			ClosableIterator<Labeling> nAryLabelings = labelingBase.getAllLabeledwith_as().asClosableIterator();
			
			// for each labeling relation
			
			while (nAryLabelings.hasNext()) {
				
				Labeling nAryLabeling = (Labeling) nAryLabelings.next();
				
				try {
					Map<String,Object> newLabel = generateLabel(labelingBaseWidth, nAryLabeling);
					labels.add(newLabel);
				} catch (Exception e){
					LOGGER.severe("Problem creating JSON label for labeling relation. Labeling " + nAryLabeling + " will be ignored: " + e.getMessage());
				}
				
			}
	
		}
	}


	/**
	 * Combined method for setting attributes, labels and the represented resource.
	 * 
	 * @param graphicObject
	 * @param jsonObject
	 */
	protected void putAttributesLabelsRepresentedResource(GraphicObjectX graphicObject, Map<String, Object> jsonObject) {

		putRepresentedResource(jsonObject, graphicObject);
		putGraphicAttributes(jsonObject, graphicObject);	
		
		// width (used for calculating label size) // the defaults, which are set when values are missing 
		// could also be stored in the AVM, but the generator should not change the AVM, so a copy 
		// had to be used
		float startNodeWidth = graphicObject.hasWidth()? graphicObject.getWidth() : getDefaultWidthNodes();
		putLabels(graphicObject, startNodeWidth, jsonObject); // TODO width OK?
	}
}