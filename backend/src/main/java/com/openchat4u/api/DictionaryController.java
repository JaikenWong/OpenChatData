package com.openchat4u.api;

import com.openchat4u.dictionary.Dictionary;
import com.openchat4u.dictionary.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/{tenantCode}")
    public List<Dictionary> listDictionaries(
            @PathVariable String tenantCode,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String term) {
        
        if (term != null && !term.trim().isEmpty()) {
            return dictionaryService.searchByTenantAndTerm(tenantCode, term);
        }
        if (type != null && !type.trim().isEmpty()) {
            return dictionaryService.findByTenantAndType(tenantCode, type);
        }
        return dictionaryService.findByTenant(tenantCode);
    }

    @GetMapping("/{id}")
    public Dictionary getDictionary(@PathVariable Long id) {
        return dictionaryService.findById(id);
    }

    @PostMapping
    public Dictionary createDictionary(@RequestBody Dictionary dictionary) {
        return dictionaryService.create(dictionary);
    }

    @PutMapping("/{id}")
    public Dictionary updateDictionary(@PathVariable Long id, @RequestBody Dictionary dictionary) {
        return dictionaryService.update(id, dictionary);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> deleteDictionary(@PathVariable Long id) {
        dictionaryService.delete(id);
        return Map.of("success", true);
    }

    @PostMapping("/{tenantCode}/enhance")
    public Map<String, String> enhanceQuestion(@PathVariable String tenantCode, @RequestBody Map<String, String> request) {
        String question = request.get("question");
        String enhanced = dictionaryService.enhanceQuestion(tenantCode, question);
        return Map.of("original", question, "enhanced", enhanced);
    }

    @GetMapping("/types")
    public List<String> getTypes() {
        return List.of("SYNONYM", "BUSINESS_TERM", "COLUMN_ALIAS", "TABLE_ALIAS");
    }
}
