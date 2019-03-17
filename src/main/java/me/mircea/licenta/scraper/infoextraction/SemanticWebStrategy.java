package me.mircea.licenta.scraper.infoextraction;

import java.util.Locale;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Preconditions;

import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.products.db.model.Book;

public class SemanticWebStrategy implements InformationExtractionStrategy {

	@Override
	public Elements extractBookCards(Document doc) {
		Preconditions.checkNotNull(doc);
		return doc.select("[itemtype$='Book'],[itemtype$='Book']");
	}

	@Override
	public Book extractBook(Element htmlElement, Document productPage) {
		Elements propElements = htmlElement.select("[itemprop]");
		
		Book book = new Book();
		for (Element item : propElements) {
			String property = item.attr("itemprop");
			String content = item.attr("content");
			
			switch (property.toLowerCase()) {
			case "name": case "title":
				book.setTitle(content);
				break;
			case "author":
				book.setAuthors(content);
				break;
			case "isbn":
				book.setIsbn(content);
				break;
			default:
				break;
			}
		}
		
		Element imgElement = htmlElement.select("img[src]").first();
		if (imgElement != null)
			book.setImageUrl(imgElement.absUrl("src"));
		
		return book;
	}
	

	@Override
	public Book extractBook(Document bookPage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PricePoint extractPricePoint(Element htmlElement, Locale locale) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractDescription(Document productPage) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Map<String, String> extractAttributes(Element bookPage) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractPublisher(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	@Override
	public String extractFormat(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractIsbn(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractAuthors(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractImageUrl(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractTitle(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
