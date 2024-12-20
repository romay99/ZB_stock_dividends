package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistCompanyException;
import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRepository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import com.dayone.scraper.YahooFinanceScraper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final Trie trie;
    private final Scraper scraper;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        if (companyRepository.existsByTicker(ticker)) {
            throw new AlreadyExistCompanyException();
        }
        return storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {
        // 1. ticker 를 기준으로 회사를 스크래핑
        Company company = scraper.scrapCompanyByTicker(ticker);

        // 2. 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        if (ObjectUtils.isEmpty(company)) {
            throw new NoCompanyException();
        }
        ScrapedResult scrap = scraper.scrap(company);

        // 3. 스크래핑 결과 반환
        CompanyEntity savedEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrap.getDividends().stream().map(
                e -> new DividendEntity(savedEntity.getId(), e)
        ).toList();
        dividendRepository.saveAll(dividendEntities);

        return company;
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        List<String> list = companyRepository.findByNameStartingWithIgnoreCase(keyword, limit).stream(
                ).map(a -> a.getName()).collect(Collectors.toList());
        return list;
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream()
                .collect(Collectors.toList());
    }

    public void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        log.info("회사 삭제: {}", ticker);
        // 1. 배당금 정보 삭제
        CompanyEntity companyEntity = companyRepository.findByTicker(ticker).orElseThrow(
                () -> new NoCompanyException()
        );
        dividendRepository.deleteAllByCompanyId(companyEntity.getId());

        // 2. 회사 정보 삭제
        companyRepository.deleteById(companyEntity.getId());

        return companyEntity.getName();
    }
}
