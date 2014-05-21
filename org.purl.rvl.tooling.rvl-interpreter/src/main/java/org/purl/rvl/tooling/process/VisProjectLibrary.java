package org.purl.rvl.tooling.process;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ontoware.rdf2go.Reasoning;
import org.purl.rvl.tooling.ModelBuilder;
import org.purl.rvl.tooling.avm2d3.D3GeneratorSimpleJSON;
import org.purl.rvl.tooling.avm2d3.D3GeneratorTreeJSON;
import org.purl.rvl.tooling.codegen.rdfreactor.OntologyFile;

/**
 * @author Jan Polowinski
 *
 */
public class VisProjectLibrary {
	
	Map<String,VisProject> library = new HashMap<String,VisProject>();
	
	private final static Logger LOGGER = Logger.getLogger(VisProjectLibrary.class.getName()); 

	/**
	 * @throws FileNotFoundException 
	 * 
	 */
	public VisProjectLibrary() {
		super();
		try {
			initWithUseCaseTestProjects();
		} catch (FileNotFoundException e) {
			LOGGER.severe("One of the files could not be read into the visualisation project library:");
			LOGGER.severe(e.getMessage());
			System.exit(0);
		}
	}
	
	public void initWithUseCaseTestProjects() throws FileNotFoundException {
		
		//////////////////////////////////////////////////////////////////
		// Amino-Acids
		///////////////////////////////////////////////////////////////////
		VisProject useCaseAA = new VisProject("aa");
		useCaseAA.registerMappingFile(ExampleMapping.AA);
		useCaseAA.registerDataFile(ExampleData.AA);
		//useCaseAA.setRvlInterpreter(new SimpleRVLInterpreter());
		useCaseAA.setD3Generator(new D3GeneratorTreeJSON());
		//useCaseAA.setD3Generator(new D3GeneratorSimpleJSON());
		storeProject(useCaseAA);
		
		//////////////////////////////////////////////////////////////////
		// AVM Bootstrap
		///////////////////////////////////////////////////////////////////
		VisProject avmBootstrap = new VisProject("avmbootstrap");
		//avmBootstrap.setWriteAVM(false);
		avmBootstrap.registerMappingFile(ExampleMapping.AVM_EXAMPLE_BOOTSTRAP);
		avmBootstrap.registerDataFile(ExampleData.AVM);
		avmBootstrap.registerDataFile(ExampleData.AVM_EXTRA_DATA);
		//avmBootstrap.setRvlInterpreter(new SimpleRVLInterpreter());
		avmBootstrap.setD3Generator(new D3GeneratorSimpleJSON());
		//avmBootstrap.setD3Generator(new D3GeneratorTreeJSON());
		storeProject(avmBootstrap);
		
		//////////////////////////////////////////////////////////////////
		// RO Instance Data
		///////////////////////////////////////////////////////////////////
		VisProject useCaseROInstanceData = new VisProject("roinstancedata");
		useCaseROInstanceData.registerMappingFile(ExampleMapping.RO_SOCIAL_NETWORK);
		useCaseROInstanceData.registerDataFile(ExampleData.RO_SOCIAL_NETWORK);
		useCaseROInstanceData.registerDataFile(ExampleData.RO_SOCIAL_NETWORK_EXTRA_DATA);
		useCaseROInstanceData.setReasoningDataModel(Reasoning.rdfs);
		//useCaseROInstanceData.setRvlInterpreter(new SimpleRVLInterpreter());
		useCaseROInstanceData.setD3Generator(new D3GeneratorSimpleJSON());
		//useCaseROInstanceData.setD3Generator(new D3GeneratorTreeJSON());
		storeProject(useCaseROInstanceData);

		//////////////////////////////////////////////////////////////////
		// PO
		///////////////////////////////////////////////////////////////////
		VisProject useCasePO = new VisProject("po");
		useCasePO.registerMappingFile(ExampleMapping.PO);
		useCasePO.registerDataFile(ExampleData.PO);
		useCasePO.registerDataFile(ExampleData.PO_EXTRA_DATA);
		//useCasePO.setRvlInterpreter(new SimpleRVLInterpreter());
		useCasePO.setD3Generator(new D3GeneratorTreeJSON());
		//useCasePO.setD3Generator(new D3GeneratorSimpleJSON());
		storeProject(useCasePO);
		
		//////////////////////////////////////////////////////////////////
		// RO
		///////////////////////////////////////////////////////////////////
		VisProject useCaseRO = new VisProject("ro");
		useCaseRO.registerMappingFile(ExampleMapping.RO);
		useCaseRO.registerDataFile(ExampleData.RO_SEMVIS);
		useCaseRO.setD3Generator(new D3GeneratorTreeJSON());
		storeProject(useCaseRO);
		
		//////////////////////////////////////////////////////////////////
		// RVL
		///////////////////////////////////////////////////////////////////
		VisProject useCaseRVLClasses = new VisProject("rvlclasses");
		useCaseRVLClasses.registerMappingFile(ExampleMapping.RVL_EXAMPLE_BOOTSTRAP);
		useCaseRVLClasses.registerDataFile(OntologyFile.RVL);
		useCaseRVLClasses.registerDataFile(ExampleData.RVL_EXTRA_DATA);
		//useCaseRVLClasses.setRvlInterpreter(new SimpleRVLInterpreter());
		useCaseRVLClasses.setD3Generator(new D3GeneratorTreeJSON());
		//useCaseRVLClasses.setD3Generator(new D3GeneratorSimpleJSON());
		storeProject(useCaseRVLClasses);
		
		//////////////////////////////////////////////////////////////////
		// RVL Example Data
		///////////////////////////////////////////////////////////////////
		VisProject useCaseRVLExampleData = new VisProject("rvlexampledata");
		useCaseRVLExampleData.registerMappingFile(ExampleMapping.RVL_EXAMPLE);
		//useCaseRVLExampleData.registerMappingFile(ExampleMapping.RVL_EXAMPLE_MINI);
		useCaseRVLExampleData.registerDataFile(ExampleData.RVL_EXAMPLE);
		//useCaseRVLExampleData.registerMappingFile(ExampleMapping.RVL_EXAMPLE_OLD);
		//useCaseRVLExampleData.registerDataFile(ExampleData.RVL_EXAMPLE_OLD);
		//useCaseRVLExampleData.setRvlInterpreter(new SimpleRVLInterpreter());
		useCaseRVLExampleData.setD3Generator(new D3GeneratorSimpleJSON());
		//useCaseRVLExampleData.setD3Generator(new D3GeneratorTreeJSON());
		storeProject(useCaseRVLExampleData);
		
		//////////////////////////////////////////////////////////////////
		// LLD
		///////////////////////////////////////////////////////////////////
		VisProject useCaseLLD = new VisProject("lld");
		useCaseLLD.registerDataFile(ExampleData.LLD_TEST);
		useCaseLLD.registerDataFile(ExampleData.LLD_EXTRA_DATA);
		useCaseLLD.registerMappingFile(ExampleMapping.LLD);
		//useCaseLLD.setRvlInterpreter(new SimpleRVLInterpreter());
		//useCaseLLD.setD3Generator(new D3GeneratorTreeJSON());
		useCaseLLD.setD3Generator(new D3GeneratorSimpleJSON());
		storeProject(useCaseLLD);
		
		//////////////////////////////////////////////////////////////////
		// VISO_GRAPHIC Classes
		///////////////////////////////////////////////////////////////////
		VisProject useCaseVISOClasses = new VisProject("visoclasses");
		useCaseVISOClasses.registerMappingFile(ExampleMapping.RVL_EXAMPLE_BOOTSTRAP);
		//useCaseVISOClasses.setRvlInterpreter(new SimpleRVLInterpreter());
		useCaseVISOClasses.setD3Generator(new D3GeneratorTreeJSON());
		//useCaseVISOClasses.setD3Generator(new D3GeneratorSimpleJSON());
		storeProject(useCaseVISOClasses);
		
		//////////////////////////////////////////////////////////////////
		// ZFO
		///////////////////////////////////////////////////////////////////
		VisProject useCaseZFO = new VisProject("zfo");
		//useCaseZFO.registerMappingFile(ExampleMapping.ZFO_X);
		useCaseZFO.registerMappingFile(ExampleMapping.ZFO_Y);
		//useCaseZFO.registerDataFile(ExampleData.ZFO_SUBSET);
		useCaseZFO.registerDataFile(ExampleData.ZFO);
		//useCaseZFO.setRvlInterpreter(new SimpleRVLInterpreter());
		useCaseZFO.setD3Generator(new D3GeneratorTreeJSON());
		//useCaseZFO.setD3Generator(new D3GeneratorSimpleJSON());
		storeProject(useCaseZFO);
	}
	
	public void storeProject(VisProject project){
		this.library.put(project.getName(), project);
	}

	public void listProjects() {
		
		System.out.println("Available Visualization Projects: ");	
		
		/*Set<Entry<String,VisProject>> projects = library.entrySet();
		
		for (Iterator<Entry<String,VisProject>> iterator = projects.iterator(); iterator.hasNext();) {
			Entry<String, VisProject> entry = (Entry<String, VisProject>) iterator
					.next();
			
			System.out.println(entry.getValue());	
		}*/
		
		
		Set<String> projectNames = library.keySet();
		
		for (String projectName : projectNames) {
			System.out.println(projectName);	
		}
		
	}

	public VisProject getProject(String useCaseName) {
		return library.get(useCaseName);
	}
	
	public boolean contains(String projectName){
		return library.containsKey(projectName);
	}

}
