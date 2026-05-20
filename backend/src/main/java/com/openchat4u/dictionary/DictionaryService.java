package com.openchat4u.dictionary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {
    private final DictionaryRepository dictionaryRepository;

    public List<Dictionary> findByTenantAndType(String tenantCode, String type) {
        return dictionaryRepository.findByTenantCodeAndTypeAndIsActiveTrue(tenantCode, type);
    }

    public List<Dictionary> findByTenant(String tenantCode) {
        return dictionaryRepository.findByTenantCodeAndIsActiveTrue(tenantCode);
    }

    public List<Dictionary> searchByTenantAndTerm(String tenantCode, String term) {
        return dictionaryRepository.findByTenantCodeAndTermContainingIgnoreCaseAndIsActiveTrue(tenantCode, term);
    }

    public Dictionary findById(Long id) {
        return dictionaryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dictionary not found: " + id));
    }

    public Dictionary create(Dictionary dictionary) {
        if (dictionaryRepository.existsByTenantCodeAndTypeAndTerm(
                dictionary.getTenantCode(), dictionary.getType(), dictionary.getTerm())) {
            throw new IllegalArgumentException("Dictionary term already exists for this tenant and type");
        }
        return dictionaryRepository.save(dictionary);
    }

    public Dictionary update(Long id, Dictionary details) {
        Dictionary dictionary = findById(id);
        dictionary.setTerm(details.getTerm());
        dictionary.setSynonyms(details.getSynonyms());
        dictionary.setDescription(details.getDescription());
        dictionary.setType(details.getType());
        dictionary.setIsActive(details.getIsActive());
        return dictionaryRepository.save(dictionary);
    }

    public void delete(Long id) {
        Dictionary dictionary = findById(id);
        dictionary.setIsActive(false);
        dictionaryRepository.save(dictionary);
    }

    public String enhanceQuestion(String tenantCode, String question) {
        List<Dictionary> dictionaries = dictionaryRepository.findByTenantCodeAndIsActiveTrue(tenantCode);
        String enhanced = question;
        for (Dictionary dict : dictionaries) {
            if (dict.getSynonyms() != null && !dict.getSynonyms().isEmpty()) {
                String[] synonyms = dict.getSynonyms().split(",");
                for (String synonym : synonyms) {
                    String trimmed = synonym.trim();
                    if (!trimmed.isEmpty() && question.toLowerCase().contains(trimmed.toLowerCase())) {
                        String escapedTerm = java.util.regex.Pattern.quote(trimmed);
                        enhanced = enhanced.replaceAll("(?i)" + escapedTerm, java.util.regex.Matcher.quoteReplacement(dict.getTerm()));
                    }
                }
            }
        }
        return enhanced;
    }
}
