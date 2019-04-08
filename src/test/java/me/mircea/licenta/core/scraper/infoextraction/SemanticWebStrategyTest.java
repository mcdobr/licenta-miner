package me.mircea.licenta.core.scraper.infoextraction;

import me.mircea.licenta.core.parser.utils.HtmlUtil;
import me.mircea.licenta.scraper.infoextraction.InformationExtractionStrategy;
import me.mircea.licenta.scraper.infoextraction.SemanticWebStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SemanticWebStrategyTest {
	InformationExtractionStrategy extractionStrategy = new SemanticWebStrategy();
	
	@Test
	public void shouldExtractElementsFromDownloadedMultiPage() throws IOException {
		Document doc = HtmlUtil.sanitizeHtml(Jsoup.connect("https://www.bookdepository.com/category/2630/Romance/browse/viewmode/all").get());
		
		Elements bookElements = extractionStrategy.extractBookCards(doc);
		assertNotNull(bookElements);
		assertEquals(30, bookElements.size());
	}
	
	@Test
	public void shoudlExtractAttributes() throws IOException {
		fail();
	}
}
