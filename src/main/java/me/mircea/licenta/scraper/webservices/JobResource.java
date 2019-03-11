package me.mircea.licenta.scraper.webservices;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import me.mircea.licenta.scraper.Scraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mircea.licenta.scraper.ProductDatabaseManager;

@Path("/jobs")
public class JobResource {
	private static final ExecutorService ASYNC_TASK_EXECUTOR = Executors.newCachedThreadPool();
	private static final Logger LOGGER = LoggerFactory.getLogger(JobResource.class);
	private static final ProductDatabaseManager DAO = ProductDatabaseManager.instance;
	
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String listActiveCrawlerJobs() {
		return "Wtf";
	}

	@GET
	@Path("{jobId}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCrawlerJobStatus(@PathParam("jobId") int jobId) {
		throw new UnsupportedOperationException();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createCrawlerJob(JsonObject scrapeRequest) {
		try {
			Scraper scraper = new Scraper(scrapeRequest.getString("seed"));

			//ASYNC_TASK_EXECUTOR.submit(scraper);
			return Response.status(202)
					.entity(scraper.getJob())
					.build();
		} catch (IOException e) {
			LOGGER.warn("Could not read a file {}", e);
			return Response.status(500)
					.build();
		}
	}
}
