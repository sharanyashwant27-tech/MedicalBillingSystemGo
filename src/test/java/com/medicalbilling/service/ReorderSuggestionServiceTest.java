package com.medicalbilling.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReorderSuggestionServiceTest {

    @Autowired
    private ReorderSuggestionService reorderSuggestionService;

    @Test
    void getSuggestionsReturnsList() {
        List<Map<String, Object>> suggestions = reorderSuggestionService.getSuggestions();
        assertNotNull(suggestions);
    }
}
