package com.github.springboot.repository;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springboot.model.Company;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.Disposable;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"configuration.initialLoad=false", "logging.level.com.github.springboot=debug"})
@Import({ObjectMapper.class})
public class CompanyRepositoryTest {
    @Autowired
    CompanyRepository companyRepository;

    @BeforeEach
    public void setup() {
        companyRepository.save(Company.builder().name("Test")
                .createdByUser("me")
                .activated(true)
                .build()).block();

        companyRepository.save(Company.builder().name("Test not active")
                .createdByUser("me")
                .activated(false)
                .build()).block();

        companyRepository.save(Company.builder().name("Test 2")
                .createdByUser("another_user")
                .activated(true)
                .build()).block();
    }

    @Test
    @DisplayName("Test - Return list of Companies")
    public void findAllStream() {
        StepVerifier.create(companyRepository.findAll())
                .expectNextCount(3)
                .expectComplete()
                .verify();
    }

    @Test
    public void testFindAllActiveCompanies() throws Exception {
        Queue<Company> companies = new ConcurrentLinkedQueue<>();

        Disposable disposable = companyRepository.findActiveCompanies()
                .doOnNext(companies::add)
                .subscribe();

        TimeUnit.MILLISECONDS.sleep(100);

        disposable.dispose();

        assertThat(companies.size()).isEqualTo(2);
        assertThat(Stream.of(companies.toArray(new Company[] {})).map(Company::getName))
                .containsExactlyInAnyOrder("Test", "Test 2");
    }

    @Test
    public void testFindActiveCompaniesByUser() throws InterruptedException {
        Queue<Company> companies = new ConcurrentLinkedQueue<>();

        Disposable disposable = companyRepository.findActiveCompaniesByUser("me")
                .doOnNext(companies::add)
                .subscribe();

        TimeUnit.MILLISECONDS.sleep(100);

        disposable.dispose();

        assertThat(companies.size()).isEqualTo(1);
        assertThat(companies.peek().getName()).isEqualTo("Test");
    }

    @AfterEach
    public void tearDown() {
        companyRepository.deleteAll().subscribe(a -> log.debug("Delete all companies"));
    }
}
