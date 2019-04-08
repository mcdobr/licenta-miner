package me.mircea.licenta.scraper.infoextraction;

import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Locale;
import java.util.Map;

/**
 * @author mircea
 */
public interface InformationExtractionStrategy {
	Elements extractBookCards(Document multiBookPage);

	Book extractBook(Document bookPage);

	String extractTitle(Element htmlElement);
	
	String extractAuthors(Element htmlElement);
	
	String extractImageUrl(Element htmlElement);
	
	String extractIsbn(Element htmlElement);
	
	String extractFormat(Element htmlElement);
	
	String extractPublisher(Element htmlElement);

	/*
	default Set<String> extractKeywords(String ... values) {

	}*/

	PricePoint extractPricePoint(Element bookCard, Locale locale);

	String extractDescription(Document bookPage);

	Map<String, String> extractAttributes(Element bookPage);
	
}
