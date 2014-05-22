package org.purl.rvl.java.rvl;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

/**
 * @author Jan Polowinski
 *
 */
public class MappingX extends org.purl.rvl.java.gen.rvl.Mapping implements MappingIF {

	static final String NL =  System.getProperty("line.separator");

	
	public MappingX(Model model, BlankNode bnode, boolean write) {
		super(model, bnode, write);
		// TODO Auto-generated constructor stub
	}

	public MappingX(Model model, boolean write) {
		super(model, write);
		// TODO Auto-generated constructor stub
	}

	public MappingX(Model model, Resource instanceIdentifier, boolean write) {
		super(model, instanceIdentifier, write);
		// TODO Auto-generated constructor stub
	}

	public MappingX(Model model, String uriString, boolean write)
			throws ModelRuntimeException {
		super(model, uriString, write);
		// TODO Auto-generated constructor stub
	}

	protected MappingX(Model model, URI classURI, Resource instanceIdentifier,
			boolean write) {
		super(model, classURI, instanceIdentifier, write);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1457148355972827796L;

	public String toStringDetailed() {
		String s = "################################################" + NL;
		String label = this.getAllLabel_as().firstValue();
		Boolean includeInLegend = this.getAllIncludeinlegend_as().firstValue();
		
			s += "     ID: " + this.toString() + NL;
		
		// label
		if (null != label) {
			s += "     Label: " + label + NL;
		} else {
			s += "     Mapping without label (" + super.toString() + ")" + NL;
		}
		// legends
		if (null!=includeInLegend && includeInLegend) {
			s += "     Mapping will be included in legends." + NL;
		}
		
		return s;
	}

	public boolean isDisabled() {
		if (this.hasDisabled()) {
			return this.getAllDisabled_as().firstValue();
		} else return false;
	}

}