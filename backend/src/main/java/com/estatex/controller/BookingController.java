package com.estatex.controller;

import com.estatex.dto.ApiResponse;
import com.estatex.dto.BookingDto;
import com.estatex.dto.BookingRequest;
import com.estatex.dto.BookingStatusUpdateRequest;
import com.estatex.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(originPatterns = "*")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Retrieve bookings with optional filters.
     * GET /api/bookings
     */
    @GetMapping
    public ResponseEntity<List<BookingDto>> getBookings(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerUsername,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String agentUsername,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<BookingDto> list = bookingService.getBookings(
                customerId, customerUsername,
                agentId, agentUsername,
                propertyId, status, search
        );
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieve single booking by ID.
     * GET /api/bookings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(@PathVariable Long id) {
        try {
            BookingDto dto = bookingService.getBookingById(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Schedule a new viewing appointment.
     * POST /api/bookings
     */
    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequest request,
                                           @RequestParam(required = false) String customerUsername) {
        try {
            BookingDto created = bookingService.createBooking(request, customerUsername);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Viewing appointment booked successfully.", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reschedule / edit a booking.
     * PUT /api/bookings/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBooking(@PathVariable Long id,
                                           @RequestBody BookingRequest request,
                                           @RequestParam(required = false) String username) {
        try {
            BookingDto updated = bookingService.updateBooking(id, request, username);
            return ResponseEntity.ok(new ApiResponse(true, "Booking updated successfully.", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update booking status (e.g. Approved, Rejected, Completed, Cancelled).
     * PATCH /api/bookings/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long id,
                                                 @Valid @RequestBody BookingStatusUpdateRequest request,
                                                 @RequestParam(required = false) String actorUsername) {
        try {
            BookingDto updated = bookingService.updateBookingStatus(id, request, actorUsername);
            return ResponseEntity.ok(new ApiResponse(true, "Booking status updated to " + updated.getStatus() + ".", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Cancel or delete a booking.
     * DELETE /api/bookings/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id,
                                           @RequestParam(required = false) String username) {
        try {
            bookingService.deleteBooking(id, username);
            return ResponseEntity.ok(new ApiResponse(true, "Booking removed successfully.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
