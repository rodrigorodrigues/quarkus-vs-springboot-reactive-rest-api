package com.github.springboot;

import java.io.File;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.github.springboot.model.Company;
import com.github.springboot.repository.CompanyRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Slf4j
@SpringBootApplication
public class SpringBootServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(SpringBootServiceApplication.class)
            .initializers(new InitPublicKeyConfiguration())
            .run(args);
    }

    static class InitPublicKeyConfiguration implements ApplicationContextInitializer<GenericReactiveWebApplicationContext> {

        @SneakyThrows
        @Override
        public void initialize(GenericReactiveWebApplicationContext applicationContext) {
            File privateKeyFile = new File(System.getProperty("java.io.tmpdir"), "privateKey.pem");
            File publicKeyFile = new File(System.getProperty("java.io.tmpdir"), "publicKey.pem");

            if (!privateKeyFile.exists() || privateKeyFile.length() == 0) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
                Key pvt = kp.getPrivate();

                Base64.Encoder encoder = Base64.getEncoder();

                Files.write(privateKeyFile.toPath(),
                    Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
                        .encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));

                Files.write(publicKeyFile.toPath(),
                    Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
                        .encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));

                applicationContext.registerBean(KeyPair.class, () -> kp);
                applicationContext.registerBean(RSAPublicKey.class, () -> pub);
            } else {
                String privateKeyContent = new String(Files.readAllBytes(privateKeyFile.toPath()));
                byte[] encodedBytes = Base64.getDecoder().decode(removeBeginEnd(privateKeyContent));

                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = kf.generatePrivate(keySpec);

                String publicKeyContent = new String(Files.readAllBytes(publicKeyFile.toPath()));
                encodedBytes = Base64.getDecoder().decode(removeBeginEnd(publicKeyContent));
                X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedBytes);
                PublicKey publicKey = kf.generatePublic(pubSpec);

                applicationContext.registerBean(KeyPair.class, () -> new KeyPair(publicKey, privateKey));
                applicationContext.registerBean(RSAPublicKey.class, () -> (RSAPublicKey) publicKey);

                log.info("loaded privateKey: {} and publicKey: {}", privateKey, publicKey);
            }
        }

        private String removeBeginEnd(String pem) {
            pem = pem.replaceAll("-----BEGIN (.*)-----", "");
            pem = pem.replaceAll("-----END (.*)----", "");
            pem = pem.replaceAll("\r\n", "");
            pem = pem.replaceAll("\n", "");
            return pem.trim();
        }

    }

    @ConditionalOnProperty(prefix = "configuration", name = "mongo", havingValue = "true", matchIfMissing = true)
    @Configuration
    @EnableMongoAuditing
    @EnableReactiveMongoRepositories(basePackageClasses = CompanyRepository.class)
    static class MongoConfiguration {
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            jacksonObjectMapperBuilder.dateFormat(new StdDateFormat());
        };
    }

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(CompanyRepository companyRepository) {
        return args -> companyRepository.count()
            .filter(c -> c == 0)
            .map(c -> companyRepository.saveAll(Arrays.asList(
                Company.builder().name("Facebook").activated(true).createdByUser("default@admin.com").build(),
                Company.builder().name("Google").activated(true).createdByUser("default@admin.com").build(),
                Company.builder().name("Twitter").activated(true).createdByUser("default@admin.com").build())))
            .flatMap(Flux::count)
            .subscribe(c -> log.debug("Saved Default Companies:size: {}", c));
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Primary
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
