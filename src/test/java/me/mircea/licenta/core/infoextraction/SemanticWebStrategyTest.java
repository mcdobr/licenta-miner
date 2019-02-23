package me.mircea.licenta.core.infoextraction;

import static org.junit.Assert.*;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import me.mircea.licenta.core.entities.Book;
import me.mircea.licenta.core.parser.utils.HtmlUtil;

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
		Document doc = HtmlUtil.sanitizeHtml(Jsoup.connect("https://www.bookdepository.com/category/2630/Romance/browse/viewmode/all").get());
		Element productElement = extractionStrategy.extractBookCards(doc).get(0);
		Book book = extractionStrategy.extractBook(productElement);
		
		assertNotNull(book);
		assertNotNull(book.getTitle());
		assertNotNull(book.getIsbn());
	}
}
