package com.github.quarkus;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.mongodb.client.MongoClient;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AppLifecycleBean {
	private static final Logger log = LoggerFactory.getLogger(AppLifecycleBean.class);

	@ConfigProperty(name = "configuration.initialLoad", defaultValue = "true")
	boolean loadMockedData;

	@Inject
	MongoClient mongoClient;

	void onStart(@Observes StartupEvent ev) {
		if (loadMockedData) {
			log.debug("MongoDB settings: {}", mongoClient.getClusterDescription());
			Company.count()
					.subscribe().with(i -> {
				log.debug("count: {}", i);
				if (i == 0) {
					Company company = new Company();
					company.name = "Facebook";
					company.createdByUser = "default@admin.com";
					Company company1 = new Company();
					company1.name ="Google";
					company1.createdByUser = "default@admin.com";
					Company company2 = new Company();
					company2.name ="Amazon";
					company2.createdByUser = "default@admin.com";
					Company.persist(Stream.of(company, company1, company2))
							.await()
							.indefinitely();
				}
			}, RuntimeException::new);
		}
	}
}
