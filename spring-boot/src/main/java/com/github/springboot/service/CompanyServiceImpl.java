package com.github.springboot.service;

import com.github.springboot.dto.CompanyDto;
import com.github.springboot.model.Company;
import com.github.springboot.repository.CompanyRepository;
import lombok.AllArgsConstructor;
import org.mapstruct.Mapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;

    private final CompanyMapper companyMapper;

    public Mono<CompanyDto> save(CompanyDto companyDto) {
        Company company = companyMapper.dtoToEntity(companyDto);
        return companyMapper.entityToDto(companyRepository.save(company));
    }

    @Override
    public Mono<CompanyDto> findById(String id) {
        return companyMapper.entityToDto(companyRepository.findById(id));
    }

    @Override
    public Flux<CompanyDto> findAllActiveCompanies(Integer pageSize) {
        return companyMapper.entityToDto(companyRepository.findActiveCompanies(PageRequest.of(0, pageSize)));
    }

    @Override
    public Flux<CompanyDto> findActiveCompaniesByUser(String name, Integer pageSize) {
        return companyMapper.entityToDto(companyRepository.findActiveCompaniesByUser(name, PageRequest.of(0, pageSize)));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return companyRepository.deleteById(id);
    }

	@Mapper(componentModel = "spring")
	interface CompanyMapper {
		default Mono<CompanyDto> entityToDto(Mono<Company> company) {
			return company.map(this::map);
		}

		default Flux<CompanyDto> entityToDto(Flux<Company> companies) {
			return companies.map(this::map);
		}

		Company dtoToEntity(CompanyDto companyDto);

		CompanyDto map(Company company);
	}
}
