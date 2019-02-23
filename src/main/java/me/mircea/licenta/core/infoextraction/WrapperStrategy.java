package me.mircea.licenta.core.infoextraction;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import me.mircea.licenta.core.entities.Book;
import me.mircea.licenta.core.entities.PricePoint;
import me.mircea.licenta.core.entities.WebWrapper;
import me.mircea.licenta.core.parser.utils.TextContentAnalyzer;

public class WrapperStrategy implements InformationExtractionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(WrapperStrategy.class);

	private WebWrapper wrapper;

	public WrapperStrategy(WebWrapper wrapper) {
		super();
		this.wrapper = wrapper;
	}

	@Override
	public Elements extractBookCards(Document doc) {
		return doc.select(wrapper.getBookCardSelector());
	}

	@Override
	public Book extractBook(Element htmlElement, Document bookPage) {
		Preconditions.checkNotNull(bookPage);

		Book book = new Book();
		if (wrapper.getTitleSelector() != null)
			book.setTitle(extractTitle(bookPage));

		if (wrapper.getAttributeSelector() != null) {
			Map<String, String> attributes = extractAttributes(bookPage);
		}

		book.setAuthors(extractAuthors(bookPage));

		book.setDescription(extractDescription(bookPage));

		return book;
	}

	@Override
	public String extractTitle(Element htmlElement) {
		return htmlElement.selectFirst(wrapper.getTitleSelector()).text();
	}

	@Override
	public String extractAuthors(Element htmlElement) {
		if (wrapper.getAuthorsSelector() != null && !wrapper.getAuthorsSelector().isEmpty()) {
			return String.join(",", htmlElement.select(wrapper.getAuthorsSelector()).eachText());
		} else {
			Map<String, String> attributes = extractAttributes(htmlElement);
			Optional<String> authorAttribute = attributes.keySet().stream()
					.filter(TextContentAnalyzer.authorWordSet::contains)
					.findFirst();
			
			if (authorAttribute.isPresent())
				return attributes.get(authorAttribute.get());
			else
				return null;
		}
	}

	@Override
	public String extractImageUrl(Element htmlElement) {
		Element imageUrlElement = htmlElement.selectFirst(wrapper.getImageLinkSelector());
		if (imageUrlElement.hasAttr("src"))
			return imageUrlElement.absUrl("src");
		else
			return imageUrlElement.attr("content");
	}

	@Override
	public String extractIsbn(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractPublisher(Element htmlElement) {
		Map<String, String> attributes = extractAttributes(htmlElement);

		// Find any attribute that contains a word in publisherWordSet
		Optional<String> publisherAttribute = attributes.keySet().stream()
			.filter(key -> !Collections.disjoint(Arrays.asList(key.split(" ")),
												TextContentAnalyzer.publisherWordSet))
			.findFirst();
		
		if (publisherAttribute.isPresent())
			return attributes.get(publisherAttribute.get());
		else
			return null;
		
	}

	@Override
	public String extractFormat(Element htmlElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PricePoint extractPricePoint(Element htmlElement, Locale locale, Instant retrievedTime) {
		String priceText = htmlElement.selectFirst(wrapper.getPriceSelector()).text();

		PricePoint pricePoint = null;
		try {
			pricePoint = PricePoint.valueOf(priceText, locale, retrievedTime, htmlElement.baseUri());
		} catch (ParseException e) {
			logger.warn("Price tag was ill-formated {}", priceText);
		} catch (MalformedURLException e) {
			logger.warn("Url was malformed {}", e);
		}
		return pricePoint;
	}

	@Override
	public String extractDescription(Document bookPage) {
		if (wrapper.getDescriptionSelector() != null)
			return bookPage.selectFirst(wrapper.getDescriptionSelector()).text();
		else
			return null;
	}

	@Override
	public Map<String, String> extractAttributes(Element bookPage) {
		Elements specs = bookPage.select(wrapper.getAttributeSelector());
		Map<String, String> attributes = new TreeMap<>();

		Elements normalizedSpecs = new Elements();
		for (Element spec : specs) {
			if (spec.tagName().equals("ul"))
				normalizedSpecs.addAll(spec.children());
			else
				normalizedSpecs.add(spec);
		}
		
		for (Element spec : normalizedSpecs) {
			
			String[] keyValuePair = spec.text().split(":", 2);
			if (keyValuePair.length > 1)
				attributes.put(keyValuePair[0].trim(), keyValuePair[1].trim());
			else
				attributes.put(keyValuePair[0], keyValuePair[0]);
		}

		return attributes;
	}
}
