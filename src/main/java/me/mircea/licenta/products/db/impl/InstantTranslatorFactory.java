package me.mircea.licenta.products.db.impl;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;

import java.time.Instant;


public class InstantTranslatorFactory extends SimpleTranslatorFactory<Instant, Timestamp> {

    public InstantTranslatorFactory() {
        super(Instant.class, ValueType.NULL, ValueType.LONG, ValueType.STRING, ValueType.TIMESTAMP);
    }


    @Override
    protected Instant toPojo(Value<Timestamp> value) {
        return Instant.ofEpochSecond(value.get().getSeconds(), value.get().getNanos());
    }

    @Override
    protected Value<Timestamp> toDatastore(Instant value) {
        return TimestampValue.of(Timestamp.ofTimeSecondsAndNanos(value.getEpochSecond(), value.getNano()));
    }
}
