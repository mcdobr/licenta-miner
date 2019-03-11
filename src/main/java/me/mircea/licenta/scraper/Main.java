package me.mircea.licenta.scraper;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws IOException {
		ProductDatabaseManager dao = ProductDatabaseManager.instance;
		if (args.length > 0) {
			for (String seed: args) {
				Scraper scraper = new Scraper(seed);
				
				scraper.run();
			}
		}
	}
}
