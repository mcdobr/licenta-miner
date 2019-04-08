package me.mircea.licenta.scraper.webservices;

import com.fasterxml.jackson.databind.node.ObjectNode;
import me.mircea.licenta.core.crawl.db.CrawlDatabaseManager;
import me.mircea.licenta.core.crawl.db.model.Job;
import me.mircea.licenta.core.crawl.db.model.JobType;
import me.mircea.licenta.scraper.Scraper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/jobs")
public class JobResource {
	private static final ExecutorService ASYNC_TASK_EXECUTOR = Executors.newCachedThreadPool();
	private static final Logger LOGGER = LoggerFactory.getLogger(JobResource.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Job> listActiveScraperJobs() {
		Iterable<Job> iterable = CrawlDatabaseManager.instance.getActiveJobsByType(JobType.SCRAPE);

		List<Job> jobList = new ArrayList<>();
		iterable.forEach(jobList::add);
		return jobList;
	}

	@GET
	@Path("{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Job getScraperJobStatus(@PathParam("jobId") ObjectId jobId) {
		return CrawlDatabaseManager.instance.getJobById(jobId);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createScraperJob(ObjectNode scrapeRequest) {
		try {
			Scraper scraper;
			if (scrapeRequest.has("continue")) {
				scraper = new Scraper(scrapeRequest.get("seed").asText(),
						new ObjectId(scrapeRequest.get("continue").asText()));
			} else {
				scraper = new Scraper(scrapeRequest.get("seed").asText());
			}

			ASYNC_TASK_EXECUTOR.execute(scraper);
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
