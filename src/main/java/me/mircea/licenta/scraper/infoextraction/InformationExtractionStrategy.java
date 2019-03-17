package me.mircea.licenta.scraper.infoextraction;

import java.util.Locale;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.products.db.model.Book;

/**
 * @author mircea
 */
public interface InformationExtractionStrategy {
	/**
	 * @brief Select all the product cards on a multiproduct page.
	 * @param doc
	 * @return
	 * @throws NullPointerException
	 *             if the passed document is null.
	 */
	public Elements extractBookCards(Document multiBookPage);

	/**
	 * @brief Extracts a product from the given book card.
	 * @param bookCard
	 * @param retrievedTime
	 * @param site
	 * @return HTML elements that contain products.
	 * @throws NullPointerException
	 *             if the passed document is null.
	 */
	public Book extractBook(Element bookCard, Document bookPage);
	
	public default Book extractBook(Element bookCard) {
		return extractBook(bookCard, null);
	}
	
	public Book extractBook(Document bookPage);
	

	public String extractTitle(Element htmlElement);
	
	public String extractAuthors(Element htmlElement);
	
	public String extractImageUrl(Element htmlElement);
	
	public String extractIsbn(Element htmlElement);
	
	public String extractFormat(Element htmlElement);
	
	public String extractPublisher(Element htmlElement);
	
	public PricePoint extractPricePoint(Element bookCard, Locale locale);


	/**
	 * @param bookPage
	 * @return The description of the product on that page.
	 * @throws NullPointerException
	 *             if the passed document is null.
	 */
	public String extractDescription(Document bookPage);
	/**
	 * @brief Function that extracts the attribute of a book off of its page.
	 * @param bookPage
	 * @return An associative array of attributes.
	 * @throws NullPointerException
	 *             if the passed document is null.
	 */
	public Map<String, String> extractAttributes(Element bookPage);
	
}
