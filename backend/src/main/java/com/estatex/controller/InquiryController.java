package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.InquiryDto;
import com.estatex.dto.InquiryReplyRequest;
import com.estatex.dto.InquiryRequest;
import com.estatex.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@CrossOrigin(originPatterns = "*")
public class InquiryController {

    private final InquiryService inquiryService;

    @Autowired
    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /**
     * Retrieve inquiries with optional filters.
     * GET /api/inquiries
     */
    @GetMapping
    public ResponseEntity<List<InquiryDto>> getInquiries(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerUsername,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String agentUsername,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        List<InquiryDto> list = inquiryService.getInquiries(
                customerId, customerUsername,
                agentId, agentUsername,
                propertyId, status, search
        );
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve single inquiry by ID.
     * GET /api/inquiries/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getInquiryById(@PathVariable Long id) {
        try {
            InquiryDto dto = inquiryService.getInquiryById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Send an inquiry from customer to property's agent.
     * POST /api/inquiries
     */
    @PostMapping
    public ResponseEntity<?> createInquiry(@Valid @RequestBody InquiryRequest request,
                                           @RequestParam(required = false) String customerUsername) {
        try {
            InquiryDto created = inquiryService.createInquiry(request, customerUsername);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Your inquiry has been sent to the agent.", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Agent replies to a customer inquiry.
     * POST /api/inquiries/{id}/reply
     */
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyInquiry(@PathVariable Long id,
                                          @Valid @RequestBody InquiryReplyRequest request,
                                          @RequestParam(required = false) String agentUsername) {
        try {
            InquiryDto updated = inquiryService.replyInquiry(id, request, agentUsername);
            return ResponseEntity.ok(new ApiResponse(true, "Reply sent to customer successfully.", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete an inquiry.
     * DELETE /api/inquiries/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInquiry(@PathVariable Long id,
                                           @RequestParam(required = false) String username) {
        try {
            inquiryService.deleteInquiry(id, username);
            return ResponseEntity.ok(new ApiResponse(true, "Inquiry deleted successfully.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
