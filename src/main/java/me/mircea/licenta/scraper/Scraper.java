package me.mircea.licenta.scraper;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.LoadType;
import javafx.scene.paint.Stop;
import me.mircea.licenta.core.crawl.db.CrawlDatabaseManager;
import me.mircea.licenta.core.crawl.db.RobotDefaults;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.AbstractMap.SimpleImmutableEntry;

/**
 * This class is used to extract books from product description pages.
 */
public class Scraper implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Scraper.class);

	private final Job job;
    private final ProductExtractor extractor;

	private final ScheduledExecutorService downloader;
	private final BlockingQueue<Map.Entry<Page, Document>> documentQueue;

	private AtomicInteger noPagesToBeRequested = new AtomicInteger(0);
	private AtomicInteger noRequests = new AtomicInteger(0);
	private AtomicInteger noPagesReached = new AtomicInteger(0);
	private AtomicInteger noProductOfferPairsFound = new AtomicInteger(0);

	private Duration totalDuration = Duration.ZERO;
	private Duration totalDownloadDuration = Duration.ZERO;
	private Duration totalProcessingDuration = Duration.ZERO;
	private Duration totalCrawlPersistenceDuration = Duration.ZERO;
	private Duration totalProductPersistenceDuration = Duration.ZERO;


	public Scraper(String domain) throws IOException {
		Preconditions.checkNotNull(domain);

		this.job = new Job(domain, JobType.SCRAPE);
		this.extractor = chooseStrategy();

		this.downloader = Executors.newScheduledThreadPool(1);
		this.documentQueue = new LinkedBlockingQueue<>();
	}

	public Scraper(String domain, ObjectId jobIdToBeContinued) throws IOException {
		Preconditions.checkNotNull(domain);
		Preconditions.checkNotNull(jobIdToBeContinued);

		this.job = new Job(domain, JobType.SCRAPE, jobIdToBeContinued);
		this.extractor = chooseStrategy();

		this.downloader = Executors.newScheduledThreadPool(1);
		this.documentQueue = new LinkedBlockingQueue<>();
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
			Stopwatch stopwatch = Stopwatch.createStarted();
			scrape();

			stopwatch.stop();
			totalDuration = stopwatch.elapsed();
		} catch (InterruptedException e) {
			LOGGER.warn("Thread interrupted {}", e);
			Thread.currentThread().interrupt();
		}
	}

	public void scrape() throws InterruptedException {
		CrawlDatabaseManager.instance.upsertJob(this.job);
		Iterator<Page> pageIterator = CrawlDatabaseManager.instance.getPossibleProductPages(job.getDomain()).iterator();

		downloader.scheduleAtFixedRate(() -> {
			if (!pageIterator.hasNext()) {
				downloader.shutdown();
			} else {
				Page page = pageIterator.next();
				if (this.job.getId().equals(page.getLastJob())) {
					downloader.shutdown();
				} else {
					Optional<Document> possibleDocument = downloadDocument(page.getUrl());
					documentQueue.add(new AbstractMap.SimpleImmutableEntry<Page, Document>(page, possibleDocument.orElse(null)));

					noPagesToBeRequested.getAndIncrement();
				}
			}
		}, 0, job.getRobotRules().getCrawlDelay(), TimeUnit.MILLISECONDS);

		while (!downloader.isShutdown() || !documentQueue.isEmpty()) {
			Map.Entry<Page, Document> documentPair = documentQueue.poll(500, TimeUnit.MILLISECONDS);

			if (documentPair != null) {
				Page page = documentPair.getKey();
				Document doc = documentPair.getValue();
				if (doc != null) {
					scrapeFromReachablePage(documentPair.getKey(), documentPair.getValue());
				} else {
					page.setType(PageType.UNREACHABLE);
				}

				updateCrawlFrontier(page);

				if (noPagesToBeRequested.get() % 10000 == 0) {
					logStatistics();
				}
			}
		}

		this.job.setEnd(Instant.now());
		this.job.setStatus(JobStatus.FINISHED);
		CrawlDatabaseManager.instance.upsertJob(job);

		LOGGER.info("Job {} finished", job);
		logStatistics();
	}

	private void logStatistics() {
		LOGGER.info("Domain {}, number of pages to be requested: {}", job.getDomain(), noPagesToBeRequested);
		LOGGER.info("Domain {}, number of GET requests made: {}", job.getDomain(), noRequests);
		LOGGER.info("Domain {}, number of pages reached: {}", job.getDomain(), noPagesReached);
		LOGGER.info("Domain {}, number of product-offer pairs found: {}", job.getDomain(), noProductOfferPairsFound);


		LOGGER.info("Domain {}, total duration: {}", job.getDomain(), totalDuration);
		LOGGER.info("Domain {}, total download of page time: {}", job.getDomain(), totalDownloadDuration);
		LOGGER.info("Domain {}, total processing of page time: {}", job.getDomain(), totalProcessingDuration);
		LOGGER.info("Domain {}, total crawl persistence of product time: {}", job.getDomain(), totalCrawlPersistenceDuration);
		LOGGER.info("Domain {}, total product persistence of product time: {}", job.getDomain(), totalProductPersistenceDuration);


		LOGGER.info("Domain {}, average download of page time: {}", job.getDomain(), totalDownloadDuration.dividedBy(noPagesToBeRequested.get()));
		LOGGER.info("Domain {}, average processing of page time: {}", job.getDomain(), totalProcessingDuration.dividedBy(noPagesReached.get()));
		LOGGER.info("Domain {}, average crawl persistence of product time: {}", job.getDomain(), totalCrawlPersistenceDuration.dividedBy((noPagesReached.get())));
		LOGGER.info("Domain {}, average product persistence of product time: {}", job.getDomain(), totalProductPersistenceDuration.dividedBy(noProductOfferPairsFound.get()));
	}

	private Optional<Document> downloadDocument(String url) {

    	Stopwatch stopwatch = Stopwatch.createStarted();
		final int MAX_TRIES = 2;
		Document bookPage = null;
		for (int i = 0; i < MAX_TRIES; ++i) {

			noRequests.getAndIncrement();
			try {
				LOGGER.info("Retrieving {} at {}", url, Instant.now());
				bookPage = HtmlUtil.sanitizeHtml(
						Jsoup.connect(url).userAgent(RobotDefaults.getDefault("user_agent")).get());

				stopwatch.stop();

				totalDownloadDuration = totalDownloadDuration.plus(stopwatch.elapsed());
				noPagesReached.getAndIncrement();
				break;
			} catch (SocketTimeoutException e) {
				LOGGER.warn("Socket timed out on {}", url);
			} catch (IOException e) {
				LOGGER.warn("Could not get page {}", url);
			}
		}
		return Optional.ofNullable(bookPage);
	}

	private Page scrapeFromReachablePage(Page page, Document htmlDocument) {
    	Stopwatch stopwatch = Stopwatch.createStarted();

    	SimpleImmutableEntry<Book, PricePoint> bookOfferPair = extractBookOffer(htmlDocument);

		page.setTitle(htmlDocument.title());
		page.setUrl(HtmlUtil.getCanonicalUrl(htmlDocument).orElse(page.getUrl()));
		page.setRetrievedTime(Instant.now());
		page.setLastJob(this.job.getId());

		if (hasValidBookOfferPair(bookOfferPair)) {
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

		stopwatch.stop();
		totalProcessingDuration = totalProcessingDuration.plus(stopwatch.elapsed());

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
		Stopwatch stopwatch = Stopwatch.createStarted();

		persistBookOfferPair(bookOfferPair.getKey(), bookOfferPair.getValue());

		stopwatch.stop();
		totalProductPersistenceDuration = totalProductPersistenceDuration.plus(stopwatch.elapsed());
	}

	private void persistBookOfferPair(Book book, PricePoint offer) {
    	Preconditions.checkNotNull(book);
    	Preconditions.checkNotNull(offer);

		noProductOfferPairsFound.getAndIncrement();

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

	public Job getJob() {
		return job;
	}
}
