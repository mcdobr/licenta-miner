package me.mircea.licenta.scraper.webservices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import me.mircea.licenta.core.crawl.db.CrawlDatabaseManager;
import me.mircea.licenta.core.crawl.db.model.Job;
import me.mircea.licenta.core.crawl.db.model.JobActiveOnHost;
import me.mircea.licenta.core.crawl.db.model.JobType;
import me.mircea.licenta.core.parser.utils.HtmlUtil;
import me.mircea.licenta.scraper.Scraper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/jobs")
public class ScrapeJobResource {
	private static final ExecutorService ASYNC_TASK_EXECUTOR = Executors.newCachedThreadPool();
	private static final Logger LOGGER = LoggerFactory.getLogger(ScrapeJobResource.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Job> listActiveScraperJobs() {
		Iterable<Job> iterable = CrawlDatabaseManager.instance.getActiveJobsByType(JobType.SCRAPE);
		return Lists.newArrayList(iterable);
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
	public Response createScraperJob(@Context HttpServletRequest request, ObjectNode scrapeRequest) {
		Scraper scraper;
		String domain = null;
		try {
			if (!scrapeRequest.has("homepage") || !scrapeRequest.get("homepage").isTextual()) {
				throw new IllegalArgumentException("No homepage was received");
			}

			JsonNode homepageNode = scrapeRequest.get("homepage");
			domain = HtmlUtil.getDomainOfUrl(homepageNode.asText());

			if (scrapeRequest.has("continue")) {
				scraper = new Scraper(homepageNode.asText(),
						new ObjectId(scrapeRequest.get("continue").asText()));
			} else {
				scraper = new Scraper(homepageNode.asText());
			}

			ASYNC_TASK_EXECUTOR.execute(scraper);
			return Response.status(202).entity(scraper.getJob()).build();
		} catch (MalformedURLException | IllegalArgumentException e) {
			LOGGER.warn("Could not find a valid homepage url {}", request);
			return Response.status(400).build();
		} catch (JobActiveOnHost e) {
			LOGGER.warn("A job was active on the host before trying to start a new one");
			Job activeJob = CrawlDatabaseManager.instance.getActiveJobOnDomain(domain);
			String redirectUri = request.getRequestURI() + "/" + activeJob.getId().toString();
			return Response.status(409).header("Location", redirectUri).entity(activeJob).build();
		} catch (IOException e) {
			LOGGER.warn("Could not read a configuration file {}", e);
			return Response.status(500).build();
		}
	}
}
