package me.mircea.licenta.core.scraper.infoextraction;

import me.mircea.licenta.core.parser.utils.HtmlUtil;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;
import me.mircea.licenta.scraper.infoextraction.BookExtractor;
import me.mircea.licenta.scraper.infoextraction.HeuristicalBookExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import static org.junit.Assert.*;

public class HeuristicalBookExtractorTest {
	private static final ClassLoader classLoader = HeuristicalBookExtractorTest.class.getClassLoader();
	private static final Logger logger = LoggerFactory.getLogger(HeuristicalBookExtractorTest.class);

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

	static Element cartepediaPage;
	
	BookExtractor extractionStrategy = new HeuristicalBookExtractor();
	
	@Before
	public void setUp() throws IOException {
		alexandriaContent = HtmlUtil.sanitizeHtml(Jsoup.parse(alexandriaBookPageFile, "UTF-8", alexandriaUrl));
		carturestiContent = HtmlUtil.sanitizeHtml(Jsoup.parse(carturestiBookPageFile, "UTF-8", carturestiUrl));
		librisContent = HtmlUtil.sanitizeHtml(Jsoup.parse(librisBookPageFile, "UTF-8", librisUrl));
		
		alexandriaMainContent = HtmlUtil.extractMainContent(Jsoup.parse(alexandriaBookPageFile, "UTF-8", alexandriaUrl));
		carturestiMainContent = HtmlUtil.extractMainContent(Jsoup.parse(carturestiBookPageFile, "UTF-8", carturestiUrl));
		librisMainContent = HtmlUtil.extractMainContent(Jsoup.parse(librisBookPageFile, "UTF-8", librisUrl));

		cartepediaPage = Jsoup.connect("https://www.cartepedia.ro/carte/fictiune-literatura/literatura-contemporana/josh-malerman/bird-box-orbeste-58781.html")
				.get();
	}
	

	@Test
	public void shouldExtractTitles() {
		assertEquals("Inima omului", extractionStrategy.extractTitle(carturestiContent));
		assertEquals("Medicina, nutritie si buna dispozitie - Simona Tivadar", extractionStrategy.extractTitle(librisContent));
		assertEquals("Pentru o genealogie a globalizării", extractionStrategy.extractTitle(alexandriaContent));
		assertEquals("Bird Box. Orbește", extractionStrategy.extractTitle(cartepediaPage));
	}

	/*
	@Test
	public void shouldExtractAuthors() {
		assertEquals("Claude Karnoouh", extractionStrategy.extractAuthors(alexandriaMainContent));
		assertEquals("Jon Kalman Stefansson", extractionStrategy.extractAuthors(carturestiMainContent));
		assertEquals("Simona Tivadar", extractionStrategy.extractAuthors(librisMainContent));
		assertEquals("Josh Malerman", extractionStrategy.extractAuthors(cartepediaPage));
	}
	*/

	@Test
	public void shouldExtractImages() {
		assertEquals("http://www.librariilealexandria.ro/image/cache/catalog/produse/carti/Filosofie%20politica/Pentru%20o%20genealogie%202017-480x480.jpg", extractionStrategy.extractImageUrl(alexandriaContent));
		assertEquals("https://cdn.dc5.ro/img/prod/171704655-0.jpeg", extractionStrategy.extractImageUrl(carturestiContent));
		assertEquals("https://www.libris.ro/img/pozeprod/59/1002/16/1250968.jpg", extractionStrategy.extractImageUrl(librisContent));
		assertEquals("https://static.cartepedia.ro/image/228717/bird-box-orbeste-produs_imagine.jpg", extractionStrategy.extractImageUrl(cartepediaPage));
	}

	@Test
	public void shouldExtractAttributes() throws IOException {
		Document doc = HtmlUtil.sanitizeHtml(
				Jsoup.connect("https://carturesti.ro/carte/mindhalalig-szinesz-153594?p=7744").get());

		
		final URL resource = classLoader.getResource("bookCardCarturesti.html");
		File inputFile = new File(resource.getFile());
		Element htmlElement = Jsoup.parse(inputFile, "UTF-8");

		Book book = (Book)extractionStrategy.extract(doc);
		assertNotNull(book.getIsbn());
		assertNotEquals(book.getIsbn().trim(), "");
	}

