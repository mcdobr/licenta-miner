package me.mircea.licenta.core.infoextraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mircea.licenta.core.entities.Book;
import me.mircea.licenta.core.entities.WebWrapper;
import me.mircea.licenta.core.parser.utils.HtmlUtil;

public class HeuristicalStrategyTest {
	private static final ClassLoader classLoader = HeuristicalStrategyTest.class.getClassLoader();
	private static final Logger logger = LoggerFactory.getLogger(HeuristicalStrategyTest.class);

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
	
	InformationExtractionStrategy extractionStrategy = new HeuristicalStrategy();
	
	@Before
	public void setUp() throws IOException {
		alexandriaContent = HtmlUtil.sanitizeHtml(Jsoup.parse(alexandriaBookPageFile, "UTF-8", alexandriaUrl));
		carturestiContent = HtmlUtil.sanitizeHtml(Jsoup.parse(carturestiBookPageFile, "UTF-8", carturestiUrl));
		librisContent = HtmlUtil.sanitizeHtml(Jsoup.parse(librisBookPageFile, "UTF-8", librisUrl));
		
		alexandriaMainContent = HtmlUtil.extractMainContent(Jsoup.parse(alexandriaBookPageFile, "UTF-8", alexandriaUrl));
		carturestiMainContent = HtmlUtil.extractMainContent(Jsoup.parse(carturestiBookPageFile, "UTF-8", carturestiUrl));
		librisMainContent = HtmlUtil.extractMainContent(Jsoup.parse(librisBookPageFile, "UTF-8", librisUrl));
	}
	
	
	@Test
	public void shouldExtractAuthors()
	{
		assertEquals("Claude Karnoouh", extractionStrategy.extractAuthors(alexandriaMainContent));
		assertEquals("Jon Kalman Stefansson", extractionStrategy.extractAuthors(carturestiMainContent));
		assertEquals("Simona Tivadar", extractionStrategy.extractAuthors(librisMainContent));
	}
	
	@Test
	public void shouldExtractElementsFromDownloadedMultiPage() throws IOException {
		final URL resource = classLoader.getResource("bookGridAlexandria.html");
		assertNotNull(resource);

		File inputFile = new File(resource.getFile());
		assertTrue(inputFile.exists());

		Document doc = Jsoup.parse(inputFile, "UTF-8", "http://www.librariilealexandria.ro/carte");

		Elements productElements = extractionStrategy.extractBookCards(doc);
		assertNotNull(productElements);
		assertTrue(2000 <= productElements.size());
	}

	@Test
	public void shouldExtractAttributes() throws IOException {
		Document doc = HtmlUtil.sanitizeHtml(
				Jsoup.connect("https://carturesti.ro/carte/mindhalalig-szinesz-153594?p=7744").get());

		
		final URL resource = classLoader.getResource("bookCardCarturesti.html");
		File inputFile = new File(resource.getFile());
		Element htmlElement = Jsoup.parse(inputFile, "UTF-8");

		Book book = extractionStrategy.extractBook(htmlElement, doc);
		assertNotNull(book.getIsbn());
		assertNotEquals(book.getIsbn().trim(), "");
	}


	
	
	@Test
	public void shouldCreateAppropriateWrapperOnAlexandria() throws IOException {
		String url = "http://www.librariilealexandria.ro/elita-din-umbra";
		Element mainContent = HtmlUtil.extractMainContent(Jsoup.connect(url).get());
		WrapperGenerationStrategy strategy = new HeuristicalStrategy();
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

		WrapperGenerationStrategy strategy = new HeuristicalStrategy();
		WebWrapper wrapper = strategy.generateWrapper(mainContent, additionals);

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
		
		WrapperGenerationStrategy strategy = new HeuristicalStrategy();
		WebWrapper wrapper = strategy.generateWrapper(mainContent);

		logger.info("Libris: {}", wrapper.toString());
		assertEquals("#product_title", wrapper.getTitleSelector());
		assertEquals("#price", wrapper.getPriceSelector());
		assertEquals("#text_container>p", wrapper.getAttributeSelector());
	}
}
