package org.purl.rvl.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.ontoware.rdf2go.Reasoning;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.purl.rvl.exception.D3GeneratorException;
import org.purl.rvl.exception.OGVICModelsException;
import org.purl.rvl.exception.OGVICRepositoryException;
import org.purl.rvl.tooling.avm2d3.D3GeneratorDeepLabelsJSON;
import org.purl.rvl.tooling.avm2d3.GraphicType;
import org.purl.rvl.tooling.codegen.rdfreactor.OntologyFile;
import org.purl.rvl.tooling.commons.FileRegistry;
import org.purl.rvl.tooling.commons.utils.FileResourceUtils;
import org.purl.rvl.tooling.model.ModelManager;
import org.purl.rvl.tooling.process.OGVICProcess;
import org.purl.rvl.tooling.process.VisProject;
import org.purl.rvl.tooling.process.VisProjectLibraryExamples;

/**
 * @author Jan Polowinski
 * based on http://www.vogella.com/tutorials/REST/article.html
 */
@Path("/projects")
public class ProjectsResource {

	// Allows to insert contextual objects into the class,
	// e.g. ServletContext, Request, Response, UriInfo
	@Context
	UriInfo uriInfo;
	@Context
	Request request;
	
	
	final static Random random = new Random();

	
	// Return the list of projects to a browser
	@GET
	@Produces(MediaType.TEXT_XML)
	public List<VisProject> getVisProjectsBrowser() {
		List<VisProject> projects = new ArrayList<VisProject>();
		try {
			projects.addAll(VisProjectLibraryExamples.getInstance().getProjects());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return projects;
	}

	// Return the list of projects for applications
	@GET
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
	public List<VisProject> getProjects() {
		List<VisProject> projects = new ArrayList<VisProject>();
		projects.addAll(VisProjectLibraryExamples.getInstance().getProjects());
		return projects;
	}
	
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void newVisProject(@FormParam("id") String id, @FormParam("name") String name,
			@FormParam("description") String description, @Context HttpServletResponse servletResponse)
			throws IOException {

		System.out.println("Creating new project " + id);

		VisProject project = new VisProject(id);
		if (name != null) {
			project.setName(name);
		}
		if (description != null) {
			project.setDescription(description);
		}
		VisProjectLibraryExamples.getInstance().storeProject(project);

		servletResponse.sendRedirect("http://localhost:8585/semvis/forms/run.html");
		servletResponse.setStatus(HttpServletResponse.SC_OK);
	}
	
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/run")
	public Response runNewVisProject(
			@FormParam("id") String id,
			@FormParam("graphicType") String graphicType,
			@FormParam("data") String data,
			@FormParam("mappings") String mappings,
			@FormParam("stay") String stay,
			@Context HttpServletResponse servletResponse
			)
			throws IOException, URISyntaxException {
		
		System.out.println("Running new project " + id);

		if (id == null || id.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("<message>ID is required.</message>").build();
		} 
		
		VisProject project = new VisProject(id);
		project.setReasoningDataModel(Reasoning.rdfs);
		project.setDefaultGraphicType(graphicType);
		
		if (data != null) {
			File newTmpDataFile = saveToTempFile(data);
			project.registerDataFile(newTmpDataFile.getPath());
		}
		if (mappings != null) {
			File newTmpMappingFile = saveToTempFile(mappings);
			project.registerMappingFile(newTmpMappingFile.getPath());
		}
		
		VisProjectLibraryExamples.getInstance().storeProject(project);
		
		System.out.println("Created new project " + project.getId());
		
		try {
			runProject(project.getId());
		} catch (OGVICRepositoryException e) {
			e.printStackTrace();
			return Response.status(Status.EXPECTATION_FAILED).build();
			// TODO correct return status?
		}

		if (null != stay && stay.equals("on")) {
			// just stay on the page, don't show any new content
			return Response.status(Status.NO_CONTENT).build();
		} else {
			//URI redirectTo = new URI("http://localhost:8080/semvis/projects/");
			URI redirectTo = new URI("http://localhost:8585/semvis/gen/html/index-rest.html");
			System.out.println("Redirecting to " + redirectTo);
			return Response.seeOther(redirectTo).build();		
		}
			
//		code below only works when servlets are available:
//		"When deploying a JAX-RS application using servlet then ServletConfig, ServletContext, HttpServletRequest and HttpServletResponse are available using @Context. " taken from: 
//		https://jersey.java.net/documentation/latest/user-guide.html#d0e5169
//
//		servletResponse.sendRedirect("http://localhost:8080/semvis/projects");
//		servletResponse.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
//		servletResponse.setHeader("Location", "http://localhost:8080/semvis/projects");
//		servletResponse.setContentType(MediaType.APPLICATION_XML);
//		servletResponse.setStatus(HttpServletResponse.SC_OK);
//		servletResponse.setHeader("Access-Control-Allow-Origin", "*");
//		servletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
	}
	
	public static File saveToTempFile(String data) throws IOException {
		File tmpFile = new File("tmp/data/tmp-data-" + random.nextDouble() + System.currentTimeMillis()  + ".tmp");
		FileWriter writer;
		writer = new FileWriter(tmpFile);
		writer.write(data);
		writer.close();
		return tmpFile;
	}

	@GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
	@Path("/run/{id}")
    public String runVisProject(@PathParam("id") String id, @Context HttpServletResponse servletResponse) {
		
		System.out.println("/run/" + id);
		
		String jsonResult;
		try {
			jsonResult = runProject(id);
		} catch (OGVICRepositoryException e) {
			jsonResult =  "";
			e.printStackTrace();
		}
		
//		if (null == servletResponse) {
//			System.out.println("servlet response was null");
//		}
		
		return jsonResult;
    }
	
	@GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
	@Path("/run/external")
    public String runExternalEditingProject(@Context HttpServletResponse servletResponse) {

		String jsonResult;
		try {
			jsonResult = runExternalEditingProject(GraphicType.FORCE_DIRECTED_GRAPH);
		} catch (OGVICRepositoryException e) {
			jsonResult =  e.getMessage();
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			jsonResult =  e.getMessage();
			e.printStackTrace();
		}
		
//		if (null == servletResponse) {
//			System.out.println("servlet response was null");
//		}
		
		return jsonResult;
    }
	
	@GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
	@Path("/run/external/{graphicType}")
    public String runExternalEditingProject(@PathParam("graphicType") String graphicType, @Context HttpServletResponse servletResponse) {

		String jsonResult;
		try {
			jsonResult = runExternalEditingProject(graphicType);
		} catch (OGVICRepositoryException e) {
			jsonResult =  e.getMessage();
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			jsonResult =  e.getMessage();
			e.printStackTrace();
		}
		
		return jsonResult;
    }
	
	
	@GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
	@Path("/latest")
    public String getLatestGeneratedD3() {
		
		System.out.println("/run/latest");
		
		String jsonResult;
		try {
			jsonResult = OGVICProcess.getInstance().getGeneratedD3json();
		} catch (OGVICRepositoryException e) {
			jsonResult =  "";
			e.printStackTrace();
		}
		
		System.out.println(jsonResult);
		
		return jsonResult;
    }
	
	@GET
    @Produces({MediaType.TEXT_PLAIN})
	@Path("/example/data")
    public String getExampleData() {
		try {
			return FileResourceUtils.getFromExampleData("rvl-example-data.ttl");
		} catch (IOException e) {
			e.printStackTrace();
			return "# example data could not be found.";
		}
    }
	
	@GET
    @Produces({MediaType.TEXT_PLAIN})
	@Path("/example/mappings")
    public String getExampleMappings() {
		try {
			return FileResourceUtils.getFromExampleMappings("rvl-example-mappings.ttl");
		} catch (IOException e) {
			e.printStackTrace();
			return "# example mappings could not be found.";
		}
    }
	
	@GET
    @Produces({MediaType.TEXT_PLAIN})
	@Path("/mappingmodel/{id}")
    public String getMappingModel(@PathParam("id") String id) throws OGVICRepositoryException {
		
		ModelManager modelManager = ModelManager.getInstance();
		VisProject project = VisProjectLibraryExamples.getInstance().getProject(id);
		
		FileRegistry mfr = project.getMappingFileRegistry();

		modelManager.initInternalModels(); // TODO refaktor? actually only RVL model is required here
		modelManager.initMappingsModel(project.getMappingFileRegistry());
		
		Model model = modelManager.getMappingsModel();
		
		return model.serialize(Syntax.Turtle);
		//return "requested mapping model for project " + id;
    }
	
	@GET
    @Produces({MediaType.TEXT_PLAIN})
	@Path("/datamodel/{id}")
    public String getDataModel(@PathParam("id") String id) throws OGVICRepositoryException {
		
		ModelManager modelManager = ModelManager.getInstance();
		VisProject project = VisProjectLibraryExamples.getInstance().getProject(id);

		modelManager.initDataModel(project.getDataFileRegistry(), project.getReasoningDataModel());
		
		Model model = modelManager.getDataModel();
		
		return model.serialize(Syntax.Turtle);
    }



	// Defines that the next path parameter after projects is
	// treated as a parameter and passed to the ProjectsResources
	// Allows to type http://localhost:8080/semvis/projects/1
	// 1 will be treaded as parameter project and passed to ProjectResource
	@Path("{project}")
	public ProjectResource getProject(@PathParam("project") String id) {
		return new ProjectResource(uriInfo, request, id);
	}
	
	
	private String runProject(String id) throws OGVICRepositoryException {
	
		OGVICProcess process = OGVICProcess.getInstance();
	
		try {
			process.registerOntologyFile(OntologyFile.VISO_GRAPHIC);
			process.registerOntologyFile(OntologyFile.RVL);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		VisProject project = VisProjectLibraryExamples.getInstance().getProject(id);
		
		try {
			process.loadProject(project);
		} catch (OGVICRepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		try {
			process.runOGVICProcess();
		} catch (D3GeneratorException | OGVICModelsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String json = "could not be generated";
		
		try {
			json = process.getGeneratedD3json();
		} catch (Exception e) {
			// TODO: handle exception
		}
	
		return json;
	}

	private String runExternalEditingProject(String defaultGraphicType) throws FileNotFoundException, OGVICRepositoryException {
	
		String json;
		OGVICProcess process = OGVICProcess.getInstance();
	
		process.registerOntologyFile(OntologyFile.VISO_GRAPHIC);
		process.registerOntologyFile(OntologyFile.RVL);
		
		VisProject project = new VisProject("external-editing-test");
		project.setReasoningDataModel(Reasoning.rdfs);
		project.registerDataFile("editing/data.ttl");
		project.registerMappingFile("editing/mapping.ttl");
		project.setDefaultGraphicType(defaultGraphicType);
		
		// load from optional files
		try {
			project.registerDataFile("editing/ontology.ttl");
		} catch (FileNotFoundException e) {}
		try {
			project.registerMappingFile("/example-commons/rvl-example-commons.ttl");
		} catch (FileNotFoundException e) {}
		
		try {
			process.loadProject(project);
		} catch (OGVICRepositoryException e) {
			json = e.getMessage();
			e.printStackTrace();
		}
	
		try {
			process.runOGVICProcess();
		} catch (D3GeneratorException | OGVICModelsException e) {
			json = e.getMessage();
			e.printStackTrace();
		}

		try {
			json = process.getGeneratedD3json();
		} catch (Exception e) {
			json = e.getMessage();
			e.printStackTrace();
		}
	
		return json;
	}

}