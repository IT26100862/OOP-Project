package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.ReviewDto;
import com.estatex.dto.ReviewRequest;
import com.estatex.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(originPatterns = "*")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Retrieve reviews with optional filters.
     * GET /api/reviews
     */
    @GetMapping
    public ResponseEntity<List<ReviewDto>> getReviews(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerUsername,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search) {

        List<ReviewDto> list = reviewService.getReviews(
                propertyId, customerId, customerUsername, rating, search
        );
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve single review by ID.
     * GET /api/reviews/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReviewById(@PathVariable Long id) {
        try {
            ReviewDto dto = reviewService.getReviewById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create a new review from customer.
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<?> createReview(@Valid @RequestBody ReviewRequest request,
                                          @RequestParam(required = false) String customerUsername) {
        try {
            ReviewDto created = reviewService.createReview(request, customerUsername);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Review submitted successfully!", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update an existing review.
     * PUT /api/reviews/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id,
                                          @RequestBody ReviewRequest request,
                                          @RequestParam(required = false) String customerUsername) {
        try {
            ReviewDto updated = reviewService.updateReview(id, request, customerUsername);
            return ResponseEntity.ok(new ApiResponse(true, "Review updated successfully!", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a review.
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id,
                                          @RequestParam(required = false) String username) {
        try {
            reviewService.deleteReview(id, username);
            return ResponseEntity.ok(new ApiResponse(true, "Review deleted successfully.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
