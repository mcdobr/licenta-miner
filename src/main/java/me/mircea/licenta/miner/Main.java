package me.mircea.licenta.miner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mircea.licenta.core.crawl.CrawlRequest;

public final class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	//public static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			for (String startUrl: args) {
				CrawlRequest request = new CrawlRequest(startUrl);
				Miner miner = new Miner(request);
				
				
			}
		}
		
		/*
		for (String startUrl : seedList) {
			try {
				executor.execute(new BrowserIndexer(new CrawlRequest(startUrl)));
			} catch (MalformedURLException e) {
				logger.debug("Problem regarding gathering pages: {}.", e.getMessage());
			} catch (FileNotFoundException | NullPointerException e) {
				logger.error("Properties file not found. Exception details: {}", e);
			} catch (IOException e) {
				logger.warn("Could not read from an input stream. Exception details: {}", e);
			}
		}
		
		if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
			executor.shutdownNow();
			if (!executor.awaitTermination(1, TimeUnit.SECONDS))
			{
				logger.info("Exiting because of timeout: {}", executor);
				System.exit(0);
			}
			logger.info("Exiting normally: {}", executor);
		}
		*/
	}
}
