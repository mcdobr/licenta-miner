package me.mircea.licenta.scraper;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
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
import java.net.SocketTimeoutException;
import java.time.Duration;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(Scraper.class);

	private final Job job;

	private final ExecutorService backgroundWorker;
    private final ProductExtractor extractor;

    // grija la acces nesincronizat?
    private int noPagesToBeRequested = 0;
    private int noRequests = 0;
    private int noPagesReached = 0;
	private int noProductOfferPairsFound = 0;

	private Duration totalAsyncDuration = Duration.ZERO;
	private Duration totalDownloadDuration = Duration.ZERO;
	private Duration totalProcessingDuration = Duration.ZERO;
	private Duration totalCrawlPersistenceDuration = Duration.ZERO;
	private Duration totalProductPersistenceDuration = Duration.ZERO;


    public Scraper(String seed, ObjectId continueJob) throws IOException {
    	Preconditions.checkNotNull(seed);
    	Preconditions.checkNotNull(continueJob);

    	this.job = new Job(continueJob, seed, JobType.SCRAPE);
		this.backgroundWorker = Executors.newSingleThreadExecutor();
		this.extractor = chooseStrategy();
	}

	public Scraper(String seed) throws IOException {
        Preconditions.checkNotNull(seed);
		this.job = new Job(seed, JobType.SCRAPE);
		this.backgroundWorker = Executors.newSingleThreadExecutor();
		this.extractor = chooseStrategy();
	}

    private ProductExtractor chooseStrategy() {
        Optional<Wrapper> possibleWrapper = CrawlDatabaseManager.instance.getWrapperForDomain(this.job.getDomain());
        if (possibleWrapper.isPresent()) {
            return new WrapperBookExtractor(possibleWrapper.get());
        } else {
            return new HeuristicalBookExtractor();
        }
    }

	@Override
	public void run() {
		try {
			scrape();
		} catch (InterruptedException e) {
			LOGGER.warn("Thread interrupted {}", e);
			Thread.currentThread().interrupt();
		}
	}

	public void scrape() throws InterruptedException {
		CrawlDatabaseManager.instance.upsertJob(this.job);
		Iterable<Page> pages = CrawlDatabaseManager.instance.getPossibleProductPages(job.getDomain());

		for (Page page : pages) {
			if (this.job.getId().equals(page.getLastJob()))
				break;




			backgroundWorker.execute(() -> scrapeFromPage(page));
			TimeUnit.MILLISECONDS.sleep(job.getRobotRules().getCrawlDelay());

			++noPagesToBeRequested;
		}

		this.job.setEnd(Instant.now());
		this.job.setStatus(JobStatus.FINISHED);
		CrawlDatabaseManager.instance.upsertJob(job);

		shutdownExecutor();

		LOGGER.info("Domain {}, number of pages to be requested: {}", job.getDomain(), noPagesToBeRequested);
		LOGGER.info("Domain {}, number of GET requests made: {}", job.getDomain(), noRequests);
		LOGGER.info("Domain {}, number of pages reached: {}", job.getDomain(), noPagesReached);
		LOGGER.info("Domain {}, number of product-offer pairs found: {}", job.getDomain(), noProductOfferPairsFound);


		LOGGER.info("Domain {}, total async duration: {}", job.getDomain(), totalAsyncDuration);
		LOGGER.info("Domain {}, total download of page time: {}", job.getDomain(), totalDownloadDuration);
		LOGGER.info("Domain {}, total processing of page time: {}", job.getDomain(), totalProcessingDuration);
        LOGGER.info("Domain {}, total crawl persistence of product time: {}", job.getDomain(), totalCrawlPersistenceDuration);
		LOGGER.info("Domain {}, total product persistence of product time: {}", job.getDomain(), totalProductPersistenceDuration);


		LOGGER.info("Domain {}, average async duration: {}", job.getDomain(), totalAsyncDuration.dividedBy(noPagesToBeRequested));
		LOGGER.info("Domain {}, average download of page time: {}", job.getDomain(), totalDownloadDuration.dividedBy(noPagesToBeRequested));
		LOGGER.info("Domain {}, average processing of page time: {}", job.getDomain(), totalDownloadDuration.dividedBy(noPagesReached));
        LOGGER.info("Domain {}, average crawl persistence of product time: {}", job.getDomain(), totalCrawlPersistenceDuration.dividedBy((noPagesReached)));
		LOGGER.info("Domain {}, average product persistence of product time: {}", job.getDomain(), totalDownloadDuration.dividedBy(noProductOfferPairsFound));
	}

	private Optional<Document> tryToGetPage(String url) {
		final int MAX_TRIES = 2;
		Document bookPage = null;
		for (int i = 0; i < MAX_TRIES; ++i) {
			try {
				++noRequests;
				LOGGER.info("Retrieving {} at {}", url, Instant.now());
				bookPage = HtmlUtil.sanitizeHtml(
						Jsoup.connect(url).userAgent(Job.getDefault("user_agent")).get());
				break;
			} catch (SocketTimeoutException e) {
				LOGGER.warn("Socket timed out on {}", url);
			} catch (IOException e) {
				LOGGER.warn("Could not get page {}", url);
			}
		}
		return Optional.ofNullable(bookPage);
	}

	private void scrapeFromPage(Page page) {
		Stopwatch asyncStopwatch = Stopwatch.createStarted();

		Optional<Document> possibleDocument = tryToGetPage(page.getUrl());
		totalDownloadDuration = totalDownloadDuration.plus(asyncStopwatch.elapsed());

		if (possibleDocument.isPresent()) {
			scrapeFromReachablePage(page, possibleDocument.get());

			++noPagesReached;
		} else {
			page.setType(PageType.UNREACHABLE);
		}

		updateCrawlFrontier(page);


		asyncStopwatch.stop();
		totalAsyncDuration = totalAsyncDuration.plus(asyncStopwatch.elapsed());
	}

	private Page scrapeFromReachablePage(Page page, Document htmlDocument) {
		Stopwatch processingStopwatch = Stopwatch.createStarted();
    	SimpleImmutableEntry<Book, PricePoint> bookOfferPair = extractBookOffer(htmlDocument);

    	processingStopwatch.stop();
		totalProcessingDuration = totalProcessingDuration.plus(processingStopwatch.elapsed());


		page.setTitle(htmlDocument.title());
		page.setUrl(HtmlUtil.getCanonicalUrl(htmlDocument).orElse(page.getUrl()));
		page.setRetrievedTime(Instant.now());
		page.setLastJob(this.job.getId());

		if (hasValidBookOfferPair(bookOfferPair)) {
			++noProductOfferPairsFound;

            page.setType(PageType.PRODUCT);
            ObjectifyService.run(() -> {
                persistBookOfferPair(bookOfferPair);
                return null;
            });
		} else if (!hasValidBook(bookOfferPair.getKey())) {
			page.setType(PageType.JUNK);
			LOGGER.info("Page did not have a product {}", page.getUrl());
		} else if (!hasValidOffer(bookOfferPair.getValue())){
			page.setType(PageType.UNAVAILABLE);
			LOGGER.info("Page did not have an offer {}", page.getUrl());
		}

		return page;
	}

	private boolean hasValidBookOfferPair(SimpleImmutableEntry<Book, PricePoint> bookOfferPair) {
        return hasValidBook(bookOfferPair.getKey()) && hasValidOffer(bookOfferPair.getValue());
    }

    private boolean hasValidBook(Book book) {
        return book != null && book.getIsbn() != null;
    }

    private boolean hasValidOffer(PricePoint pricePoint) {
        return pricePoint != null;
    }

	private void updateCrawlFrontier(Page page) {
		Preconditions.checkNotNull(page);

		Stopwatch stopwatch = Stopwatch.createStarted();
		CrawlDatabaseManager.instance.upsertOnePage(page);
		stopwatch.stop();

		totalCrawlPersistenceDuration = totalCrawlPersistenceDuration.plus(stopwatch.elapsed());
	}

	private SimpleImmutableEntry<Book, PricePoint> extractBookOffer(Document doc) {
		Preconditions.checkNotNull(doc);

		Book book = (Book)this.extractor.extract(doc);
		PricePoint offer = this.extractor.extractPricePoint(doc, Locale.forLanguageTag("ro-ro"));
		return new SimpleImmutableEntry<>(book, offer);
	}

	private void persistBookOfferPair(SimpleImmutableEntry<Book, PricePoint> bookOfferPair) {
    	Stopwatch persistStopwatch = Stopwatch.createStarted();
		persistBookOfferPair(bookOfferPair.getKey(), bookOfferPair.getValue());

		persistStopwatch.stop();
		totalProductPersistenceDuration = totalProductPersistenceDuration.plus(persistStopwatch.elapsed());
	}

	private void persistBookOfferPair(Book book, PricePoint offer) {
    	Preconditions.checkNotNull(book);
    	Preconditions.checkNotNull(offer);

        List<Book> persistedBooks = findBookByIsbn(book);
        Key<PricePoint> offerKey = ObjectifyService.ofy().save().entity(offer).now();
		book.setBestCurrentOffer(offer);


		book.getPricepoints().add(offerKey);
		if (persistedBooks.isEmpty()) {
			ofy().save().entity(book);
			LOGGER.info("Saving new {} to db.", book);
		} else {
			Optional<Book> mergedBook = persistedBooks.stream().reduce(Book::merge);

			if (mergedBook.isPresent()) {
				Book bookToPersist = mergedBook.get();
				bookToPersist = Book.merge(bookToPersist, book);

				ofy().delete().entities(persistedBooks);
				ofy().save().entities(bookToPersist);
				LOGGER.info("Updating book to {} in db.", mergedBook.get());
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
				LOGGER.info("Exiting scraper because of timeout: {}", backgroundWorker);
				System.exit(0);
			}
			LOGGER.info("Exiting scraper normally: {}", backgroundWorker);
		}
	}

	public Job getJob() {
		return job;
	}
}
