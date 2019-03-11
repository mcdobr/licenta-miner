package me.mircea.licenta.core.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Locale;

import me.mircea.licenta.products.db.PricePoint;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

public class PricePointTest {
	final String mockUrl = "http://www.dummyUrl.com";
	final Instant mockInstant = Instant.now();
	
	@Test
	public void shouldBeEqual() throws MalformedURLException {
		Currency ron = Currency.getInstance(Locale.forLanguageTag("ro-ro"));
		Currency ron2 = Currency.getInstance(Locale.forLanguageTag("ro-ro"));
		assertEquals(ron, ron2);
		
		
		PricePoint p1 = new PricePoint(1L, BigDecimal.valueOf(20.05), ron, mockInstant, mockUrl);
		PricePoint p2 = new PricePoint(2L, BigDecimal.valueOf(20.05), ron, mockInstant, mockUrl);
		assertEquals(p1, p2);
		
		PricePoint p3 = new PricePoint(2L, BigDecimal.valueOf(20.05), ron, mockInstant.plus(1L, ChronoUnit.DAYS), mockUrl);
		assertNotEquals(p1, p3);
	}
	
	@Test
	public void shouldParseCorrectly() throws ParseException, MalformedURLException {
		Locale locale = Locale.forLanguageTag("ro-ro");
		final double delta = 1e-5;
		
		String text = "<div data-ng-if=\"::product.stockStatus.slug != 'indisponibil' &amp;&amp; product.stockStatus.slug != 'promo'\" data-ng-bind-html=\"::h.formatPrice(product.price)\" class=\"productPrice ng-binding ng-scope discountPrice\" data-ng-class=\"::product.discount?'discountPrice':''\"><span class=\"suma\" itemprop=\"price\" content=\"16.99\">16</span><span class=\"bani\">99</span><span class=\"priceCurrency\" content=\"RON\">lei</span></div>";
		Element priceElement = Jsoup.parse(text);
		PricePoint price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(16.99, price.getNominalValue().doubleValue(), delta);
	
		text = "<span class=\"pret\">19.2lei <del>24lei</del></span>";
		priceElement = Jsoup.parse(text);
		price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(19.2, price.getNominalValue().doubleValue(), delta);
		
		text = "<span class=\"pret\">10.79lei <del>27lei</del></span>";
		priceElement = Jsoup.parse(text);
		price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(10.79, price.getNominalValue().doubleValue(), delta);
		
		text = "<span class=\"pret\">28lei <del>35lei</del></span>";
		priceElement = Jsoup.parse(text);
		price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(28.0, price.getNominalValue().doubleValue(), delta);
	}
	@Test
	public void shouldParseCorrectlyOnEdgeCases() throws ParseException, MalformedURLException {
		Locale locale = Locale.forLanguageTag("ro-ro");
		final double delta = 1e-5;
		
		String text = "<span class=\"pret\">1.5&nbsp;lei</span>";
		Element priceElement = Jsoup.parse(text);
		PricePoint price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(1.5, price.getNominalValue().doubleValue(), delta);
		
		text = "<span class=\"pret\">2,780.5&nbsp;lei</span>";
		priceElement = Jsoup.parse(text);
		price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(2780.5, price.getNominalValue().doubleValue(), delta);
		
		text = "<span class=\"pret\">1,260&nbsp;lei</span>";
		priceElement = Jsoup.parse(text);
		price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(1260.0, price.getNominalValue().doubleValue(), delta);
		
		text = "<div data-ng-if=\"::product.stockStatus.slug != 'indisponibil' &amp;&amp; product.stockStatus.slug != 'promo'\" data-ng-bind-html=\"::h.formatPrice(product.price)\" class=\"productPrice ng-binding ng-scope\" data-ng-class=\"::product.discount?'discountPrice':''\"><span class=\"suma\" itemprop=\"price\" content=\"2017.00\">2017</span><span class=\"bani\">00</span><span class=\"priceCurrency\" content=\"RON\">lei</span></div>";
		priceElement = Jsoup.parse(text);
		price = PricePoint.valueOf(priceElement.text(), locale, Instant.now(), mockUrl);
		assertEquals(2017.0, price.getNominalValue().doubleValue(), delta);
	}
}
