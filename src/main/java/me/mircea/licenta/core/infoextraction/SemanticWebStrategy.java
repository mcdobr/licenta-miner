package me.mircea.licenta.core.infoextraction;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Preconditions;

import me.mircea.licenta.core.entities.PricePoint;
import me.mircea.licenta.core.entities.Book;

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
	public PricePoint extractPricePoint(Element htmlElement, Locale locale, Instant retrievedDay) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractDescription(Document productPage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> extractAttributes(Element bookPage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractPublisher(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String extractFormat(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractIsbn(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractAuthors(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractImageUrl(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractTitle(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}
}
