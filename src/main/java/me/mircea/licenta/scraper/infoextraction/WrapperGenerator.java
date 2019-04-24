package me.mircea.licenta.scraper.infoextraction;

import me.mircea.licenta.core.crawl.db.model.Wrapper;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public interface WrapperGenerator {
	Wrapper generateWrapper(Element productPage, Elements additionals);

	default Wrapper generateWrapper(Element bookPage) {
		throw new UnsupportedOperationException("Not implemented yet");
		//return generateWrapper(bookPage, new Elements());
	}
	
	String generateCssSelectorFor(Elements elements);
}
