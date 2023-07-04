package com.github.springboot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.github.springboot.config.JwtConfigurationProperties;
import com.github.springboot.model.Company;
import com.github.springboot.repository.CompanyRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(JwtConfigurationProperties.class)
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
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            String privateKeyProperty = environment.getProperty("cert.privateKey");
            String publicKeyProperty = environment.getProperty("cert.publicKey");

            File privateKeyFile = (StringUtils.isNotBlank(privateKeyProperty) ? new File(privateKeyProperty) :
                new File(System.getProperty("java.io.tmpdir"), "privateKey.pem"));

            File publicKeyFile = (StringUtils.isNotBlank(publicKeyProperty) ? new File(publicKeyProperty) :
                new File(System.getProperty("java.io.tmpdir"), "publicKey.pem"));

            if (!privateKeyFile.exists() || privateKeyFile.length() == 0) {
                generatePrivateAndPublicKey(applicationContext, privateKeyFile, publicKeyFile);
            } else {
                KeyPair keyPair = readPrivateKey(privateKeyFile, publicKeyFile);

                applicationContext.registerBean(KeyPair.class, () -> keyPair);
                applicationContext.registerBean(RSAPublicKey.class, () -> (RSAPublicKey) keyPair.getPublic());

                log.info("loaded cert: {} ", keyPair);
            }
        }

        private KeyPair readPrivateKey(File privateKeyFile, File publicKeyFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
            String privateKeyContent = new String(Files.readAllBytes(privateKeyFile.toPath()));
            byte[] encodedBytes = Base64.getDecoder().decode(removeBeginEnd(privateKeyContent));

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(keySpec);

            PublicKey publicKey = readPublicKey(publicKeyFile);
            return new KeyPair(publicKey, privateKey);
        }

        private PublicKey readPublicKey(File publicKeyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
            byte[] encodedBytes;
            String publicKeyContent = new String(Files.readAllBytes(publicKeyFile.toPath()));
            encodedBytes = Base64.getDecoder().decode(removeBeginEnd(publicKeyContent));
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(pubSpec);
        }

        private void generatePrivateAndPublicKey(GenericReactiveWebApplicationContext applicationContext, File privateKeyFile, File publicKeyFile) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            Key pvt = kp.getPrivate();

            Base64.Encoder encoder = Base64.getEncoder();

            Files.write(privateKeyFile.toPath(),
                Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
                    .encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));
            log.info("Loaded private key: {}", privateKeyFile.toPath());

            if (!publicKeyFile.exists() || publicKeyFile.length() == 0) {
                Files.write(publicKeyFile.toPath(),
                    Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
                        .encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));
                log.info("Loaded public key: {}", privateKeyFile.toPath());
                applicationContext.registerBean(RSAPublicKey.class, () -> pub);
            } else {
                RSAPublicKey publicKey = (RSAPublicKey) readPublicKey(publicKeyFile);
                applicationContext.registerBean(RSAPublicKey.class, () -> publicKey);
            }

            applicationContext.registerBean(KeyPair.class, () -> kp);
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
