package me.mircea.licenta.products.db.model;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import me.mircea.licenta.core.parser.utils.HtmlUtil;

@Entity
public class PricePoint {
	@Id
	private Long id;
	private BigDecimal nominalValue;
	private Currency currency;

	@Index
	private Instant retrievedTime;
	private String url;
	private String pageTitle;
	// private Boolean available;
	@Index
	private String site;

	public PricePoint() {
		retrievedTime = Instant.now();
	}

	public PricePoint(Long id, BigDecimal nominalValue, Currency currency, Instant retrievedTime, String url)
			throws MalformedURLException {
		super();
		Preconditions.checkNotNull(retrievedTime);
		Preconditions.checkNotNull(url);
		this.id = id;
		this.nominalValue = nominalValue;
		this.currency = currency;
		this.retrievedTime = retrievedTime;
		this.url = url;
		this.site = HtmlUtil.getDomainOfUrl(url);
	}

	public PricePoint(BigDecimal nominalValue, Currency currency, Instant retrievedTime, String url)
			throws MalformedURLException {
		this(null, nominalValue, currency, retrievedTime, url);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BigDecimal getNominalValue() {
		return nominalValue;
	}

	public void setNominalValue(BigDecimal nominalValue) {
		this.nominalValue = nominalValue;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public Instant getRetrievedTime() {
		return retrievedTime;
	}

	public void setRetrievedTime(Instant retrievedTime) {
		this.retrievedTime = retrievedTime;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PricePoint [id=").append(id);
		builder.append(", nominalValue=").append(nominalValue);
		builder.append(", currency=").append(currency);
		builder.append(", retrievedTime=").append(retrievedTime);
		builder.append(", url=").append(url);
		builder.append(", pageTitle=").append(pageTitle);
		builder.append(", site=").append(site);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currency == null) ? 0 : currency.hashCode());
		result = prime * result + ((nominalValue == null) ? 0 : nominalValue.hashCode());
		result = prime * result + ((retrievedTime == null) ? 0 : retrievedTime.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((site == null) ? 0 : site.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PricePoint))
			return false;

		PricePoint other = (PricePoint) obj;
		if (currency == null) {
			if (other.currency != null)
				return false;
		} else if (!currency.equals(other.currency))
			return false;

		if (nominalValue == null) {
			if (other.nominalValue != null)
				return false;
		} else if (!nominalValue.equals(other.nominalValue))
			return false;

		if (retrievedTime == null) {
			if (other.retrievedTime != null)
				return false;
		} else if (!retrievedTime.equals(other.retrievedTime))
			return false;

		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	/**
	 * @brief Transforms a price tag string into a PricePoint. If say, a romanian
	 *        site uses the . (dot) decimal separator, this replaces all commas and
	 *        dots to the romanian default decimal separator (which is a comma). If
	 *        the price tag contains no decimal separator whatsoever, the last two
	 *        digits are considered to be cents. If it has more than two digits
	 *        after the normal decimal point, the function considers that a mistake
	 *        on part of the document and makes it right.
	 * @param price
	 *            The string representation of a price tag.
	 * @param locale
	 *            The locale considered for extracting the price tag.
	 * @param retrievedTime
	 *            The day when the price was read.
	 * @param site
	 * @return A pricepoint extracted from the string.
	 * @throws ParseException
	 *             if the String is not formatted according to the locale.
	 * @throws MalformedURLException
	 */
	public static PricePoint valueOf(String price, final Locale locale, Instant retrievedTime, String url)
			throws ParseException, MalformedURLException {
		price = normalizeStringWithLocale(price, locale);

		final NumberFormat noFormat = NumberFormat.getNumberInstance(locale);
		if (noFormat instanceof DecimalFormat) {
			((DecimalFormat) noFormat).setParseBigDecimal(true);
		}

		BigDecimal nominalValue = (BigDecimal) noFormat.parse(price);
		if (!price.matches(".*[.,].*") && nominalValue.stripTrailingZeros().scale() <= 0
				&& nominalValue.compareTo(BigDecimal.valueOf(100)) >= 1)
			nominalValue = nominalValue.divide(BigDecimal.valueOf(100));

		return new PricePoint(null, nominalValue, Currency.getInstance(locale), retrievedTime, url);
	}

	/**
	 * Does the actual fixing of the price tag.
	 * 
	 * @param price
	 * @param locale
	 * @return
	 */
	private static String normalizeStringWithLocale(String price, Locale locale) {
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
		String normalDecimalSeparator = String.valueOf(symbols.getDecimalSeparator());
		String normalGroupingSeparator = String.valueOf(symbols.getGroupingSeparator());

		// If a mismatch between locale and website, try and fix it
		final int decimalFirst = price.indexOf(normalDecimalSeparator);
		final int groupingFirst = price.indexOf(normalGroupingSeparator);

		final boolean hasNormalDecimalSeparator = (decimalFirst != -1);
		final boolean hasNormalGroupingSeparator = (groupingFirst != -1);
		final boolean hasBothButReversed = hasNormalDecimalSeparator && hasNormalGroupingSeparator
				&& groupingFirst > decimalFirst;
		if (!hasNormalDecimalSeparator) {
			price = price.replaceAll("[.,]", normalDecimalSeparator);
		} else if (!hasNormalGroupingSeparator) { // has decimal but no grouping
			String substring = price.substring(decimalFirst + 1);
			if (substring.matches("^\\d{3,}.*"))
				price = price.replaceAll(normalDecimalSeparator, normalGroupingSeparator);
		} else if (hasBothButReversed) {
			price = swapCharactersInString(price, normalDecimalSeparator.charAt(0), normalGroupingSeparator.charAt(0));
		}

		return price;
	}

	private static String swapCharactersInString(final String str, final char first, final char second) {
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			if (chars[i] == first)
				chars[i] = second;
			else if (chars[i] == second)
				chars[i] = first;

		}
		return String.valueOf(chars);
	}
}
