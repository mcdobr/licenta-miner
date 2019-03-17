package me.mircea.licenta.scraper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import me.mircea.licenta.core.crawl.db.model.Job;
import me.mircea.licenta.core.crawl.db.model.JobStatus;
import me.mircea.licenta.core.crawl.db.model.JobType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.LoadType;

import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.core.crawl.db.CrawlDatabaseManager;
import me.mircea.licenta.core.crawl.db.model.Page;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.WebWrapper;
import me.mircea.licenta.scraper.infoextraction.HeuristicalStrategy;
import me.mircea.licenta.scraper.infoextraction.InformationExtractionStrategy;
import me.mircea.licenta.core.parser.utils.HtmlUtil;

import static com.googlecode.objectify.ObjectifyService.*;

/**
 * @brief This class is used to extract books from product description pages.
 */
public class Scraper implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Scraper.class);

	private final Job job;
	private WebWrapper wrapper;
	private final ExecutorService backgroundWorker;

	public Scraper(String seed) throws IOException {
		this.job = new Job(seed, JobType.SCRAPE);
		this.backgroundWorker = Executors.newSingleThreadExecutor();
	}

	@Override
	public void run() {
		try {
			InformationExtractionStrategy strategy = chooseStrategy();
			scrape(strategy);
		} catch (InterruptedException e) {
			logger.warn("Thread interrupted {}", e);
			Thread.currentThread().interrupt();
		}
	}

	public void scrape(InformationExtractionStrategy strategy) throws InterruptedException {
		CrawlDatabaseManager.instance.upsertJob(this.job);
		Iterable<Page> pages = CrawlDatabaseManager.instance.getPossibleProductPages(job.getDomain());

		for (Page page : pages) {
			if (this.job.getId().equals(page.getLastJob()))
				break;

			Optional<Document> optionalDoc = tryToGetPage(page.getUrl());
			if (optionalDoc.isPresent()) {
				Document pageContent = optionalDoc.get();

				updateProductsDatabase(strategy, pageContent);
				updateCrawlFrontier(page, pageContent);
			}
			TimeUnit.MILLISECONDS.sleep(job.getRobotRules().getCrawlDelay());
		}

		this.job.setEnd(Instant.now());
		this.job.setStatus(JobStatus.FINISHED);
		CrawlDatabaseManager.instance.upsertJob(job);

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
						Jsoup.connect(url).userAgent(Job.getDefault("user_agent")).get());
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
		page.setLastJob(this.job.getId());

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
		book.setLatestRetrievedTime(offer.getRetrievedTime());
		book.setLatestRetrievedPrice(offer.getNominalValue().divide(BigDecimal.valueOf(100)).toString());

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
		if (!backgroundWorker.awaitTermination(1, TimeUnit.MINUTES)) {
			backgroundWorker.shutdownNow();
			if (!backgroundWorker.awaitTermination(1, TimeUnit.MINUTES)) {
				logger.info("Exiting scraper because of timeout: {}", backgroundWorker);
				System.exit(0);
			}
			logger.info("Exiting scraper normally: {}", backgroundWorker);
		}
	}

	public Job getJob() {
		return job;
	}
}
