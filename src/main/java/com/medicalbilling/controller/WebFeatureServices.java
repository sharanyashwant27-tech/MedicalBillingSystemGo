package com.medicalbilling.controller;

import com.medicalbilling.service.AuditLogService;
import com.medicalbilling.service.BranchService;
import com.medicalbilling.service.OnlineOrderService;
import com.medicalbilling.service.ReorderSuggestionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class WebFeatureServices {

    private final BranchService branchService;
    private final OnlineOrderService onlineOrderService;
    private final ReorderSuggestionService reorderSuggestionService;
    private final AuditLogService auditLogService;
}
