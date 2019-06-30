package me.mircea.licenta.products.db.impl;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;

import java.util.Currency;

public class CurrencyTranslatorFactory extends SimpleTranslatorFactory<Currency, String> {
    public CurrencyTranslatorFactory() {
        super(Currency.class, ValueType.STRING);
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
