package me.mircea.licenta.scraper.infoextraction;

import com.google.common.base.Preconditions;
import me.mircea.licenta.core.parser.utils.CssUtil;
import me.mircea.licenta.core.parser.utils.EntityNormalizer;
import me.mircea.licenta.core.parser.utils.HtmlUtil;
import me.mircea.licenta.core.parser.utils.TextContentAnalyzer;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.products.db.model.WebWrapper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HeuristicalStrategy implements RuleBasedStrategy {
	private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicalStrategy.class);
	private static final String ISBN_PATTERN_STRING = "(?=[-\\d\\ xX]{10,})\\d+[-\\ ]?\\d+[-\\ ]?\\d+[-\\ ]?\\d*[-\\ ]?[\\dxX]";
	private static final Pattern ISBN_PATTERN = Pattern.compile(ISBN_PATTERN_STRING);
	private static final String IMAGE_WITH_LINK_SELECTOR = "[class*='produ']:has(img):has(a)";
	private static final String PRODUCT_CARD_SELECTOR = CssUtil.makeLeafOfSelector(IMAGE_WITH_LINK_SELECTOR);
	
	@Override
	public Elements extractBookCards(Document doc) {
		Preconditions.checkNotNull(doc);
		return doc.select(PRODUCT_CARD_SELECTOR);
	}

	@Override
	public Book extractBook(Document bookPage) {
		Preconditions.checkNotNull(bookPage);

		//String language = bookPage.selectFirst("html").attr("lang");
		String language = "ro";
		if (language == null)
			language = "ro";

		Locale locale = Locale.forLanguageTag(language);

		Book book = new Book();
		book.setTitle(extractTitle(bookPage));
		book.setImageUrl(extractImageUrl(bookPage));
		book.setDescription(extractDescription(bookPage));
		
		final Map<String, String> bookAttributes = extractAttributes(bookPage);
		book.setAuthors(extractAuthors(bookPage));
		book.setIsbn(extractIsbn(bookPage));
		book.setFormat(extractFormat(bookPage));
		book.setPublisher(extractPublisher(bookPage));


		EntityNormalizer norm = new EntityNormalizer(locale);
		Collection<String> relevantValuesForKeywords = new ArrayList<>(Arrays.asList(book.getTitle(),
				book.getAuthors(),
				book.getIsbn(),
				book.getPublisher(),
				book.getFormat()));

		book.setKeywords(norm.splitKeywords(relevantValuesForKeywords));
		return book;
	}
	
	@Override
	public String extractTitle(Element htmlElement) {
		Element ogTitle = htmlElement.selectFirst("meta[property='og:title']");
		if (ogTitle != null)
			return ogTitle.attr("content");
		
		Element title = htmlElement.selectFirst("title");
		if (title != null)
			return title.text();
		
		String cssSelector = CssUtil.makeClassOrIdContains(TextContentAnalyzer.titleWordSet);
		return htmlElement.select(cssSelector).first().text();
	}
	
	@Override
	public String extractAuthors(Element htmlElement) {
		Map<String, String> attributes = this.extractAttributes(htmlElement);
		Element authorElement = htmlElement.select(CssUtil.makeClassOrIdContains(TextContentAnalyzer.authorWordSet)).first();
		String text = null;
		if (authorElement != null) {
			text = authorElement.text();
		} else {	
			Optional<String> authorAttribute = attributes.keySet().stream()
				.filter(TextContentAnalyzer.authorWordSet::contains)
				.findFirst();
			if (authorAttribute.isPresent())
				text = attributes.get(authorAttribute.get());
		}
		
		return text;
	}
	
	@Override
	public String extractImageUrl(Element htmlElement) {
		Element ogImage = htmlElement.selectFirst("meta[property*='image']");
		if (ogImage != null)
			return ogImage.attr("content");
		
		Elements imagesWithAlt = htmlElement.select("img[alt]");
		imagesWithAlt.removeAll(htmlElement.select("img[alt='']"));
		
		if (!imagesWithAlt.isEmpty())
			return imagesWithAlt.first().absUrl("src");
		
		return null;
	}
	
	@Override
	public String extractIsbn(Element htmlElement) {
		Map<String, String> attributes = this.extractAttributes(htmlElement);
		String isbn = null;
		Optional<String> isbnAttribute = attributes.keySet()
				.stream()
				.filter(key -> !Collections.disjoint(TextContentAnalyzer.codeWordSet, Arrays.asList(key.split("[\\s|,.;:]"))))
				.findFirst();
		if (isbnAttribute.isPresent())
			isbn = attributes.get(isbnAttribute.get()).replaceAll("^[ a-zA-Z]*", "");
		
		return isbn;
	}
	
	@Override
	public String extractFormat(Element htmlElement) {
		Map<String, String> attributes = this.extractAttributes(htmlElement);
		String format = null;
		
		Optional<Map.Entry<String, String>> formatAttribute = attributes.entrySet().stream()
			.filter(entry -> TextContentAnalyzer.formatsWordSet.containsKey(entry.getValue()))
			.findFirst();
		
		if (formatAttribute.isPresent())
			format = TextContentAnalyzer.formatsWordSet.get(formatAttribute.get().getValue());
		
		return format;
	}
	
	@Override
	public String extractPublisher(Element htmlElement) {
		Map<String, String> attributes = this.extractAttributes(htmlElement);
		String publisher = null;
		
		Optional<Map.Entry<String, String>> publisherAttribute = attributes.entrySet()
				.stream()
				.filter(entry -> !Collections.disjoint(Arrays.asList(entry.getKey().split(TextContentAnalyzer.SEPARATORS)), TextContentAnalyzer.publisherWordSet))
				.findFirst();
		
		if (publisherAttribute.isPresent())
			publisher = publisherAttribute.get().getValue();
		
		return publisher;
	}
	
	@Override
	public PricePoint extractPricePoint(Element htmlElement, Locale locale) {
		Preconditions.checkNotNull(htmlElement);

		PricePoint pricePoint = null;
		try {
			// TODO: be locale sensitive
			Element priceElement = htmlElement.selectFirst(":matchesOwn((,|.)[0-9]{2} lei)");
			if (priceElement != null) {
				String priceTag = priceElement.text().replaceAll(".*:", "").trim();
				String url = HtmlUtil.getCanonicalUrl(htmlElement).orElse(htmlElement.baseUri());
				pricePoint = PricePoint.valueOf(priceTag, locale, Instant.now(), url);
				if (htmlElement instanceof Document)
					pricePoint.setPageTitle(((Document) htmlElement).title());
			}
		} catch (ParseException e) {
			LOGGER.warn("Price tag was ill-formated {}, which resulted in {}", e);
		} catch (MalformedURLException e) {
			LOGGER.warn("Url was malformed {}", e);
		}

		return pricePoint;
	}

	@Override
	public String extractDescription(Document bookPage) {
		Preconditions.checkNotNull(bookPage);
		Element descriptionElement = bookPage.select("[class*='descri']").first();
		return (descriptionElement != null) ? descriptionElement.text() : null;
	}

	@Override
	public Map<String, String> extractAttributes(Element bookPage) {
		Preconditions.checkNotNull(bookPage);

		Map<String, String> bookAttributes = new TreeMap<>();
		Matcher isbnMatcher = ISBN_PATTERN.matcher(bookPage.text());
		while (isbnMatcher.find()) {
			String matchedText = isbnMatcher.group();
			String isbn = matchedText.replaceAll("[\\s-]", "");
			if (isbn.length() == 10 || isbn.length() == 13) {
				LOGGER.debug("Found isbn {}", isbn);

				String findIsbnElement = String.format("*:contains(%s)", matchedText);
				Element isbnElement = bookPage.select(findIsbnElement).last();

				if (isbnElement.text().trim().equals(isbn))
					isbnElement = isbnElement.parent();

				Elements keyValuePairs = isbnElement.siblingElements();
				keyValuePairs.add(isbnElement);
				for (Element element : keyValuePairs) {
					String[] keyValuePair = element.text().split(":", 2);

					if (keyValuePair.length > 1) {
						bookAttributes.put(keyValuePair[0].trim(), keyValuePair[1].trim());
					} else if (keyValuePair.length == 1) {
						bookAttributes.put(keyValuePair[0].trim(), keyValuePair[0].trim());
					}
				}

				break;
			}
		}

		return bookAttributes;
	}
	//TODO: refactor this
	@Override
	public WebWrapper generateWrapper(Element bookPage, Elements additionals) {
		WebWrapper wrapper = new WebWrapper();

		String titleSelector = CssUtil.makeClassOrIdContains(TextContentAnalyzer.titleWordSet);
		Element titleElement = bookPage.select(titleSelector).select(String.format(":not(:has(%s))", titleSelector))
				.first();
		if (titleElement != null)
			wrapper.setTitleSelector(generateCssSelectorFor(new Elements(titleElement)));

		String authorsSelector = CssUtil.makeClassOrIdContains(TextContentAnalyzer.authorWordSet);
		Element authorsElement = bookPage.select(authorsSelector).first();
		if (authorsElement != null)
			wrapper.setAuthorsSelector(generateCssSelectorFor(new Elements(authorsElement)));
		
		String priceSelector = CssUtil.makeClassOrIdContains(TextContentAnalyzer.priceWordSet);
		Element priceElement = bookPage.selectFirst(priceSelector);
		//TODO: handle currency
		String priceRegexSelector = ":matchesOwn(lei)";
		Element priceRegexElement = bookPage.selectFirst(priceRegexSelector);
		if (!priceRegexElement.equals(priceElement)) {
			String priceHtml = priceElement.html();
			String priceRegexHtml = priceRegexElement.html();

			String str = bookPage.outerHtml();
			
			int indexOfPrice = str.indexOf(priceHtml);
			int indexOfRegex = str.indexOf(priceRegexHtml);
			
			//TODO: this may break
			priceElement = (indexOfPrice <= indexOfRegex) ? priceElement : priceRegexElement;
		}
		
		if (priceElement != null)
			wrapper.setPriceSelector(generateCssSelectorFor(new Elements(priceElement)));

		String isbnSelector = ":matchesOwn((?=[-\\d\\ xX]{10,})\\d+[-\\ ]?\\d+[-\\ ]?\\d+[-\\ ]?\\d*[-\\ ]?[\\dxX])";
		Element isbnElement = bookPage.select(isbnSelector).first();
		Element parent = isbnElement.parent();

		// TODO: refine this: If the book code element has no class then those are
		// probably the whole thing
		Elements attributeElements = null;
		if (isbnElement.className().isEmpty()) {
			attributeElements = isbnElement.siblingElements();
			attributeElements.add(isbnElement);
		} else {
			attributeElements = parent.siblingElements();
			attributeElements.add(parent);
		}

		wrapper.setAttributeSelector(generateCssSelectorFor(attributeElements));
		wrapper.setImageLinkSelector("img[alt]:not(img[alt='']),meta[property*='image']");

		String descriptionSelector = CssUtil.makeClassOrIdContains(TextContentAnalyzer.descriptionWordSet);
		Element descriptionElement = bookPage.select(descriptionSelector).first();
		if (descriptionElement != null)
			wrapper.setDescriptionSelector(generateCssSelectorFor(new Elements(descriptionElement)));

		if (!additionals.isEmpty()) {
			Elements bookCards = new Elements();
			for (Element element : additionals) {
				bookCards.addAll(element.select(PRODUCT_CARD_SELECTOR));
			}
			wrapper.setBookCardSelector(generateCssSelectorFor(bookCards));
		}

		return wrapper;
	}

	//TODO: refactor this
	@Override
	public String generateCssSelectorFor(Elements elements) {
		Preconditions.checkNotNull(elements);
		Preconditions.checkArgument(!elements.isEmpty());

		String selector = null;
		if (elements.size() == 1) {
			Element elem = elements.first();

			if (!elem.id().isEmpty())
				selector = "#" + elem.id();
			else if (!elem.className().isEmpty())
				selector = "." + String.join(".", elem.classNames());
			else
				selector = generateCssSelectorFor(new Elements(elem.parent())) + ">" + elem.tagName();
		} else {
			// Get the mode of class name
			final Map<String, Long> classNameFrequencies = elements.stream().map(Element::className)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			final long maxFrequency = classNameFrequencies.values().stream().max(Long::compare).orElse(0L);
			final List<String> classModes = classNameFrequencies.entrySet().stream()
					.filter(tuple -> tuple.getValue() == maxFrequency).map(Map.Entry::getKey)
					.collect(Collectors.toList());

			if (!classModes.get(0).isEmpty()) {
				selector = "." + classModes.get(0);
			} else {
				// Gets mode of tag name
				final Map<String, Long> tagNameFrequencies = elements.stream().map(Element::tagName)
						.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
				final long maxTagFrequency = tagNameFrequencies.values()
						.stream()
						.max(Long::compare)
						.orElse(0L);
				final List<String> tagModes = tagNameFrequencies.entrySet().stream()
						.filter(tuple -> tuple.getValue() == maxTagFrequency)
						.map(Map.Entry::getKey)
						.collect(Collectors.toList());
				String tag = tagModes.get(0);

				LOGGER.info("Tagname frequencies: {}", tagNameFrequencies);

				// TODO: handle case when not all are the same, handle case when tag is not unique to the site
				// Also this is probably breakable
				Element parent = elements.first().parent();
				if (!parent.id().isEmpty())
					selector = generateCssSelectorFor(new Elements(parent)) + ">" + tag;
				else
					selector = generateCssSelectorFor(new Elements(parent));
			}
		}
		
		return selector;
	}
}
