package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.PropertyDto;
import com.estatex.dto.PropertyRequest;
import com.estatex.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Agent & Public Property Management.
 */
@RestController
@RequestMapping("/api/properties")
@CrossOrigin(originPatterns = "*")
public class PropertyController {

    private final PropertyService propertyService;

    @Autowired
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    /**
     * Retrieve all properties with optional filters (search, type, status, agentId, agentUsername).
     * GET /api/properties
     */
    @GetMapping
    public ResponseEntity<List<PropertyDto>> getProperties(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String agentUsername,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        List<PropertyDto> list = propertyService.getProperties(agentId, agentUsername, search, type, status);
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve single property by ID.
     * GET /api/properties/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPropertyById(@PathVariable Long id) {
        try {
            PropertyDto dto = propertyService.getPropertyById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create new property listing.
     * POST /api/properties
     */
    @PostMapping
    public ResponseEntity<?> createProperty(@Valid @RequestBody PropertyRequest request,
                                            @RequestParam(required = false) String agentUsername) {
        try {
            PropertyDto created = propertyService.createProperty(request, agentUsername);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Property listing created successfully.", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update existing property listing.
     * PUT /api/properties/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable Long id,
                                            @Valid @RequestBody PropertyRequest request,
                                            @RequestParam(required = false) String agentUsername) {
        try {
            PropertyDto updated = propertyService.updateProperty(id, request, agentUsername);
            return ResponseEntity.ok(new ApiResponse(true, "Property listing updated successfully.", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete property listing.
     * DELETE /api/properties/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable Long id,
                                            @RequestParam(required = false) String agentUsername) {
        try {
            propertyService.deleteProperty(id, agentUsername);
            return ResponseEntity.ok(ApiResponse.success("Property listing deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update status of a property (e.g., Active, Under Review, Sold).
     * PATCH /api/properties/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updatePropertyStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> payload) {
        try {
            String status = payload.getOrDefault("status", "Active");
            PropertyDto updated = propertyService.updatePropertyStatus(id, status);
            return ResponseEntity.ok(new ApiResponse(true, "Property status updated to " + status, updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Summary stats for Agent Dashboard.
     * GET /api/properties/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAgentStats(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String agentUsername) {
        return ResponseEntity.ok(propertyService.getAgentStats(agentId, agentUsername));
    }
}
