package me.mircea.licenta.scraper.infoextraction;

import com.google.common.base.Preconditions;
import me.mircea.licenta.core.crawl.db.model.Wrapper;
import me.mircea.licenta.core.parser.utils.CssUtil;
import me.mircea.licenta.core.parser.utils.HtmlUtil;
import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.scraper.utils.BookAttributesCoercer;
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

public class HeuristicalBookExtractor implements BookExtractor, WrapperGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicalBookExtractor.class);
    private static final String ISBN_PATTERN_STRING = "(?=[-\\d\\ xX]{10,})\\d+[-\\ ]?\\d+[-\\ ]?\\d+[-\\ ]?\\d*[-\\ ]?[\\dxX]";
    private static final Pattern ISBN_PATTERN = Pattern.compile(ISBN_PATTERN_STRING);

    private BookAttributesCoercer coercer;

    public HeuristicalBookExtractor() {
        this.coercer = new BookAttributesCoercer();
    }


    @Override
    public String extractTitle(Element htmlElement) {
        Element ogTitle = htmlElement.selectFirst("meta[property='og:title']");
        if (ogTitle != null)
            return ogTitle.attr("content");

        Element title = htmlElement.selectFirst("title");
        if (title != null)
            return title.text();

        String cssSelector = CssUtil.makeClassOrIdContains(this.coercer.titleWordSet);
        return htmlElement.select(cssSelector).first().text();
    }

    @Override
    public String extractAuthors(Element htmlElement, Map<String, String> attributes) {
        Element authorElement = htmlElement.select(CssUtil.makeClassOrIdContains(this.coercer.authorWordSet)).first();
        String text = null;
        if (authorElement != null) {
            text = authorElement.text();
        } else {
            BookAttributesCoercer coercer = new BookAttributesCoercer();

            Optional<String> authorAttribute = attributes.keySet().stream()
                    .filter(this.coercer.authorWordSet::contains)
                    .findFirst();
            if (authorAttribute.isPresent())
                text = attributes.get(authorAttribute.get());
        }

        return text;
    }

    @Override
    public String extractIsbn(Element htmlElement, Map<String, String> attributes) {
        String isbn = null;
        Optional<String> isbnAttribute = attributes.keySet()
                .stream()
                .filter(key -> !Collections.disjoint(this.coercer.codeWordSet, Arrays.asList(key.split("[\\s|,.;:]"))))
                .findFirst();
        if (isbnAttribute.isPresent())
            isbn = coercer.coerceIsbn(attributes.get(isbnAttribute.get()));

        return isbn;
    }

    @Override
    public String extractFormat(Element htmlElement, Map<String, String> attributes) {
        return coercer.coerceFormat(attributes);
    }

    @Override
    public String extractPublisher(Element htmlElement, Map<String, String> attributes) {
        String publisher = null;

        Optional<Map.Entry<String, String>> publisherAttribute = attributes.entrySet()
                .stream()
                .filter(entry -> !Collections.disjoint(Arrays.asList(entry.getKey().split(BookAttributesCoercer.SEPARATORS)), this.coercer.publisherWordSet))
                .findFirst();

        if (publisherAttribute.isPresent())
            publisher = publisherAttribute.get().getValue();

        return publisher;
    }

    @Override
    public PricePoint extractPricePoint(Element productPage, Locale locale) {
        Preconditions.checkNotNull(productPage);

        PricePoint pricePoint = null;
        try {
            // TODO: be locale sensitive
            Element priceElement = productPage.selectFirst(":matchesOwn((,|.)[0-9]{2} lei)," + CssUtil.makeClassOrIdContains(this.coercer.priceWordSet));
            if (priceElement != null) {
                String priceTag = priceElement.text().replaceAll(".*:", "").trim();
                String url = HtmlUtil.getCanonicalUrl(productPage).orElse(productPage.baseUri());
                pricePoint = PricePoint.valueOf(priceTag, locale, Instant.now(), url);
                if (productPage instanceof Document)
                    pricePoint.setPageTitle(((Document) productPage).title());
            }
        } catch (ParseException e) {
            LOGGER.warn("Price tag was ill-formated {}, which resulted in {}", e);
        } catch (MalformedURLException e) {
            LOGGER.warn("Url was malformed {}", e);
        }

        return pricePoint;
    }

    @Override
    public String extractAvailability(Document productPage) {
        Element stockElement = productPage.selectFirst("[class*='stoc'],[id*='stoc']");
        if (stockElement == null)
            return null;

        String text = stockElement.text().toLowerCase();
        if (text.contains("in stoc") || text.contains("în stoc") || text.contains("limitat")) {
            return "available";
        } else if (text.contains("indisponibil") || text.contains("comanda")) {
            return "unavailable";
        } else {
            return null;
        }
    }

    @Override
    public String extractDescription(Document productPage) {
        Preconditions.checkNotNull(productPage);
        Element descriptionElement = productPage.select("[class*='descri']").first();
        return (descriptionElement != null) ? descriptionElement.text() : null;
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
    public Set<String> extractKeywords(String... values) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Map<String, String> extractAttributes(Element productPage) {
        Preconditions.checkNotNull(productPage);

        Map<String, String> bookAttributes = new TreeMap<>();
        Matcher isbnMatcher = ISBN_PATTERN.matcher(productPage.text());
        while (isbnMatcher.find()) {
            String matchedText = isbnMatcher.group();
            String isbn = matchedText.replaceAll("[\\s-]", "");
            if (isbn.length() == 10 || isbn.length() == 13) {
                LOGGER.debug("Found isbn {}", isbn);

                String findIsbnElement = String.format("*:contains(%s)", matchedText);
                Element isbnElement = productPage.select(findIsbnElement).last();

                if (isbnElement.text().trim().equals(isbn))
                    isbnElement = isbnElement.parent();

                Elements keyValuePairs = isbnElement.siblingElements();
                keyValuePairs.add(isbnElement);

                bookAttributes = BookAttributesCoercer.splitAttributes(keyValuePairs);

                break;
            }
        }

        return bookAttributes;
    }


    //TODO: refactor this
    @Override
    public Wrapper generateWrapper(Element productPage, Elements additionals) {
        throw new UnsupportedOperationException("not implemented yet");
		/*
		WebWrapper wrapper = new WebWrapper();

		String titleSelector = CssUtil.makeClassOrIdContains(BookAttributesCoercer.titleWordSet);
		Element titleElement = productPage.select(titleSelector).select(String.format(":not(:has(%s))", titleSelector))
				.first();
		if (titleElement != null)
			wrapper.setTitleSelector(generateCssSelectorFor(new Elements(titleElement)));

		String authorsSelector = CssUtil.makeClassOrIdContains(BookAttributesCoercer.authorWordSet);
		Element authorsElement = productPage.select(authorsSelector).first();
		if (authorsElement != null)
			wrapper.setAuthorsSelector(generateCssSelectorFor(new Elements(authorsElement)));
		
		String priceSelector = CssUtil.makeClassOrIdContains(BookAttributesCoercer.priceWordSet);
		Element priceElement = productPage.selectFirst(priceSelector);
		//TODO: handle currency
		String priceRegexSelector = ":matchesOwn(lei)";
		Element priceRegexElement = productPage.selectFirst(priceRegexSelector);
		if (!priceRegexElement.equals(priceElement)) {
			String priceHtml = priceElement.html();
			String priceRegexHtml = priceRegexElement.html();

			String str = productPage.outerHtml();
			
			int indexOfPrice = str.indexOf(priceHtml);
			int indexOfRegex = str.indexOf(priceRegexHtml);
			
			//TODO: this may break
			priceElement = (indexOfPrice <= indexOfRegex) ? priceElement : priceRegexElement;
		}
		
		if (priceElement != null)
			wrapper.setPriceSelector(generateCssSelectorFor(new Elements(priceElement)));

		String isbnSelector = ":matchesOwn((?=[-\\d\\ xX]{10,})\\d+[-\\ ]?\\d+[-\\ ]?\\d+[-\\ ]?\\d*[-\\ ]?[\\dxX])";
		Element isbnElement = productPage.select(isbnSelector).first();
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

		String descriptionSelector = CssUtil.makeClassOrIdContains(BookAttributesCoercer.descriptionWordSet);
		Element descriptionElement = productPage.select(descriptionSelector).first();
		if (descriptionElement != null)
			wrapper.setDescriptionSelector(generateCssSelectorFor(new Elements(descriptionElement)));

		if (!additionals.isEmpty()) {
			Elements bookCards = new Elements();
			for (Element element : additionals) {
				bookCards.addAll(element.select(PRODUCT_CARD_SELECTOR));
			}
			wrapper.setBookCardSelector(generateCssSelectorFor(bookCards));
		}

		return wrapper;*/
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
