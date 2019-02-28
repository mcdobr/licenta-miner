package me.mircea.licenta.miner;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
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
 * @brief This class is used to extract books from product description pages.
 */
public class Miner {
	private static final Logger logger = LoggerFactory.getLogger(Miner.class);

	private final CrawlRequest crawlRequest;
	private WebWrapper wrapper;
	private final ExecutorService backgroundWorker;

	public Miner(CrawlRequest crawlRequest) {
		this.crawlRequest = crawlRequest;
		this.backgroundWorker = Executors.newSingleThreadExecutor();
	}

	public void index() throws InterruptedException {
		InformationExtractionStrategy strategy = chooseStrategy();
		Iterable<Page> pages = CrawlDatabaseManager.instance.getPossibleProductPages(crawlRequest.getDomain());

		for (Page page : pages) {
			Optional<Document> optionalDoc = tryToGetPage(page.getUrl());
			if (optionalDoc.isPresent()) {
				Document pageContent = optionalDoc.get();

				updateProductsDatabase(strategy, pageContent);
				updateCrawlFrontier(page, pageContent);
			}
			TimeUnit.MILLISECONDS.sleep(crawlRequest.getRobotRules().getCrawlDelay());
		}

		shutdownExecutor();
	}

	private InformationExtractionStrategy chooseStrategy() {
		// WebWrapper wrapper = getWrapperForSite(this.site);
		return new HeuristicalStrategy();
	}

	private Optional<Document> tryToGetPage(String url) {
		final int MAX_TRIES = 2;
		Document bookPage = null;
		for (int i = 0; i < MAX_TRIES; ++i) {
			try {
				logger.info("Retrieving {} at {}", url, Instant.now());
				bookPage = HtmlUtil.sanitizeHtml(
						Jsoup.connect(url).userAgent(this.crawlRequest.getProperties().get("user_agent")).get());
				break;
			} catch (SocketTimeoutException e) {
				logger.warn("Socket timed out on {}", url);
			} catch (IOException e) {
				logger.warn("Could not get page {}", url);
			}
		}
		return Optional.ofNullable(bookPage);
	}

	private void updateProductsDatabase(InformationExtractionStrategy strategy, Document pageContent) {
		backgroundWorker.submit(() -> {
			AbstractMap.SimpleEntry<Book, PricePoint> bop = extractBookOffer(pageContent, strategy);
			if (bop.getKey().getIsbn() == null)
				return;
			
			ObjectifyService.run(() -> {
				persistBookOfferPair(bop);
				return null;
			});
		});
	}

	private void updateCrawlFrontier(Page page, Document content) {
		page.setTitle(content.title());
		page.setUrl(HtmlUtil.getCanonicalUrl(content).orElse(page.getUrl()));
		page.setRetrievedTime(Instant.now());
		CrawlDatabaseManager.instance.upsertOnePage(page);
	}

	private AbstractMap.SimpleEntry<Book, PricePoint> extractBookOffer(Document page,
			InformationExtractionStrategy strategy) {
		Book book = strategy.extractBook(page);
		PricePoint offer = strategy.extractPricePoint(page, Locale.forLanguageTag("ro-ro"));
		offer.setPageTitle(page.title());
		
		return new AbstractMap.SimpleEntry<>(book, offer);
	}

	private void persistBookOfferPair(AbstractMap.SimpleEntry<Book, PricePoint> bookOfferPair) {
		persistBookOfferPair(bookOfferPair.getKey(), bookOfferPair.getValue());
	}

	private void persistBookOfferPair(Book book, PricePoint offer) {
		List<Book> persistedBooks = findBookByIsbn(book);
		Key<PricePoint> offerKey = ObjectifyService.ofy().save().entity(offer).now();

		book.getPricepoints().add(offerKey);
		if (persistedBooks.isEmpty()) {
			ofy().save().entity(book);
			logger.info("Saving new {} to db.", book);
		} else {
			Optional<Book> mergedBook = persistedBooks.stream().reduce(Book::merge);

			if (mergedBook.isPresent()) {
				Book bookToPersist = mergedBook.get();
				bookToPersist = Book.merge(bookToPersist, book);

				ofy().delete().entities(persistedBooks);
				ofy().save().entities(bookToPersist);
				logger.info("Updating book to {} in db.", mergedBook.get());
			}
		}
	}

	/**
	 * @return A list of books containing either one with the same isbn, or other
	 *         books that have the same name.
	 */
	private List<Book> findBookByIsbn(Book candidate) {
		LoadType<Book> bookLoader = ObjectifyService.ofy().load().type(Book.class);
		if (candidate.getIsbn() != null)
			return bookLoader.filter("isbn", candidate.getIsbn()).list();
		else
			return Collections.emptyList();
	}

	private void shutdownExecutor() throws InterruptedException {
		if (!backgroundWorker.awaitTermination(2, TimeUnit.MINUTES)) {
			backgroundWorker.shutdownNow();
			if (!backgroundWorker.awaitTermination(2, TimeUnit.MINUTES)) {
				logger.info("Exiting because of timeout: {}", backgroundWorker);
				System.exit(0);
			}
			logger.info("Exiting normally: {}", backgroundWorker);
		}
	}
}
