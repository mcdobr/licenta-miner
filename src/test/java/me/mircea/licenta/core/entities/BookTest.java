package me.mircea.licenta.core.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BookTest {
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	private final String mockUrl = "http://www.dummy.com";
	private final Instant mockInstant = Instant.now();
	private final Currency mockCurrency = Currency.getInstance(Locale.forLanguageTag("ro-ro"));
	
	@Test
	public void shouldBeEqual() throws MalformedURLException {
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
	
		Optional<Book> mergeOperation = Book.merge(persisted, addition);
		assertTrue(mergeOperation.isPresent());	
		Book merged = mergeOperation.get();
		assertEquals("Si mai multa limba de lemn", merged.getDescription());
		
		addition.setIsbn("1234567890123");
		
		mergeOperation = Book.merge(persisted, addition);
		assertTrue(mergeOperation.isPresent());	
		merged = mergeOperation.get();
		assertEquals("1234567890123", merged.getIsbn());
		
		persisted.setIsbn("0987654321098");
		mergeOperation = Book.merge(persisted, addition);
		assertFalse(mergeOperation.isPresent());
	}
	
	/** TODO: change this logic maybe
	@Test
	public void shouldHaveOnlyOnePricepointPerSiteDay() throws MalformedURLException {
		Book persisted = new Book(1L, "Anna Karenina", "Limba de lemn", Arrays.asList("Lev Tolstoi"));
		Book addition = new Book(null, "Anna Karenina", "Si mai multa limba de lemn", Arrays.asList("Lev Tolstoi"));
		
		PricePoint p1 = new PricePoint(BigDecimal.valueOf(30.53), mockCurrency, mockInstant, mockUrl);
		PricePoint p2 = new PricePoint(BigDecimal.valueOf(30.53), mockCurrency, mockInstant, mockUrl);

		PricePoint p3 = new PricePoint(BigDecimal.valueOf(30.53), mockCurrency, mockInstant.plus(1, ChronoUnit.DAYS), mockUrl);
		
		
		
		persisted.getPricepoints().add(p1);
		persisted.getPricepoints().add(p3);
		addition.getPricepoints().add(p2);
		
		Optional<Book> mergeOperation = Book.merge(persisted, addition);
		assertTrue(mergeOperation.isPresent());
		Book merged = mergeOperation.get();
		
		assertEquals(2, merged.getPricepoints().size());
	} */
}
