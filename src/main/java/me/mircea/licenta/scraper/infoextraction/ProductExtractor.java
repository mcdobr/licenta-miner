package me.mircea.licenta.scraper.infoextraction;

import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.products.db.model.Product;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public interface ProductExtractor {
	Product extract(Document productPage);

	PricePoint extractPricePoint(Element productPage, Locale locale);

	Map<String, String> extractAttributes(Element productPage);

	String extractAvailability(Document productPage);

	String extractDescription(Document productPage);

	String extractImageUrl(Element htmlElement);

	Set<String> extractKeywords(String ...values);
}
