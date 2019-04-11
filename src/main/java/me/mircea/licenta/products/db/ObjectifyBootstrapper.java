package me.mircea.licenta.products.db;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import me.mircea.licenta.products.db.impl.CurrencyTranslatorFactory;
import me.mircea.licenta.products.db.impl.InstantTranslatorFactory;
import me.mircea.licenta.products.db.model.Book;
import me.mircea.licenta.products.db.model.PricePoint;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ObjectifyBootstrapper implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
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
    }
}
