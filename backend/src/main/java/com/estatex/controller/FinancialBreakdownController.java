package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.FinancialBreakdownDto;
import com.estatex.dto.FinancialBreakdownRequest;
import com.estatex.service.FinancialBreakdownService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial-breakdowns")
@CrossOrigin(originPatterns = "*")
public class FinancialBreakdownController {

    private final FinancialBreakdownService financialBreakdownService;

    @Autowired
    public FinancialBreakdownController(FinancialBreakdownService financialBreakdownService) {
        this.financialBreakdownService = financialBreakdownService;
    }

    /**
     * Retrieve financial breakdowns with optional filters.
     * GET /api/financial-breakdowns
     */
    @GetMapping
    public ResponseEntity<List<FinancialBreakdownDto>> getBreakdowns(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerUsername,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String search) {

        List<FinancialBreakdownDto> list = financialBreakdownService.getBreakdowns(
                customerId, customerUsername, propertyId, search
        );
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve single financial breakdown by ID.
     * GET /api/financial-breakdowns/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBreakdownById(@PathVariable Long id) {
        try {
            FinancialBreakdownDto dto = financialBreakdownService.getBreakdownById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create a new financial breakdown.
     * POST /api/financial-breakdowns
     */
    @PostMapping
    public ResponseEntity<?> createBreakdown(@Valid @RequestBody FinancialBreakdownRequest request,
                                            @RequestParam(required = false) String customerUsername) {
        try {
            FinancialBreakdownDto created = financialBreakdownService.createBreakdown(request, customerUsername);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Financial breakdown saved successfully!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update an existing financial breakdown.
     * PUT /api/financial-breakdowns/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBreakdown(@PathVariable Long id,
                                            @RequestBody FinancialBreakdownRequest request,
                                            @RequestParam(required = false) String customerUsername) {
        try {
            FinancialBreakdownDto updated = financialBreakdownService.updateBreakdown(id, request, customerUsername);
            return ResponseEntity.ok(new ApiResponse(true, "Financial breakdown updated successfully!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a financial breakdown.
     * DELETE /api/financial-breakdowns/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBreakdown(@PathVariable Long id,
                                            @RequestParam(required = false) String username) {
        try {
            financialBreakdownService.deleteBreakdown(id, username);
            return ResponseEntity.ok(new ApiResponse(true, "Financial breakdown deleted successfully.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
