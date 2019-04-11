package me.mircea.licenta.scraper.infoextraction;

import org.jsoup.nodes.Element;

public interface BookExtractor extends ProductExtractor {
    String extractTitle(Element htmlElement);

    String extractAuthors(Element htmlElement);

    String extractIsbn(Element htmlElement);

    String extractFormat(Element htmlElement);

    String extractPublisher(Element htmlElement);
}
