package me.mircea.licenta.scraper.infoextraction;

import me.mircea.licenta.core.crawl.db.model.Selector;
import me.mircea.licenta.core.crawl.db.model.Wrapper;
import me.mircea.licenta.scraper.utils.TextValueCoercer;
import me.mircea.licenta.products.db.model.Availability;
import me.mircea.licenta.products.db.model.PricePoint;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.*;

public class WrapperBookExtractor implements BookExtractor {
	private static final Logger LOGGER = LoggerFactory.getLogger(WrapperBookExtractor.class);
	private final Wrapper wrapper;
	private final TextValueCoercer coercer;

	public WrapperBookExtractor(Wrapper wrapper) {
		super();
		this.wrapper = wrapper;
		this.coercer = new TextValueCoercer();
	}

	@Override
	public String extractTitle(Element htmlElement) {
		return extractSingleValueWithSelectorName(htmlElement, "title");
	}

	// TODO: fix coercion duplication
	@Override
	public String extractAuthors(Element htmlElement, Map<String, String> attributes) {
		String authors = null;
		Optional<Selector> possibleSelector = wrapper.getSelectorByName("authors");
		if (possibleSelector.isPresent()) {
			Selector selector = possibleSelector.get();
			Element element = htmlElement.selectFirst(selector.getQuery());
			if (element != null)
				authors = element.text();
		} else {
			Optional<String> authorAttribute = attributes.keySet().stream()
					.filter(this.coercer.authorWordSet::contains)
					.findFirst();
			if (authorAttribute.isPresent())
				authors = attributes.get(authorAttribute.get());
		}

		return authors;
	}

	// TODO: fix coercion duplication
	@Override
	public String extractIsbn(Element htmlElement, Map<String, String> attributes) {
		String isbn = null;
		Optional<Selector> possibleSelector = wrapper.getSelectorByName("isbn");
		if (possibleSelector.isPresent()) {
			Selector selector = possibleSelector.get();
			Element isbnElement = htmlElement.selectFirst(selector.getQuery());
			if (isbnElement != null)
				isbn = isbnElement.text();
		} else {
			Optional<String> isbnAttribute = attributes.keySet()
					.stream()
					.filter(key -> !Collections.disjoint(this.coercer.codeWordSet, Arrays.asList(key.split("[\\s|,.;:]"))))
					.findFirst();
			if (isbnAttribute.isPresent())
				isbn = attributes.get(isbnAttribute.get()).replaceAll("^[ a-zA-Z]*", "");
		}
		return isbn;
	}

	// TODO: fix coercion duplication
	@Override
	public String extractFormat(Element htmlElement, Map<String, String> attributes) {
		String format = null;
		Optional<Selector> possibleSelector = wrapper.getSelectorByName("format");
		if (possibleSelector.isPresent()) {
			//TODO: finish this
		} else {
			Optional<Map.Entry<String, String>> formatAttribute = attributes.entrySet().stream()
					.filter(entry -> this.coercer.formatsWordSet.containsKey(entry.getValue()))
					.findFirst();
			if (formatAttribute.isPresent())
				format = this.coercer.formatsWordSet.get(formatAttribute.get().getValue());
		}
		return format;

	}

	@Override
	public String extractPublisher(Element htmlElement, Map<String, String> attributes) {
		String publisher = null;
		Optional<Selector> possibleSelector = wrapper.getSelectorByName("format");
		if (possibleSelector.isPresent()) {
			//TODO: finish this
		} else {
			Optional<Map.Entry<String, String>> publisherAttribute = attributes.entrySet()
					.stream()
					.filter(entry -> !Collections.disjoint(Arrays.asList(entry.getKey().split(TextValueCoercer.SEPARATORS)), this.coercer.publisherWordSet))
					.findFirst();

			if (publisherAttribute.isPresent())
				publisher = publisherAttribute.get().getValue();
		}
		return publisher;
	}

	@Override
	public PricePoint extractPricePoint(Element productPage, Locale locale) {
		PricePoint price = null;
		Optional<Selector> possibleSelector = wrapper.getSelectorByName("pricepoint");
		if (possibleSelector.isPresent()) {
			Selector selector = possibleSelector.get();
			Element priceTagElement = productPage.selectFirst(selector.getQuery());

			if (priceTagElement != null) {
				String priceTag = priceTagElement.text();

				try {
					price = PricePoint.valueOf(priceTag, locale, productPage);

					Availability availability = coercer.coerceAvailability(extractAvailability((Document) productPage));
					price.setAvailability(availability);
				} catch (ParseException e) {
					LOGGER.warn("Price tag was ill-formated {}, which resulted in {}", e);
				} catch (MalformedURLException e) {
					LOGGER.warn("Url was malformed {}", e);
				}
			}
		}

		return price;
	}

	@Override
	public Map<String, String> extractAttributes(Element productPage) {
		Map<String, String> attributes = new HashMap<>();
		Optional<Selector> possibleSelector = wrapper.getSelectorByName("attributes");
		if (possibleSelector.isPresent()) {
			Selector selector = possibleSelector.get();
			Elements attributeElements = productPage.select(selector.getQuery());

			attributes = TextValueCoercer.splitAttributes(attributeElements);
		}

		return attributes;
	}

	@Override
	public String extractAvailability(Document productPage) {
		return extractSingleValueWithSelectorName(productPage, "availability");
	}

	@Override
	public String extractDescription(Document productPage) {
		return extractSingleValueWithSelectorName(productPage, "description");
	}

	@Override
	public String extractImageUrl(Element htmlElement) {
		return extractSingleValueWithSelectorName(htmlElement, "image");
	}

	@Override
	public Set<String> extractKeywords(String... values) {
		throw new UnsupportedOperationException("Not implemented yet");
	}


	private String extractSingleValueWithSelectorName(Element htmlElement, String selectorName) {
		String result = null;
		Optional<Selector> possibleSelector = wrapper.getSelectorByName(selectorName);
		if (possibleSelector.isPresent()) {
			Selector selector = possibleSelector.get();
			Element element = htmlElement.selectFirst(selector.getQuery());

			if (element != null) {
				switch (selector.getType()) {
					case TEXT:
						result = element.text();
						break;
					case LINK:
						result = element.absUrl("href");
						break;
					case IMAGE:
						result = element.absUrl("src");
						break;
					case ATTRIBUTE:
						result = element.attr(selector.getTarget());
						break;
				}
			}
		}
		return result;
	}
}
