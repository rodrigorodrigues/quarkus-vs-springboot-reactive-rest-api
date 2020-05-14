package com.github.springboot.service;

import java.util.Arrays;

import com.github.springboot.dto.CompanyDto;
import com.github.springboot.model.Company;
import com.github.springboot.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceImplTest {

    CompanyServiceImpl companyService;

    @Mock
    CompanyRepository companyRepository;

    CompanyServiceImpl.CompanyMapper companyMapper = new CompanyServiceImpl$CompanyMapperImpl();

    @BeforeEach
    public void setup() {
        companyService = new CompanyServiceImpl(companyRepository, companyMapper);
    }

    @Test
    public void whenCallSaveShouldSaveCompany() {
        Company company = new Company();
        when(companyRepository.save(any())).thenReturn(Mono.just(company));

        CompanyDto companyDto = new CompanyDto();
        StepVerifier.create(companyService.save(companyDto))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void whenCallFindByIdShouldFindCompany() {
        Mono<Company> company = Mono.just(new Company());
        when(companyRepository.findById(anyString())).thenReturn(company);

        StepVerifier.create(companyService.findById(anyString()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void whenCallFindAllActiveCompaniesShouldReturnListOfCompanies() {
        when(companyRepository.findAll()).thenReturn(Flux.fromIterable(Arrays.asList(new Company(), new Company(), new Company())));

        Flux<CompanyDto> companies = companyService.findAllActiveCompanies();

        assertThat(companies.count().block()).isEqualTo(3);
    }

    @Test
    public void whenCallFindCompaniesByUserShouldReturnListOfCompanies() {
        when(companyRepository.findActiveCompaniesByUser(anyString())).thenReturn(Flux.fromIterable(Arrays.asList(new Company(), new Company())));

        Flux<CompanyDto> companies = companyService.findActiveCompaniesByUser(anyString());

        assertThat(companies.count().block()).isEqualTo(2);
    }

    @Test
    public void whenCallDeleteByIdShouldDeleteCompany() {
        companyService.deleteById("123");

        verify(companyRepository).deleteById("123");
    }

}
