package me.mircea.licenta.core.infoextraction;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.mircea.licenta.core.entities.WebWrapper;

public interface WrapperGenerationStrategy {
	public WebWrapper generateWrapper(Element bookPage, Elements additionals);
	public default WebWrapper generateWrapper(Element bookPage) {
		return generateWrapper(bookPage, new Elements());
	}
	
	public String generateCssSelectorFor(Elements elements);
}
