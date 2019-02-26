package me.mircea.licenta.miner;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.LoadType;

import me.mircea.licenta.core.entities.PricePoint;
import me.mircea.licenta.core.crawl.CrawlDatabaseManager;
import me.mircea.licenta.core.crawl.CrawlRequest;
import me.mircea.licenta.core.crawl.db.model.Page;
import me.mircea.licenta.core.entities.Book;
import me.mircea.licenta.core.entities.WebWrapper;
import me.mircea.licenta.core.infoextraction.HeuristicalStrategy;
import me.mircea.licenta.core.infoextraction.InformationExtractionStrategy;
import me.mircea.licenta.core.parser.utils.HtmlUtil;

import static com.googlecode.objectify.ObjectifyService.*;


/**
 * @author mircea
 * @brief This class is used to extract books from a (single) given multi-book
 *        page. The intended strategy is to extract the basic information of the
 *        given entry, then using the specific-book page, to correlate and find
 *        out details about the book.
 */
public class Miner implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Miner.class);
	
	private CrawlRequest crawlRequest;
	private WebWrapper wrapper;
	
	public Miner(CrawlRequest crawlRequest) {
		this.crawlRequest = crawlRequest;
		//this.wrapper = ObjectifyService.run(() -> getWrapperForSite(site));
	}
	

	@Override
	public void run() {
		InformationExtractionStrategy strategy = chooseStrategy();
		
		Iterable<Page> pages = CrawlDatabaseManager.instance.getPossibleProductPages(crawlRequest.getDomain());
		
		for (Page page: pages) {
			Optional<Document> optionalDoc = tryToGetPage(page.getUrl());
			if (optionalDoc.isPresent()) {
				Document doc = optionalDoc.get();
				
				AbstractMap.SimpleEntry<Book, PricePoint> bop = extractBookOffer(doc, strategy);
				ObjectifyService.run(new Work<Void>() {
					@Override
					public Void run() {
						persistBookOfferPair(bop);
						return null;
					}
				});
			}
			
			try {
				TimeUnit.MILLISECONDS.sleep(crawlRequest.getRobotRules().getCrawlDelay());
			} catch (InterruptedException e) {
				logger.warn("Interrupted thread: {}", e);
				Thread.currentThread().interrupt();
			}
		}
	}
	

	private InformationExtractionStrategy chooseStrategy() {
		//WebWrapper wrapper = getWrapperForSite(this.site);
		return new HeuristicalStrategy();
	}
	
	private Optional<Document> tryToGetPage(String url) {
		final int MAX_TRIES = 2;
		Document bookPage = null;
		for (int i = 0; i < MAX_TRIES; ++i) {
			try {
				bookPage = HtmlUtil.sanitizeHtml(Jsoup.connect(url).get());
				TimeUnit.MILLISECONDS.sleep(this.crawlRequest.getRobotRules().getCrawlDelay());
				break;
			} catch (SocketTimeoutException e) {
				logger.warn("Socket timed out on {}", url);
			} catch (IOException e) {
				logger.warn("Could not get page {}", url);
			} catch (InterruptedException e) {
				logger.info("Politeness sleep was interrupted {}", url);
				Thread.currentThread().interrupt();
			}
		}
		return Optional.ofNullable(bookPage);
	}
	
	private AbstractMap.SimpleEntry<Book, PricePoint> extractBookOffer(Document page, InformationExtractionStrategy strategy) {
		Book book = strategy.extractBook(page);
		PricePoint offer = strategy.extractPricePoint(page, Locale.forLanguageTag("ro-ro"));
		
		return new AbstractMap.SimpleEntry<>(book, offer);
	}
	
	private void persistBookOfferPair(AbstractMap.SimpleEntry<Book, PricePoint> bookOfferPair) {
		persistBookOfferPair(bookOfferPair.getKey(), bookOfferPair.getValue());
	}
	
	private void persistBookOfferPair(Book book, PricePoint offer) {
		List<Book> books = findBook(book);
		Key<PricePoint> offerKey = ObjectifyService.ofy().save().entity(offer).now();
		
		book.getPricepoints().add(offerKey);	
		if (books.isEmpty()) {
			ofy().save().entity(book);
			logger.info("Saving new {} to db.", book);
		} else {
			Book persistedBook = books.get(0);
			Optional<Book> mergedBook = Book.merge(persistedBook, book);

			if (mergedBook.isPresent()) {
				Book bookToPersist = mergedBook.get();
				ofy().delete().entities(persistedBook);
				ofy().save().entities(bookToPersist);
				logger.info("Updating book to {} in db.", mergedBook.get());
			}
		}
	}
	
	/**
	 * @return A list of books containing either one with the same isbn, or other
	 *         books that have the same name.
	 */
	private List<Book> findBook(Book candidate) {
		final int MAX_BOOKS_RETURNED = 10;
		
		LoadType<Book> bookLoader = ObjectifyService.ofy().load().type(Book.class);
		if (candidate.getIsbn() != null)
			return bookLoader.filter("isbn", candidate.getIsbn()).list();
		else
			return bookLoader.filter("title", candidate.getTitle()).limit(MAX_BOOKS_RETURNED).list();
	}

	/*
	public Elements getBookCards() {
		String bookSelector = "[class*='produ']:has(img):has(a)";
		return multiBookPage.select(String.format("%s:not(:has(%s))", bookSelector, bookSelector));
	}
	
	/*
	private Key<WebWrapper> persistNewWrapper(WrapperGenerationStrategy wrapperGenerator) {
		Element anySingleBookPage = singleBookUrls.values().iterator().next();
		WebWrapper wrapper = wrapperGenerator.generateWrapper(anySingleBookPage, new Elements(multiBookPage));
		
		logger.info("Adding wrapper {}", wrapper);
		return ObjectifyService.ofy().save().entity(wrapper).now();
	}


	private WebWrapper getWrapperForSite(String site) {
		Preconditions.checkNotNull(site);
		
		return ObjectifyService.ofy().load().type(WebWrapper.class).filter("site", site).first().now();
	}
	*/
}
