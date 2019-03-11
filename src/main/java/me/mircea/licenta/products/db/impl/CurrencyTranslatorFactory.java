package me.mircea.licenta.products.db.impl;

import java.util.Currency;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;

public class CurrencyTranslatorFactory extends SimpleTranslatorFactory<Currency, String> {
	public CurrencyTranslatorFactory() {
		super(Currency.class);
	}

	@Override
	protected Currency toPojo(final Value<String> value) {
		return Currency.getInstance(value.get());
	}

	@Override
	protected Value<String> toDatastore(final Currency value) {
		return StringValue.of(value.getCurrencyCode());
	}
}
