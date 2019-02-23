package me.mircea.licenta.db.products.impl;

import java.time.Instant;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

public class InstantTranslatorFactory extends ValueTranslatorFactory<Instant, Timestamp> {
	public InstantTranslatorFactory() {
		super(Instant.class);
	}

	@Override
	protected ValueTranslator<Instant, Timestamp> createValueTranslator(TypeKey<Instant> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<Instant, Timestamp>() {

			@Override
			protected Instant loadValue(Value<Timestamp> value, LoadContext ctx, Path path) { 
				return Instant.ofEpochSecond(value.get().getSeconds(), value.get().getNanos());
			}

			@Override
			protected Value<Timestamp> saveValue(Instant value, SaveContext ctx, Path path) {
				return TimestampValue.of(Timestamp.ofTimeSecondsAndNanos(value.getEpochSecond(), value.getNano()));
			}
		};
	}
}
