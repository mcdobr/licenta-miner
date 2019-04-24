package me.mircea.licenta.scraper;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.LoadType;
import me.mircea.licenta.core.crawl.db.CrawlDatabaseManager;
import me.mircea.licenta.core.crawl.db.model.*;
import me.mircea.licenta.core.parser.utils.HtmlUtil;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.scraper.infoextraction.HeuristicalBookExtractor;
import me.mircea.licenta.scraper.infoextraction.ProductExtractor;
import me.mircea.licenta.scraper.infoextraction.WrapperBookExtractor;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.AbstractMap.SimpleImmutableEntry;

/**
 * @brief This class is used to extract books from product description pages.
 */
public class Scraper implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Scraper.class);

	private final Job job;
	private final ExecutorService backgroundWorker;


    public Scraper(String seed, ObjectId continueJob) throws IOException {
    	Preconditions.checkNotNull(seed);
    	Preconditions.checkNotNull(continueJob);

    	this.job = new Job(continueJob, seed, JobType.SCRAPE);
		this.backgroundWorker = Executors.newSingleThreadExecutor();
	}


	public Scraper(String seed) throws IOException {
        Preconditions.checkNotNull(seed);
		this.job = new Job(seed, JobType.SCRAPE);
		this.backgroundWorker = Executors.newSingleThreadExecutor();
	}

	@Override
	public void run() {
		try {
			ProductExtractor strategy = chooseStrategy();
			scrape(strategy);
		} catch (InterruptedException e) {
			logger.warn("Thread interrupted {}", e);
			Thread.currentThread().interrupt();
		}
	}

	public void scrape(ProductExtractor strategy) throws InterruptedException {
		CrawlDatabaseManager.instance.upsertJob(this.job);
		Iterable<Page> pages = CrawlDatabaseManager.instance.getPossibleProductPages(job.getDomain());

		for (Page page : pages) {
			if (this.job.getId().equals(page.getLastJob()))
				break;

			propagateChanges(page, strategy);
			TimeUnit.MILLISECONDS.sleep(job.getRobotRules().getCrawlDelay());
		}

		this.job.setEnd(Instant.now());
		this.job.setStatus(JobStatus.FINISHED);
		CrawlDatabaseManager.instance.upsertJob(job);

		shutdownExecutor();
	}

	private ProductExtractor chooseStrategy() {
    	Optional<Wrapper> possibleWrapper = CrawlDatabaseManager.instance.getWrapperForDomain(this.job.getDomain());
    	if (possibleWrapper.isPresent()) {
			return new WrapperBookExtractor(possibleWrapper.get());
		} else {
			return new HeuristicalBookExtractor();
		}
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

	private void propagateChanges(Page page, ProductExtractor strategy) {
	    Runnable work = () -> {
            Optional<Document> possibleDocument = tryToGetPage(page.getUrl());
            if (possibleDocument.isPresent()) {
				scrapeFromReachablePage(page, strategy, possibleDocument.get());
            } else {
            	page.setType(PageType.JUNK);
			}
			updateCrawlFrontier(page);
        };
	    backgroundWorker.submit(work);
	}

	private Page scrapeFromReachablePage(Page page, ProductExtractor strategy, Document htmlDocument) {
		SimpleImmutableEntry<Book, PricePoint> bookOfferPair = extractBookOffer(htmlDocument, strategy);

		page.setTitle(htmlDocument.title());
		page.setUrl(HtmlUtil.getCanonicalUrl(htmlDocument).orElse(page.getUrl()));
		page.setRetrievedTime(Instant.now());
		page.setLastJob(this.job.getId());

		// if has a product-offer pair
		if (bookOfferPair.getKey() != null && bookOfferPair.getValue() != null) {
			if (bookOfferPair.getKey().getIsbn() != null) {
				page.setType(PageType.PRODUCT);
				ObjectifyService.run(() -> {
					persistBookOfferPair(bookOfferPair);
					return null;
				});
			} else {
				page.setType(PageType.JUNK);
			}
		} else if (bookOfferPair.getKey() == null){
			page.setType(PageType.JUNK);
		} else {
			page.setType(PageType.UNAVAILABLE);
		}

		return page;
	}

	private void updateCrawlFrontier(Page page) {
		Preconditions.checkNotNull(page);
		CrawlDatabaseManager.instance.upsertOnePage(page);
	}

	/**
	 * @return pair of non-null book and pricepoint or empty
	 */
	private SimpleImmutableEntry<Book, PricePoint> extractBookOffer(Document page,
			ProductExtractor strategy) {
		Preconditions.checkNotNull(page);
		Preconditions.checkNotNull(strategy);

		Book book = (Book)strategy.extract(page);
		PricePoint offer = strategy.extractPricePoint(page, Locale.forLanguageTag("ro-ro"));

		return new SimpleImmutableEntry<>(book, offer);
	}

	private void persistBookOfferPair(SimpleImmutableEntry<Book, PricePoint> bookOfferPair) {
		persistBookOfferPair(bookOfferPair.getKey(), bookOfferPair.getValue());
	}

	private void persistBookOfferPair(Book book, PricePoint offer) {
    	Preconditions.checkNotNull(book);
    	Preconditions.checkNotNull(offer);

    	/*
		book.setLatestRetrievedTime(offer.getRetrievedTime());
		book.setLatestRetrievedPrice(offer.getNominalValue().setScale(2, BigDecimal.ROUND_CEILING).toString());
		*/

        List<Book> persistedBooks = findBookByIsbn(book);
        Key<PricePoint> offerKey = ObjectifyService.ofy().save().entity(offer).now();
		book.setBestCurrentOffer(offer);


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
