package com.github.springboot.service;

import com.github.springboot.dto.CompanyDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for Company.
 */
public interface CompanyService {
    /**
     * Save a user.
     * @param companyDto
     * @return companyDto
     */
    Mono<CompanyDto> save(CompanyDto companyDto);

    /**
     * Return a Company by id.
     * @param id id
     * @return companyDto
     */
    Mono<CompanyDto> findById(String id);

    /**
     * Return list of active companies.
     * @param pageSize page size
     * @return list of users
     */
    Flux<CompanyDto> findAllActiveCompanies(Integer pageSize);

    /**
     * Return list of active companies by user
     * @param name user
     * @param pageSize page size
     * @return list of companies
     */
    Flux<CompanyDto> findActiveCompaniesByUser(String name, Integer pageSize);

    /**
     * Delete a user by id.
     * @param id id
     */
    Mono<Void> deleteById(String id);
}
