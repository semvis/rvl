/**
 * 
 */
package org.purl.rvl.tooling.process;

import java.io.IOException;

import org.purl.rvl.exception.OGVICRepositoryException;

/**
 * Offers an extended example library with visualisation project covering the use cases from the case studies.
 * 
 * @author Jan Polowinski
 *
 */
public class OGVICConsoleExamples extends OGVICConsole {
	
    public static void main(String[] args) throws IOException, OGVICRepositoryException {
    	
    	OGVICConsole console =  new OGVICConsoleExamples();
    	console.runConsole();
        
    }
	
	protected VisProjectLibrary getVisProjectLibrary() {
		VisProjectLibraryExamples library = VisProjectLibraryExamples.getInstance();
		return library;
	}

}
