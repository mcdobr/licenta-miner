package me.mircea.licenta.scraper.infoextraction;

import me.mircea.licenta.core.crawl.db.model.Wrapper;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WrapperBookExtractor implements BookExtractor {
	private static final Logger logger = LoggerFactory.getLogger(WrapperBookExtractor.class);
	private final Wrapper wrapper;

	public WrapperBookExtractor(Wrapper wrapper) {
		super();
		this.wrapper = wrapper;
	}

	//TODO: check this
	@Override
	public Book extract(Document productPage) {
		throw new UnsupportedOperationException("Not implemented yet");
		/*
		Preconditions.checkNotNull(productPage);

		Book book = new Book();
		if (wrapper.getTitleSelector() != null)
			book.setTitle(extractTitle(productPage));

		if (wrapper.getAttributeSelector() != null) {
			Map<String, String> attributes = extractAttributes(productPage);
		}

		book.setAuthors(extractAuthors(productPage));

		book.setDescription(extractDescription(productPage));

		return book;*/
	}

	@Override
	public String extractTitle(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
		//return htmlElement.selectFirst(wrapper.getTitleSelector()).text();
	}

	@Override
	public String extractAuthors(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
		/*
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
		}*/
	}

	@Override
	public String extractImageUrl(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
		/*
		Element imageUrlElement = htmlElement.selectFirst(wrapper.getImageLinkSelector());
		if (imageUrlElement.hasAttr("src"))
			return imageUrlElement.absUrl("src");
		else
			return imageUrlElement.attr("content");
			*/
	}

	@Override
	public Set<String> extractKeywords(String... values) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractIsbn(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractPublisher(Element htmlElement) {
		/*Map<String, String> attributes = extractAttributes(htmlElement);

		// Find any attribute that contains a word in publisherWordSet
		Optional<String> publisherAttribute = attributes.keySet().stream()
			.filter(key -> !Collections.disjoint(Arrays.asList(key.split(" ")),
												TextContentAnalyzer.publisherWordSet))
			.findFirst();
		
		if (publisherAttribute.isPresent())
			return attributes.get(publisherAttribute.get());
		else
			return null;*/
		throw new UnsupportedOperationException("Not implemented yet");
		
	}

	@Override
	public String extractFormat(Element htmlElement) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public PricePoint extractPricePoint(Element productPage, Locale locale) {
		/*
		String priceText = productPage.selectFirst(wrapper.getPriceSelector()).text();

		PricePoint pricePoint = null;
		try {
			pricePoint = PricePoint.valueOf(priceText, locale, Instant.now(), productPage.baseUri());
		} catch (ParseException e) {
			logger.warn("Price tag was ill-formated {}", priceText);
		} catch (MalformedURLException e) {
			logger.warn("Url was malformed {}", e);
		}
		return pricePoint;
		*/
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractAvailability(Document productPage) {
		/*
		throw new UnsupportedOperationException("Not implemented yet");
		*/
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public String extractDescription(Document productPage) {
		/*
		if (wrapper.getDescriptionSelector() != null)
			return productPage.selectFirst(wrapper.getDescriptionSelector()).text();
		else
			return null;*/
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Map<String, String> extractAttributes(Element productPage) {
		/*
		Elements specs = productPage.select(wrapper.getAttributeSelector());
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
		*/
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
