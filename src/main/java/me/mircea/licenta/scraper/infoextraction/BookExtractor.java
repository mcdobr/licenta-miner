package me.mircea.licenta.scraper.infoextraction;

import com.google.common.base.Preconditions;
import me.mircea.licenta.core.parser.utils.EntityNormalizer;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.Product;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public interface BookExtractor extends ProductExtractor {
    @Override
    default Product extract(Document productPage) {
        Preconditions.checkNotNull(productPage);

        // TODO: fix this hardcoding
        String language = "ro";
        Locale locale = Locale.forLanguageTag(language);

        Book book = new Book();
        book.setTitle(extractTitle(productPage));
        book.setImageUrl(extractImageUrl(productPage));
        book.setDescription(extractDescription(productPage));
        //book.setAvailability(extractAvailability(productPage));

        Map<String, String> attributes = extractAttributes(productPage);

        book.setAuthors(extractAuthors(productPage, attributes));
        book.setIsbn(extractIsbn(productPage, attributes));
        book.setFormat(extractFormat(productPage, attributes));
        book.setPublisher(extractPublisher(productPage, attributes));

        EntityNormalizer norm = new EntityNormalizer(locale);

        //book.setKeywords(extractKeywords());
        // TODO: refactor this
        Collection<String> relevantValuesForKeywords = new ArrayList<>(Arrays.asList(book.getTitle(),
                book.getAuthors(),
                book.getIsbn(),
                book.getPublisher(),
                book.getFormat()));

        book.setKeywords(norm.splitKeywords(relevantValuesForKeywords));
        return book;
    }


    String extractTitle(Element htmlElement);

    String extractAuthors(Element htmlElement, Map<String, String> attributes);

    String extractIsbn(Element htmlElement, Map<String, String> attributes);

    String extractFormat(Element htmlElement, Map<String, String> attributes);

    String extractPublisher(Element htmlElement, Map<String, String> attributes);
}
