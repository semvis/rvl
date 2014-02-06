package org.purl.rvl.interpreter.usecase;

import org.junit.Test;
import org.purl.rvl.interpreter.test.TestOGVICProcess;
import org.purl.rvl.tooling.avm.D3GeneratorSimpleJSON;
import org.purl.rvl.tooling.process.ExampleData;
import org.purl.rvl.tooling.process.ExampleMapping;
import org.purl.rvl.tooling.rvl2avm.SimpleRVLInterpreter;

public class UseCaseRVLExampleData extends TestOGVICProcess {
	
	@Test
	public void testOGVICProcess() {

		process.setUriStart("http://purl.org/rvl/example-data");
		process.setJsonFileNameRel("../org.purl.rvl.tooling.d3vis/examples/force-directed-graph/data.json");

		process.registerMappingFile(ExampleMapping.RVL_EXAMPLE_MINI);
		process.registerDataFile(ExampleData.RVL_EXAMPLE);
		
		process.setRvlInterpreter(new SimpleRVLInterpreter());
		process.setD3Generator(new D3GeneratorSimpleJSON());
		
		process.runOGVICProcess();
	}


}