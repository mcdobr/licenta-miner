package me.mircea.licenta.scraper.utils;

import me.mircea.licenta.products.db.model.Availability;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BookAttributesCoercer {
    public static final String SEPARATORS = " ";


    public final Set<String> titleWordSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    public final Set<String> authorWordSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    public final Set<String> priceWordSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    public final Set<String> codeWordSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    public final Map<String, String> formatsWordSet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public final Set<String> publisherWordSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    public final Set<String> descriptionWordSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Availability> availabilityWordSet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public BookAttributesCoercer() {
        this(Locale.forLanguageTag("ro-ro"));
    }

    public BookAttributesCoercer(Locale locale) {

        titleWordSet.add("titlu");
        titleWordSet.add("title");
        titleWordSet.add("nume");
        titleWordSet.add("name");

        authorWordSet.add("autor");
        authorWordSet.add("autori");
        authorWordSet.add("author");
        authorWordSet.add("authors");

        priceWordSet.add("pret");
        priceWordSet.add("price");

        codeWordSet.add("isbn");
        codeWordSet.add("cod");

        formatsWordSet.put("hardcover", "hardcover");
        formatsWordSet.put("paperback", "paperback");
        formatsWordSet.put("pdf", "pdf");
        formatsWordSet.put("epub", "epub");
        formatsWordSet.put("mobi", "mobi");
        formatsWordSet.put("audiobook", "audiobook");
        formatsWordSet.put("audiobooks", "audiobook");

        formatsWordSet.put("cartonata", "hardcover");
        formatsWordSet.put("necartonata", "paperback");

        publisherWordSet.add("publishing");
        publisherWordSet.add("publisher");
        publisherWordSet.add("editura");

        descriptionWordSet.add("descriere");
        descriptionWordSet.add("description");

        // TODO: add rest of item availabilities
        // discontinued (sale?)
        availabilityWordSet.put("indisponibil", Availability.DISCONTINUED);
        availabilityWordSet.put("discontinued", Availability.DISCONTINUED);

        // in stock
        availabilityWordSet.put("in stoc", Availability.IN_STOCK);
        availabilityWordSet.put("în stoc", Availability.IN_STOCK);
        availabilityWordSet.put("disponibil", Availability.IN_STOCK);
        availabilityWordSet.put("in_stock", Availability.IN_STOCK);
        availabilityWordSet.put("InStock", Availability.IN_STOCK);

        // limited availability
        availabilityWordSet.put("limitat", Availability.LIMITED_AVAILABILITY);
        availabilityWordSet.put("stoc limitat", Availability.LIMITED_AVAILABILITY);
        availabilityWordSet.put("limited", Availability.LIMITED_AVAILABILITY);
        availabilityWordSet.put("limited_availability", Availability.LIMITED_AVAILABILITY);
        availabilityWordSet.put("LimitedAvailability", Availability.LIMITED_AVAILABILITY);

        // out of stock
        availabilityWordSet.put("epuizat", Availability.OUT_OF_STOCK);
        availabilityWordSet.put("la comandă", Availability.OUT_OF_STOCK);
        availabilityWordSet.put("la comanda", Availability.OUT_OF_STOCK);
        availabilityWordSet.put("comandă", Availability.OUT_OF_STOCK);
        availabilityWordSet.put("comanda", Availability.OUT_OF_STOCK);
        availabilityWordSet.put("out_of_stock", Availability.OUT_OF_STOCK);
        availabilityWordSet.put("OutOfStock", Availability.OUT_OF_STOCK);

        // preorder
        availabilityWordSet.put("precomandă", Availability.PREORDER);
        availabilityWordSet.put("precomanda", Availability.PREORDER);
        availabilityWordSet.put("preoder", Availability.PREORDER);
    }


    public static Map<String, String> splitAttributes(Elements elements) {
        Map<String, String> attributes = new HashMap<>();

        for (Element element : elements) {
            String[] keyValuePair = element.text().split(":", 2);

            if (keyValuePair.length > 1) {
                attributes.put(keyValuePair[0].trim(), keyValuePair[1].trim());
            } else if (keyValuePair.length == 1) {
                attributes.put(keyValuePair[0].trim(), keyValuePair[0].trim());
            }
        }
        return attributes;
    }

    public Availability coerceAvailability(String str) {
        if (str != null) {
            return availabilityWordSet.getOrDefault(str.trim(), null);
        } else {
            return null;
        }
    }

    public String coerceFormat(Map<String, String> attributes) {
        String format = null;
        Optional<Map.Entry<String, String>> possibleFormatAttribute = attributes.entrySet().stream()
                .filter(entry -> !Collections.disjoint(formatsWordSet.keySet(), Arrays.asList(entry.getValue().split(" "))))
                .findFirst();
        if (possibleFormatAttribute.isPresent()) {
            Collection<String> words = Arrays.asList(possibleFormatAttribute.get().getValue().toLowerCase().split(" "));
            Optional<String> possibleFormatKeyword = words.stream().filter(formatsWordSet::containsKey).findFirst();
            if (possibleFormatKeyword.isPresent()) {
                format = formatsWordSet.get(possibleFormatKeyword.get());
            }

        }
        return format;
    }

    /**
     * Removes unecessary characters from an isbn
     */
    public String coerceIsbn(String isbn) {
        final String startLettersRegex = "^[ \\p{L}]*";
        final String endLettersRegex = "[ \\p{L}[^xX0-9]]$";

        return isbn.replaceAll(startLettersRegex, "")
                .replaceAll(endLettersRegex, "");
    }
}
