package com.openchat4u.dictionary;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {
    private final DictionaryRepository dictionaryRepository;

    public List<Dictionary> findByTenantAndType(String tenantCode, String type) {
        return dictionaryRepository.selectList(
            new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getTenantCode, tenantCode)
                .eq(Dictionary::getType, type)
                .eq(Dictionary::getIsActive, true)
        );
    }

    public List<Dictionary> findByTenant(String tenantCode) {
        return dictionaryRepository.selectList(
            new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getTenantCode, tenantCode)
                .eq(Dictionary::getIsActive, true)
        );
    }

    public List<Dictionary> searchByTenantAndTerm(String tenantCode, String term) {
        return dictionaryRepository.selectList(
            new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getTenantCode, tenantCode)
                .like(Dictionary::getTerm, term)
                .eq(Dictionary::getIsActive, true)
        );
    }

    public Dictionary findById(Long id) {
        Dictionary d = dictionaryRepository.selectById(id);
        if (d == null) {
            throw new IllegalArgumentException("Dictionary not found: " + id);
        }
        return d;
    }

    public Dictionary create(Dictionary dictionary) {
        boolean exists = dictionaryRepository.exists(
            new LambdaQueryWrapper<Dictionary>()
                .eq(Dictionary::getTenantCode, dictionary.getTenantCode())
                .eq(Dictionary::getType, dictionary.getType())
                .eq(Dictionary::getTerm, dictionary.getTerm())
        );
        if (exists) {
            throw new IllegalArgumentException("Dictionary term already exists for this tenant and type");
        }
        dictionaryRepository.insert(dictionary);
        return dictionary;
    }

    public Dictionary update(Long id, Dictionary details) {
        Dictionary dictionary = findById(id);
        dictionary.setTerm(details.getTerm());
        dictionary.setSynonyms(details.getSynonyms());
        dictionary.setDescription(details.getDescription());
        dictionary.setType(details.getType());
        dictionary.setIsActive(details.getIsActive());
        dictionaryRepository.updateById(dictionary);
        return dictionary;
    }

    public void delete(Long id) {
        Dictionary dictionary = findById(id);
        dictionary.setIsActive(false);
        dictionaryRepository.updateById(dictionary);
    }

    public String enhanceQuestion(String tenantCode, String question) {
        List<Dictionary> dictionaries = findByTenant(tenantCode);
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
