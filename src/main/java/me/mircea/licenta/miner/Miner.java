package me.mircea.licenta.miner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.LoadType;

import crawlercommons.robots.BaseRobotRules;
import me.mircea.licenta.core.entities.PricePoint;
import me.mircea.licenta.core.entities.Book;
import me.mircea.licenta.core.entities.WebWrapper;
import me.mircea.licenta.core.infoextraction.HeuristicalStrategy;
import me.mircea.licenta.core.infoextraction.InformationExtractionStrategy;
import me.mircea.licenta.core.parser.utils.HtmlUtil;

/**
 * @author mircea
 * @brief This class is used to extract books from a (single) given multi-book
 *        page. The intended strategy is to extract the basic information of the
 *        given entry, then using the specific-book page, to correlate and find
 *        out details about the book.
 */
public class Miner implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(Miner.class);
	
	private final Document multiBookPage;
	private final List<String> singleBookUrls;
	private final Instant retrievedTime;
	private final String site;
	private final BaseRobotRules crawlRules;
	private WebWrapper wrapper;
	
	public Miner(Document multiBookPage, Instant retrievedTime, List<String> singleBookUrls, BaseRobotRules crawlRules) throws MalformedURLException {
		this.multiBookPage = multiBookPage;
		this.retrievedTime = retrievedTime;
		this.singleBookUrls = singleBookUrls;
		this.site = HtmlUtil.getDomainOfUrl(multiBookPage.baseUri());
		this.crawlRules = crawlRules;
		// this.wrapper = ObjectifyService.run(() -> getWrapperForSite(site));
	}

	@Override
	public void run() {
		HtmlUtil.sanitizeHtml(multiBookPage);
		ObjectifyService.run(() -> {
				InformationExtractionStrategy extractionStrategy = chooseStrategy();
				persistBooks(extractionStrategy);
				return null;
			}
		);
	}
	
	private void persistBooks(InformationExtractionStrategy strategy) {
		int inserted = 0;
		int updated = 0;

		final Elements bookCards = getBookCards();
		for (String bookUrl : singleBookUrls) {
			final Optional<Document> optSingleBookPage = tryToGetPage(bookUrl);
			
			Document bookPage;
			if (optSingleBookPage.isPresent())
				bookPage = optSingleBookPage.get();
			else
				continue;
			
			Element bookCard = bookCards.select(String.format(":has(a[href='%s'])", bookUrl)).first();
			
			final Book book = strategy.extractBook(bookCard, bookPage);
			final PricePoint pricePoint = strategy.extractPricePoint(bookCard, Locale.forLanguageTag("ro-ro"), retrievedTime);

			List<Book> books = findBookByProperties(book);
			Key<PricePoint> priceKey = ObjectifyService.ofy().save().entity(pricePoint).now();
			
			book.getPricepoints().add(priceKey);	
			if (books.isEmpty()) {
				++inserted;
				ObjectifyService.ofy().save().entity(book);
				logger.info("Saving new {} to db.", book);
			} else {
				++updated;

				Book persistedBook = books.get(0);
				Optional<Book> mergedBook = Book.merge(persistedBook, book);

				if (mergedBook.isPresent()) {
					Book bookToPersist = mergedBook.get();
					ObjectifyService.ofy().delete().entities(persistedBook);
					ObjectifyService.ofy().save().entities(bookToPersist);
					logger.info("Updating book to {} in db.", mergedBook.get());
				}
			}
		}
		logger.info("Found {}/{} books on {} : {} inserted, {} updated.", bookCards.size(), singleBookUrls.size(),
				multiBookPage.absUrl("href"), inserted, updated);
	}

	public Elements getBookCards() {
		String bookSelector = "[class*='produ']:has(img):has(a)";
		return multiBookPage.select(String.format("%s:not(:has(%s))", bookSelector, bookSelector));
	}

	private Optional<Document> tryToGetPage(String url) {
		final int MAX_TRIES = 2;
		Document bookPage = null;
		for (int i = 0; i < MAX_TRIES; ++i) {
			try {
				bookPage = HtmlUtil.sanitizeHtml(Jsoup.connect(url).get());
				TimeUnit.MILLISECONDS.sleep(this.crawlRules.getCrawlDelay());
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
	
	/**
	 * @return Appropriate strategy for current context
	 */
	private InformationExtractionStrategy chooseStrategy() {
		WebWrapper wrapper = getWrapperForSite(this.site);
		return new HeuristicalStrategy();
	}

	/*
	private Key<WebWrapper> persistNewWrapper(WrapperGenerationStrategy wrapperGenerator) {
		Element anySingleBookPage = singleBookUrls.values().iterator().next();
		WebWrapper wrapper = wrapperGenerator.generateWrapper(anySingleBookPage, new Elements(multiBookPage));
		
		logger.info("Adding wrapper {}", wrapper);
		return ObjectifyService.ofy().save().entity(wrapper).now();
	}*/

	/**
	 * @param candidate
	 * @return A list of books containing either one with the same isbn, or other
	 *         books that have the same name.
	 */
	private List<Book> findBookByProperties(Book candidate) {
		final int MAX_BOOKS_RETURNED = 10;
		
		LoadType<Book> bookLoader = ObjectifyService.ofy().load().type(Book.class);
		if (candidate.getIsbn() != null)
			return bookLoader.filter("isbn", candidate.getIsbn()).list();
		else
			return bookLoader.filter("title", candidate.getTitle()).limit(MAX_BOOKS_RETURNED).list();
	}

	private WebWrapper getWrapperForSite(String site) {
		Preconditions.checkNotNull(site);
		
		return ObjectifyService.ofy().load().type(WebWrapper.class).filter("site", site).first().now();
	}
}
