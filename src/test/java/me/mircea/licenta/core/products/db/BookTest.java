package me.mircea.licenta.core.products.db;

import me.mircea.licenta.products.db.model.Book;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

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
	
		Book merged = Book.merge(persisted, addition);
		assertEquals("Si mai multa limba de lemn", merged.getDescription());
		
		addition.setIsbn("1234567890123");
		
		merged = Book.merge(persisted, addition);
		assertEquals("1234567890123", merged.getIsbn());
		
		persisted.setIsbn("0987654321098");
		merged = Book.merge(persisted, addition);
	}
}
