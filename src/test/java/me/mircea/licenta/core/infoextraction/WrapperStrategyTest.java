package me.mircea.licenta.core.infoextraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mircea.licenta.core.entities.PricePoint;
import me.mircea.licenta.core.entities.WebWrapper;
import me.mircea.licenta.core.parser.utils.HtmlUtil;

public class WrapperStrategyTest {
	static final Logger logger = LoggerFactory.getLogger(WrapperStrategyTest.class);
	
	static final ClassLoader classLoader = WrapperStrategyTest.class.getClassLoader();
	static final File alexandriaBookPageFile = new File(classLoader.getResource("bookPageAlexandria.html").getFile());
	static final File carturestiBookPageFile = new File(classLoader.getResource("bookPageCarturesti.html").getFile());
	static final File librisBookPageFile = new File(classLoader.getResource("bookPageLibris.html").getFile());
	
	static final String alexandriaUrl = "http://www.librariilealexandria.ro/";	
	static final String carturestiUrl = "https://carturesti.ro/";
	static final String librisUrl = "https://www.libris.ro/";
	
	static Element alexandriaContent;
	static Element carturestiContent;
	static Element librisContent;
	
	static Element alexandriaMainContent;
	static Element carturestiMainContent;
	static Element librisMainContent;
	
	static RuleBasedStrategy donor;
	
	@Before
	public void setUp() throws IOException {
		alexandriaContent = HtmlUtil.sanitizeHtml(Jsoup.parse(alexandriaBookPageFile, "UTF-8", alexandriaUrl));
		carturestiContent = HtmlUtil.sanitizeHtml(Jsoup.parse(carturestiBookPageFile, "UTF-8", carturestiUrl));
		librisContent = HtmlUtil.sanitizeHtml(Jsoup.parse(librisBookPageFile, "UTF-8", librisUrl));
		
		alexandriaMainContent = HtmlUtil.extractMainContent(Jsoup.parse(alexandriaBookPageFile, "UTF-8", alexandriaUrl));
		carturestiMainContent = HtmlUtil.extractMainContent(Jsoup.parse(carturestiBookPageFile, "UTF-8", carturestiUrl));
		librisMainContent = HtmlUtil.extractMainContent(Jsoup.parse(librisBookPageFile, "UTF-8", librisUrl));
		
		donor = new HeuristicalStrategy();
	}
	
	@Test
	public void shouldExtractBookCards() throws IOException {
		Element multiPageContent = HtmlUtil.sanitizeHtml(Jsoup.connect("https://www.libris.ro/carti").get());

		WebWrapper wrapper = donor.generateWrapper(librisMainContent,
				new Elements(Jsoup.parseBodyFragment(multiPageContent.outerHtml())));
		InformationExtractionStrategy strategy = new WrapperStrategy(wrapper);

		// TODO: fix your damn interface
		Document dummyDoc = Jsoup.parseBodyFragment(multiPageContent.outerHtml());

		Elements bookCards = dummyDoc.select(wrapper.getBookCardSelector());
		assertTrue(40 <= bookCards.size());
		logger.info("Wrapper: {}", wrapper.toString());
	}

	@Test
	public void shouldExtractTitles()
	{
		InformationExtractionStrategy strategy;
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaMainContent));
		assertEquals("Pentru o genealogie a globalizării", strategy.extractTitle(alexandriaMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(carturestiMainContent));
		assertEquals("Inima omului", strategy.extractTitle(carturestiMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(librisMainContent));
		assertEquals("Medicina, nutritie si buna dispozitie - Simona Tivadar", strategy.extractTitle(librisMainContent));
	}
	
	@Test
	public void shouldExtractImages()
	{
		InformationExtractionStrategy strategy;
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaContent));
		assertEquals("http://www.librariilealexandria.ro/image/cache/catalog/produse/carti/Filosofie%20politica/Pentru%20o%20genealogie%202017-480x480.jpg", strategy.extractImageUrl(alexandriaContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(carturestiContent));
		assertEquals("https://cdn.dc5.ro/img/prod/171704655-0.jpeg", strategy.extractImageUrl(carturestiContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(librisContent));
		assertEquals("https://www.libris.ro/img/pozeprod/59/1002/16/1250968.jpg", strategy.extractImageUrl(librisContent));
	
	}
	
	@Test
	public void shouldExtractAuthors()
	{
		InformationExtractionStrategy strategy; 
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaMainContent));
		assertEquals("Claude Karnoouh", strategy.extractAuthors(alexandriaMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(carturestiMainContent));
		assertEquals("Jon Kalman Stefansson", strategy.extractAuthors(carturestiMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(librisMainContent));
		assertEquals("Simona Tivadar", strategy.extractAuthors(librisMainContent));
	}
	
	@Test
	public void shouldExtractDescriptions()
	{
		InformationExtractionStrategy strategy;
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaMainContent));
		fail();
		//assertNotNull(strategy.extractDescription(alexandriaMainContent);
	}
	
	@Test
	public void shouldExtractFormats()
	{
		InformationExtractionStrategy strategy; 
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaMainContent));
		assertEquals("Paperback", strategy.extractFormat(alexandriaMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(carturestiMainContent));
		assertEquals("paperback", strategy.extractFormat(carturestiMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(librisMainContent));
		assertEquals("paperback", strategy.extractFormat(librisMainContent));
	}
	
	@Test
	public void shouldExtractPublishers()
	{
		InformationExtractionStrategy strategy; 
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaMainContent));
		assertEquals("Alexandria Publishing House", strategy.extractPublisher(alexandriaMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(carturestiMainContent));
		assertEquals("Polirom", strategy.extractPublisher(carturestiMainContent));
		
		strategy = new WrapperStrategy(donor.generateWrapper(librisMainContent));
		assertEquals("HUMANITAS", strategy.extractPublisher(librisMainContent));
	}

	@Test
	public void shouldExtractPrices() throws IOException {
		InformationExtractionStrategy strategy;
		PricePoint price;
		
		strategy = new WrapperStrategy(donor.generateWrapper(alexandriaMainContent));
		price = strategy.extractPricePoint(alexandriaMainContent, Locale.forLanguageTag("ro-ro"));
		assertEquals(39.00, price.getNominalValue().doubleValue(), 1e-5);
		
		strategy = new WrapperStrategy(donor.generateWrapper(carturestiMainContent));
		price = strategy.extractPricePoint(carturestiMainContent, Locale.forLanguageTag("ro-ro"));
		assertEquals(41.95, price.getNominalValue().doubleValue(), 1e-5);
		
		strategy = new WrapperStrategy(donor.generateWrapper(librisMainContent));
		price = strategy.extractPricePoint(librisMainContent, Locale.forLanguageTag("ro-ro"));
		assertEquals(32.55, price.getNominalValue().doubleValue(), 1e-5);
	}
}
