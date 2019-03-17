package me.mircea.licenta.products.db.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import me.mircea.licenta.core.parser.utils.Normalizer;

@Entity
public class Book {
	private static final Logger logger = LoggerFactory.getLogger(Book.class);
	
	@Id
	private Long id;
	private String title;
	private String authors;
	@Index
	private String isbn;
	@Index
	private Set<String> keywords;
	
	private String description;
	private List<Key<PricePoint>> pricepoints;
	private String publisher;
	private String format;
	private String imageUrl;

	private Instant latestRetrievedTime;
	private String latestRetrievedPrice;

	public Book() {
		this.pricepoints = new ArrayList<>();
		this.keywords = new HashSet<>();
	}

	public Book(Long id, String title, String description, String authors) {
		super();

		Preconditions.checkNotNull(authors);
		this.id = id;
		this.title = title;
		this.description = description;
		this.authors = authors;
		this.pricepoints = new ArrayList<>();
		this.keywords = new HashSet<>();
	}

	private Book(Book persisted, Book addition) {
		this();
		Preconditions.checkNotNull(persisted);
		Preconditions.checkNotNull(addition);
		
		
		this.id = persisted.id;
		this.title = (String)Normalizer.getNotNullIfPossible(persisted.title, addition.title);
		this.authors = Normalizer.getLongestOfNullableStrings(persisted.authors, addition.authors);
		this.isbn = (String)Normalizer.getNotNullIfPossible(persisted.isbn, addition.isbn);
		this.description = Normalizer.getLongestOfNullableStrings(persisted.description, addition.description);
		
		this.pricepoints = persisted.pricepoints;	
		this.pricepoints.addAll(addition.pricepoints);
		
		this.keywords = persisted.keywords;
		this.keywords.addAll(addition.keywords);
		
		this.publisher = (String)Normalizer.getNotNullIfPossible(persisted.publisher, addition.publisher);
		this.format = (String) Normalizer.getNotNullIfPossible(persisted.format, addition.format);
		this.imageUrl = (String)Normalizer.getNotNullIfPossible(persisted.imageUrl, addition.imageUrl);
	}
	
	/**
	 * @param persisted
	 * @param addition
	 * @return An object resulted from a merger that strives for completeness.
	 */
	public static Book merge(Book persisted, Book addition) {
		return new Book(persisted, addition);
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = (isbn != null) ? isbn.replaceAll("[-\\ ]", "") : null;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Key<PricePoint>> getPricepoints() {
		return pricepoints;
	}

	public void setPricepoints(List<Key<PricePoint>> pricepoints) {
		this.pricepoints = pricepoints;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Instant getLatestRetrievedTime() {
		return latestRetrievedTime;
	}

	public void setLatestRetrievedTime(Instant latestRetrievedTime) {
		this.latestRetrievedTime = latestRetrievedTime;
	}

	public String getLatestRetrievedPrice() {
		return latestRetrievedPrice;
	}

	public void setLatestRetrievedPrice(String latestRetrievedPrice) {
		this.latestRetrievedPrice = latestRetrievedPrice;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Book [id=").append(id);
		builder.append(", title=").append(title);
		builder.append(", authors=").append(authors);
		builder.append(", isbn=").append(isbn);
		builder.append(", description=").append(description != null);
		builder.append(", pricepoints=").append(pricepoints != null);
		builder.append(", keywords=").append(keywords);
		builder.append(", publisher=").append(publisher);
		builder.append(", format=").append(format);
		builder.append(", imageUrl=").append(imageUrl);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authors == null) ? 0 : authors.hashCode());
		result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((format == null) ? 0 : format.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isbn == null) ? 0 : isbn.hashCode());
		result = prime * result + ((pricepoints == null) ? 0 : pricepoints.hashCode());
		result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result + ((publisher == null) ? 0 : publisher.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Book)) {
			return false;
		}
		Book other = (Book) obj;
		if (authors == null) {
			if (other.authors != null) {
				return false;
			}
		} else if (!authors.equals(other.authors)) {
			return false;
		}
		if (imageUrl == null) {
			if (other.imageUrl != null) {
				return false;
			}
		} else if (!imageUrl.equals(other.imageUrl)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (format == null) {
			if (other.format != null) {
				return false;
			}
		} else if (!format.equals(other.format)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (isbn == null) {
			if (other.isbn != null) {
				return false;
			}
		} else if (!isbn.equals(other.isbn)) {
			return false;
		}
		if (pricepoints == null) {
			if (other.pricepoints != null) {
				return false;
			}
		} else if (!pricepoints.equals(other.pricepoints)) {
			return false;
		}
		if (keywords == null) {
			if (other.keywords != null) {
				return false;
			}
		} else if (!keywords.equals(other.keywords)) {
			return false;
		}
		if (publisher == null) {
			if (other.publisher != null) {
				return false;
			}
		} else if (!publisher.equals(other.publisher)) {
			return false;
		}
		if (title == null) {
			if (other.title != null) {
				return false;
			}
		} else if (!title.equals(other.title)) {
			return false;
		}
		return true;
	}
}
