package com.openchat4u.dictionary;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private DictionaryRepository dictionaryRepository;

    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        dictionaryService = new DictionaryService(dictionaryRepository);
    }

    @Test
    void testFindByTenantAndType() {
        String tenantCode = "tenant1";
        String type = "SYNONYM";
        List<Dictionary> expected = Arrays.asList(
            createDictionary(1L, tenantCode, type, "销售额", "营收,收入"),
            createDictionary(2L, tenantCode, type, "用户", "客户")
        );

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(expected);

        List<Dictionary> result = dictionaryService.findByTenantAndType(tenantCode, type);

        assertEquals(2, result.size());
        verify(dictionaryRepository).selectList(any(Wrapper.class));
    }

    @Test
    void testFindByTenant() {
        String tenantCode = "tenant1";
        List<Dictionary> expected = Arrays.asList(
            createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收"),
            createDictionary(2L, tenantCode, "BUSINESS_TERM", "GMV", "成交总额")
        );

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(expected);

        List<Dictionary> result = dictionaryService.findByTenant(tenantCode);

        assertEquals(2, result.size());
        verify(dictionaryRepository).selectList(any(Wrapper.class));
    }

    @Test
    void testSearchByTenantAndTerm() {
        String tenantCode = "tenant1";
        String term = "销售";
        List<Dictionary> expected = List.of(
            createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收")
        );

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(expected);

        List<Dictionary> result = dictionaryService.searchByTenantAndTerm(tenantCode, term);

        assertEquals(1, result.size());
        verify(dictionaryRepository).selectList(any(Wrapper.class));
    }

    @Test
    void testFindById() {
        Dictionary expected = createDictionary(1L, "tenant1", "SYNONYM", "销售额", "营收");
        when(dictionaryRepository.selectById(1L)).thenReturn(expected);

        Dictionary result = dictionaryService.findById(1L);

        assertNotNull(result);
        assertEquals("销售额", result.getTerm());
    }

    @Test
    void testFindByIdNotFound() {
        when(dictionaryRepository.selectById(999L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> dictionaryService.findById(999L));
    }

    @Test
    void testCreate() {
        Dictionary newDict = createDictionary(null, "tenant1", "SYNONYM", "新术语", "同义词1,同义词2");
        when(dictionaryRepository.exists(any(Wrapper.class))).thenReturn(false);
        when(dictionaryRepository.insert(any(Dictionary.class))).thenAnswer(invocation -> {
            Dictionary d = invocation.getArgument(0);
            d.setId(1L);
            return 1;
        });

        Dictionary result = dictionaryService.create(newDict);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(dictionaryRepository).insert(newDict);
    }

    @Test
    void testCreateDuplicate() {
        Dictionary newDict = createDictionary(null, "tenant1", "SYNONYM", "已存在", "同义词");
        when(dictionaryRepository.exists(any(Wrapper.class))).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> dictionaryService.create(newDict));
        verify(dictionaryRepository, never()).insert(any(Dictionary.class));
    }

    @Test
    void testUpdate() {
        Dictionary existing = createDictionary(1L, "tenant1", "SYNONYM", "旧术语", "旧同义词");
        Dictionary details = createDictionary(null, "tenant1", "SYNONYM", "新术语", "新同义词");
        details.setDescription("新描述");

        when(dictionaryRepository.selectById(1L)).thenReturn(existing);
        when(dictionaryRepository.updateById(any(Dictionary.class))).thenReturn(1);

        Dictionary result = dictionaryService.update(1L, details);

        assertEquals("新术语", existing.getTerm());
        assertEquals("新同义词", existing.getSynonyms());
        assertEquals("新描述", existing.getDescription());
        assertSame(existing, result);
    }

    @Test
    void testDelete() {
        Dictionary existing = createDictionary(1L, "tenant1", "SYNONYM", "术语", "同义词");
        when(dictionaryRepository.selectById(1L)).thenReturn(existing);
        when(dictionaryRepository.updateById(any(Dictionary.class))).thenReturn(1);

        dictionaryService.delete(1L);

        assertFalse(existing.getIsActive());
        verify(dictionaryRepository).updateById(existing);
    }

    @Test
    void testEnhanceQuestion() {
        String tenantCode = "tenant1";
        String question = "查询上个月的营收情况";

        Dictionary dict = createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收,收入");
        dict.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("查询上个月的销售额情况", result);
    }

    @Test
    void testEnhanceQuestionMultipleSynonyms() {
        String tenantCode = "tenant1";
        String question = "统计收入和用户数量";

        Dictionary dict1 = createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收,收入");
        dict1.setIsActive(true);
        Dictionary dict2 = createDictionary(2L, tenantCode, "SYNONYM", "客户", "用户,顾客");
        dict2.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict1, dict2));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("统计销售额和客户数量", result);
    }

    @Test
    void testEnhanceQuestionNoMatch() {
        String tenantCode = "tenant1";
        String question = "查询订单数据";

        Dictionary dict = createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收");
        dict.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("查询订单数据", result);
    }

    @Test
    void testEnhanceQuestionWithSpecialCharacters() {
        String tenantCode = "tenant1";
        String question = "查询营收 (含税) 数据";

        Dictionary dict = createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收");
        dict.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("查询销售额 (含税) 数据", result);
    }

    @Test
    void testEnhanceQuestionEmptySynonyms() {
        String tenantCode = "tenant1";
        String question = "查询营收数据";

        Dictionary dict = createDictionary(1L, tenantCode, "SYNONYM", "销售额", "");
        dict.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("查询营收数据", result);
    }

    @Test
    void testEnhanceQuestionNullSynonyms() {
        String tenantCode = "tenant1";
        String question = "查询营收数据";

        Dictionary dict = createDictionary(1L, tenantCode, "SYNONYM", "销售额", null);
        dict.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("查询营收数据", result);
    }

    @Test
    void testEnhanceQuestionCaseInsensitive() {
        String tenantCode = "tenant1";
        String question = "查询 YINGSOU 数据";

        Dictionary dict = createDictionary(1L, tenantCode, "SYNONYM", "销售额", "营收");
        dict.setIsActive(true);

        when(dictionaryRepository.selectList(any(Wrapper.class))).thenReturn(List.of(dict));

        String result = dictionaryService.enhanceQuestion(tenantCode, question);

        assertEquals("查询 YINGSOU 数据", result);
    }

    private Dictionary createDictionary(Long id, String tenantCode, String type, String term, String synonyms) {
        Dictionary dict = new Dictionary();
        dict.setId(id);
        dict.setTenantCode(tenantCode);
        dict.setType(type);
        dict.setTerm(term);
        dict.setSynonyms(synonyms);
        dict.setIsActive(true);
        return dict;
    }
}
