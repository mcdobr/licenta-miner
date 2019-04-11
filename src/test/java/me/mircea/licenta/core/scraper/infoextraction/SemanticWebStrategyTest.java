package me.mircea.licenta.core.scraper.infoextraction;

import me.mircea.licenta.scraper.infoextraction.BookExtractor;
import me.mircea.licenta.scraper.infoextraction.SemanticBookExtractor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SemanticWebStrategyTest {
	BookExtractor extractionStrategy = new SemanticBookExtractor();
	
	@Test
	public void shoudlExtractAttributes() throws IOException {
		fail();
	}
}
