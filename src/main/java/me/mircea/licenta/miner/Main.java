package me.mircea.licenta.miner;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mircea.licenta.core.crawl.CrawlRequest;

public final class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws IOException {
		ProductDatabaseManager dao = ProductDatabaseManager.instance;
		if (args.length > 0) {
			for (String startUrl: args) {
				CrawlRequest request = new CrawlRequest(startUrl);
				Miner miner = new Miner(request);
				
				miner.run();
			}
		}
	}
}