	/*
	@Test
	public void shouldExtractFormats() {
		assertTrue("paperback".equalsIgnoreCase(extractionStrategy.extractFormat(alexandriaMainContent)));
		assertTrue("paperback".equalsIgnoreCase(extractionStrategy.extractFormat(carturestiMainContent)));
		assertTrue("paperback".equalsIgnoreCase(extractionStrategy.extractFormat(librisMainContent)));
	}
	
	@Test
	public void shouldExtractPublishers() {

		assertEquals("Alexandria Publishing House", extractionStrategy.extractPublisher(alexandriaMainContent));	
		assertEquals("Polirom", extractionStrategy.extractPublisher(carturestiMainContent));
		assertEquals("HUMANITAS", extractionStrategy.extractPublisher(librisMainContent));
	} */

	@Test
	public void shouldExtractPrices() throws IOException {
		PricePoint price;
		
		price = extractionStrategy.extractPricePoint(alexandriaMainContent, Locale.forLanguageTag("ro-ro"));
		assertEquals(39.00, price.getNominalValue().doubleValue(), 1e-5);
		
		price = extractionStrategy.extractPricePoint(carturestiMainContent, Locale.forLanguageTag("ro-ro"));
		assertEquals(41.95, price.getNominalValue().doubleValue(), 1e-5);
		
		price = extractionStrategy.extractPricePoint(librisMainContent, Locale.forLanguageTag("ro-ro"));
		assertEquals(32.55, price.getNominalValue().doubleValue(), 1e-5);
	}
	
	/*
	@Test
	public void shouldCreateAppropriateWrapperOnAlexandria() throws IOException {
		String url = "http://www.librariilealexandria.ro/elita-din-umbra";
		Element mainContent = HtmlUtil.extractMainContent(Jsoup.connect(url).get());
		WrapperGenerator strategy = new HeuristicalBookExtractor();
		WebWrapper wrapper = strategy.generateWrapper(mainContent);

		logger.info("Alexandria: {}", wrapper.toString());
		
		assertEquals(".product-name", wrapper.getTitleSelector());
		assertEquals(".product-author", wrapper.getAuthorsSelector());
		assertEquals(".big-text>b", wrapper.getPriceSelector());
		//logger.info(wrapper.getAttributeSelector());
	}
	
	@Test
	public void shouldCreateAppropriateWrapperOnCarturesti() throws IOException {
		String url = "https://carturesti.ro/carte/pedaland-prin-viata-181658144?p=2";
		Element mainContent = HtmlUtil.extractMainContent(Jsoup.connect(url).get());

		final URL resource = classLoader.getResource("heuristicGridMock.html");
		File inputFile = new File(resource.getFile());
		
		Elements additionals = new Elements();
		additionals.add(Jsoup.parse(inputFile, "UTF-8"));

		WrapperGenerator strategy = new HeuristicalBookExtractor();
		Wrapper wrapper = strategy.generateWrapper(mainContent, additionals);

		logger.info("Carturesti: {}", wrapper.toString());

		assertEquals(".titluProdus", wrapper.getTitleSelector());
		assertEquals(".autorProdus", wrapper.getAuthorsSelector());
		assertEquals(".pret", wrapper.getPriceSelector());
		assertEquals(".productAttr", wrapper.getAttributeSelector());
		assertEquals(".product-grid-container", wrapper.getBookCardSelector());
	}

	@Test
	public void shouldCreateAppropriateWrapperOnLibris() throws IOException {
		String url = "https://www.libris.ro/naufragii-akira-yoshimura-HUM978-606-779-038-2--p1033264.html";
		Element mainContent = HtmlUtil.extractMainContent(Jsoup.connect(url).get());
		
		// TODO: add mock grid page to extract book cards
		
		WrapperGenerator strategy = new HeuristicalBookExtractor();
		Wrapper wrapper = strategy.generateWrapper(mainContent);

		logger.info("Libris: {}", wrapper.toString());
		assertEquals("#product_title", wrapper.getTitleSelector());
		assertEquals("#price", wrapper.getPriceSelector());
		assertEquals("#text_container>p", wrapper.getAttributeSelector());
	} */

	@Test
	public void shouldExtractIsbn() throws IOException {
		Document doc = Jsoup.connect("https://carturesti.ro/carte/noul-cod-civil-studii-si-comentarii-vol-al-iii-lea-p-i-art-1164-1649-239686").get();

		Book book = (Book)extractionStrategy.extract(HtmlUtil.sanitizeHtml(doc));

		assertEquals("9786066733632", book.getIsbn());


		//assertEquals("9786068564203", (Book)(extractionStrategy.extract(HtmlUtil.sanitizeHtml(Jsoup.connect("http://www.librariilealexandria.ro/alexander-marguier-lux-lexicon").get())
		//).getIsbn());
	}
}
