package me.mircea.licenta.miner;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;

import me.mircea.licenta.core.entities.Book;
import me.mircea.licenta.core.entities.PricePoint;
import me.mircea.licenta.core.entities.WebWrapper;
import me.mircea.licenta.db.products.impl.CurrencyTranslatorFactory;
import me.mircea.licenta.db.products.impl.InstantTranslatorFactory;

class ProductDatabaseManager {
	static {
		Datastore datastore = DatastoreOptions.newBuilder()
				.setProjectId("bookworm-221210")
				.build()
				.getService();
		ObjectifyService.init(new ObjectifyFactory(datastore));
		
		ObjectifyService.factory().getTranslators().add(new InstantTranslatorFactory());
		ObjectifyService.factory().getTranslators().add(new CurrencyTranslatorFactory());
		ObjectifyService.factory().getTranslators().add(new BigDecimalLongTranslatorFactory(100));
		
		ObjectifyService.register(Book.class);
		ObjectifyService.register(PricePoint.class);
		ObjectifyService.register(WebWrapper.class);
	}
	
	private ProductDatabaseManager() {
	}
}
