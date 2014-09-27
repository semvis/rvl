package org.purl.rvl.example.basic;

import java.io.FileNotFoundException;

import org.junit.Test;
import org.ontoware.rdf2go.Reasoning;
import org.purl.rvl.interpreter.test.TestOGVICProcess;
import org.purl.rvl.tooling.avm2d3.D3GeneratorTreeJSON;
import org.purl.rvl.tooling.codegen.rdfreactor.OntologyFile;
import org.purl.rvl.tooling.process.ExampleData;
import org.purl.rvl.tooling.process.ExampleMapping;

public class UseCaseVISOClasses extends TestOGVICProcess {
	
	@Test
	public void testOGVICProcess() throws FileNotFoundException {
		
		project.setReasoningDataModel(Reasoning.rdfs);
		
		project.registerMappingFile(ExampleMapping.VISO_EXAMPLE_BOOTSTRAP);
		project.registerDataFile(OntologyFile.VISO_GRAPHIC);
		project.registerDataFile(ExampleData.VISO_EXTRA_DATA);
		
		//project.setRvlInterpreter(new SimpleRVLInterpreter());
		project.setD3Generator(new D3GeneratorTreeJSON());
		//project.setD3Generator(new D3GeneratorSimpleJSON());
		

		
		loadProjectAndRunProcess();
	}

	@Override
	protected String getExpectedD3JSONFileName() {
		// TODO Auto-generated method stub
		return null;
	}


}
