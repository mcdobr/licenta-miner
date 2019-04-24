package me.mircea.licenta.scraper.infoextraction;

import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SemanticBookExtractor implements BookExtractor {

	@Override
	public PricePoint extractPricePoint(Element productPage, Locale locale) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractAvailability(Document productPage) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractDescription(Document productPage) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Map<String, String> extractAttributes(Element productPage) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractPublisher(Element htmlElement, Map<String, String> attributes) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	@Override
	public String extractFormat(Element htmlElement, Map<String, String> attributes) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractIsbn(Element htmlElement, Map<String, String> attributes) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractAuthors(Element htmlElement, Map<String, String> attributes) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractImageUrl(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Set<String> extractKeywords(String... values) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractTitle(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
