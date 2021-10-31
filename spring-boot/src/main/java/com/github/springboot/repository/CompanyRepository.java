package com.github.springboot.repository;

import com.github.springboot.model.Company;
import reactor.core.publisher.Flux;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Company Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface CompanyRepository extends ReactiveMongoRepository<Company, String> {
    @Query("{'activated': true}")
    Flux<Company> findActiveCompanies(final Pageable page);

    @Query("{'activated': true, 'createdByUser': ?0}")
    Flux<Company> findActiveCompaniesByUser(String user, final Pageable page);
}
