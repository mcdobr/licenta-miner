package me.mircea.licenta.core.products.db;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.util.Closeable;
import me.mircea.licenta.products.db.impl.CurrencyTranslatorFactory;
import me.mircea.licenta.products.db.impl.InstantTranslatorFactory;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class BookTest {
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	private final String mockUrl = "http://www.dummy.com";
	private final Instant mockInstant = Instant.now();
	private final Currency mockCurrency = Currency.getInstance(Locale.forLanguageTag("ro-ro"));

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
	private Closeable session;

	@BeforeClass
	public static void setUpBeforeClass() {
		ObjectifyService.init(new ObjectifyFactory());
		ObjectifyService.factory().getTranslators().add(new InstantTranslatorFactory());
		ObjectifyService.factory().getTranslators().add(new CurrencyTranslatorFactory());
		ObjectifyService.factory().getTranslators().add(new BigDecimalLongTranslatorFactory(100));

		ObjectifyService.register(Book.class);
		ObjectifyService.register(PricePoint.class);
	}

	@Before
	public void setUp() {
		helper.setUp();
		session = ObjectifyService.begin();
	}

	@After
	public void tearDown() {
		helper.tearDown();
		session.close();
		session = null;
	}

	@Test
	public void shouldBeEqual() {
		Book book1 = new Book();
		Book book2 = new Book();
		
		assertEquals(book1, book2);
		
		book1.setIsbn("978-1234-1093-23");
		book2.setIsbn("978 -1234-1093-23");
		
		assertEquals(book1, book2);
	}

	@Test
	public void shouldMergeForMostInformation() {
		Book persisted = new Book(1L, "Anna Karenina", "Limba de lemn", "Lev Tolstoi");
		Book addition = new Book(null, "Anna Karenina", "Si mai multa limba de lemn", "Lev Tolstoi");
	
		Book merged = Book.merge(persisted, addition);
		assertEquals("Si mai multa limba de lemn", merged.getDescription());
		
		addition.setIsbn("1234567890123");
		
		merged = Book.merge(persisted, addition);
		assertEquals("1234567890123", merged.getIsbn());
		
		persisted.setIsbn("0987654321098");
		merged = Book.merge(persisted, addition);
	}

	@Test
	public void shouldMergeBestOffer() throws ParseException, MalformedURLException {
		PricePoint p1 = PricePoint.valueOf("10.05", Locale.forLanguageTag("ro-ro"), Instant.now(), "http://google.com");
		PricePoint p2 = PricePoint.valueOf("12.05", Locale.forLanguageTag("ro-ro"), Instant.now().plusSeconds(3600), "http://google.com");
		ObjectifyService.ofy().save().entities(p1, p2).now();

		Book b1 = new Book();
		Book b2 = new Book();

		b1.setBestCurrentOffer(p1);
		b2.setBestCurrentOffer(p2);

		Book merged = Book.merge(b1, b2);

		assertEquals(p2, merged.getBestCurrentOffer());
	}
}
