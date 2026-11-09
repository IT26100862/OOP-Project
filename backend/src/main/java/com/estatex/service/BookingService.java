package com.estatex.service;

import com.estatex.dto.BookingDto;
import com.estatex.dto.BookingRequest;
import com.estatex.dto.BookingStatusUpdateRequest;
import com.estatex.model.*;
import com.estatex.repository.BookingRepository;
import com.estatex.repository.PropertyImageRepository;
import com.estatex.repository.PropertyRepository;
import com.estatex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // High-performance in-memory cache to eliminate remote WAN database latency
    private final Map<String, List<BookingDto>> bookingsListCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> bookingsListCacheExpiry = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60 * 1000L; // 60 seconds TTL

    private static final String DEFAULT_IMAGE = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80";

    public void invalidateCache() {
        bookingsListCache.clear();
        bookingsListCacheExpiry.clear();
    }

    @Autowired
    public BookingService(BookingRepository bookingRepository,
            PropertyRepository propertyRepository,
            PropertyImageRepository propertyImageRepository,
            UserRepository userRepository,
            UserService userService) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Resolves Customer ID from given identifier or request.
     */
    public Long resolveCustomerId(Long customerId, String identifier) {
        if (customerId != null && customerId > 0) {
            return customerId;
        }
        if (identifier != null && !identifier.isBlank()) {
            User user = userService.findUserByIdentifier(identifier);
            if (user != null) {
                return user.getId();
            }
        }
        // Fallback to first available customer in database
        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        if (!customers.isEmpty()) {
            return customers.get(0).getId();
        }
        return 1L;
    }

    /**
     * Resolves Agent ID from given identifier.
     */
    public Long resolveAgentId(Long agentId, String identifier) {
        if (agentId != null && agentId > 0) {
            return agentId;
        }
        if (identifier != null && !identifier.isBlank()) {
            User user = userService.findUserByIdentifier(identifier);
            if (user != null) {
                return user.getId();
            }
        }
        List<User> agents = userRepository.findByRole(Role.AGENT);
        if (!agents.isEmpty()) {
            return agents.get(0).getId();
        }
        return 1L;
    }

    /**
     * Create a new booking / viewing schedule.
     */
    public BookingDto createBooking(BookingRequest request, String customerIdentifier) {
        if (request.getPropertyId() == null) {
            throw new IllegalArgumentException("Property ID must be specified.");
        }
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Property not found with ID: " + request.getPropertyId()));

        Long customerId = resolveCustomerId(
                request.getCustomerId(),
                request.getCustomerUsername() != null ? request.getCustomerUsername() : customerIdentifier);

        LocalDate viewingDate;
        try {
            viewingDate = LocalDate.parse(request.getViewingDate().trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Invalid viewing date format. Expected yyyy-MM-dd, received: " + request.getViewingDate());
        }

        String timeSlot = request.getTimeSlot() != null ? request.getTimeSlot().trim() : "10:00 AM";

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setPropertyId(property.getId());
        booking.setViewingDate(viewingDate);
        booking.setTimeSlot(timeSlot);
        booking.setStatus("Pending");
        booking.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Booking saved = bookingRepository.saveAndFlush(booking);
        invalidateCache();
        return toDto(saved);
    }

    /**
     * Retrieve bookings with multiple optional filters:
     * - customerId / customerUsername
     * - agentId / agentUsername
     * - propertyId
     * - status
     * - search
     */
    @Transactional(readOnly = true)
    public List<BookingDto> getBookings(Long customerId, String customerUsername,
            Long agentId, String agentUsername,
            Long propertyId,
            String status, String search) {
        String cacheKey = String.format("cId=%s:cUsr=%s:aId=%s:aUsr=%s:pId=%s:st=%s:q=%s",
                customerId, customerUsername, agentId, agentUsername, propertyId, status, search);
        Long exp = bookingsListCacheExpiry.get(cacheKey);
        if (exp != null && System.currentTimeMillis() < exp) {
            List<BookingDto> cached = bookingsListCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        List<Booking> list;

        // 1. Filter by Customer
        Long resolvedCustId = null;
        if (customerId != null && customerId > 0) {
            resolvedCustId = customerId;
        } else if (customerUsername != null && !customerUsername.isBlank()) {
            User u = userService.findUserByIdentifier(customerUsername);
            if (u != null)
                resolvedCustId = u.getId();
        }

        // 2. Filter by Agent
        Long resolvedAgentId = null;
        if (agentId != null && agentId > 0) {
            resolvedAgentId = agentId;
        } else if (agentUsername != null && !agentUsername.isBlank()) {
            User u = userService.findUserByIdentifier(agentUsername);
            if (u != null)
                resolvedAgentId = u.getId();
        }

        if (resolvedCustId != null) {
            list = bookingRepository.findByCustomerIdOrderByIdDesc(resolvedCustId);
        } else if (resolvedAgentId != null) {
            List<Property> agentProps = propertyRepository.findByAgentIdOrderByIdDesc(resolvedAgentId);
            if (agentProps.isEmpty()) {
                bookingsListCache.put(cacheKey, Collections.emptyList());
                bookingsListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
                return Collections.emptyList();
            }
            List<Long> propIds = agentProps.stream().map(Property::getId).collect(Collectors.toList());
            list = bookingRepository.findByPropertyIdInOrderByIdDesc(propIds);
        } else if (propertyId != null && propertyId > 0) {
            list = bookingRepository.findByPropertyIdOrderByIdDesc(propertyId);
        } else {
            list = bookingRepository.findAllByOrderByIdDesc();
        }

        if (list.isEmpty()) {
            bookingsListCache.put(cacheKey, Collections.emptyList());
            bookingsListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
            return Collections.emptyList();
        }

        // Batch fetch customers and properties in ONE query each to avoid N+1 remote database round trips
        Set<Long> customerIds = list.stream().map(Booking::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> propertyIds = list.stream().map(Booking::getPropertyId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> customerMap = customerIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(customerIds).stream()
                        .collect(Collectors.toMap(User::getId, java.util.function.Function.identity(), (a, b) -> a));

        Map<Long, Property> propertyMap = propertyIds.isEmpty() ? Collections.emptyMap() :
                propertyRepository.findAllById(propertyIds).stream()
                        .collect(Collectors.toMap(Property::getId, java.util.function.Function.identity(), (a, b) -> a));

        Set<Long> agentIds = propertyMap.values().stream().map(Property::getAgentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> agentMap = agentIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(agentIds).stream()
                        .collect(Collectors.toMap(User::getId, java.util.function.Function.identity(), (a, b) -> a));

        // Apply Status and Search in-memory
        String stFilter = (status != null) ? status.trim().toLowerCase() : "";
        String q = (search != null) ? search.trim().toLowerCase() : "";

        List<BookingDto> result = list.stream().map(b -> {
            BookingDto dto = new BookingDto(b);

            if (b.getCustomerId() != null && customerMap.containsKey(b.getCustomerId())) {
                User u = customerMap.get(b.getCustomerId());
                dto.setCustomerName(u.getFullName() != null ? u.getFullName() : u.getUsername());
                dto.setCustomerEmail(u.getEmail());
                dto.setCustomerPhone(u.getPhone());
            }

            if (b.getPropertyId() != null && propertyMap.containsKey(b.getPropertyId())) {
                Property p = propertyMap.get(b.getPropertyId());
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyAddress(p.getAddress());
                dto.setPropertyPrice(p.getPrice());
                dto.setPropertyListingType(p.getListingType());
                dto.setPropertyImage(DEFAULT_IMAGE);

                if (p.getAgentId() != null && agentMap.containsKey(p.getAgentId())) {
                    User agent = agentMap.get(p.getAgentId());
                    dto.setAgentId(p.getAgentId());
                    dto.setAgentName(agent.getFullName() != null ? agent.getFullName() : agent.getUsername());
                    dto.setAgentEmail(agent.getEmail());
                    dto.setAgentPhone(agent.getPhone());
                }
            } else {
                dto.setPropertyImage(DEFAULT_IMAGE);
            }

            return dto;
        }).filter(dto -> {
            boolean matchesStatus = stFilter.isEmpty() ||
                    (dto.getStatus() != null && dto.getStatus().toLowerCase().equals(stFilter));

            boolean matchesQuery = q.isEmpty() ||
                    (dto.getBookingCode() != null && dto.getBookingCode().toLowerCase().contains(q)) ||
                    (dto.getId() != null && String.valueOf(dto.getId()).equals(q)) ||
                    (dto.getPropertyTitle() != null && dto.getPropertyTitle().toLowerCase().contains(q)) ||
                    (dto.getCustomerName() != null && dto.getCustomerName().toLowerCase().contains(q)) ||
                    (dto.getCustomerEmail() != null && dto.getCustomerEmail().toLowerCase().contains(q)) ||
                    (dto.getAgentName() != null && dto.getAgentName().toLowerCase().contains(q));

            return matchesStatus && matchesQuery;
        }).collect(Collectors.toList());

        bookingsListCache.put(cacheKey, result);
        bookingsListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
        return result;
    }

    /**
     * Retrieve single booking by ID.
     */
    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));
        return toDto(booking);
    }

    /**
     * Update viewing date, time slot, or notes for a booking.
     */
    public BookingDto updateBooking(Long id, BookingRequest request, String actorIdentifier) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));

        if (request.getViewingDate() != null && !request.getViewingDate().isBlank()) {
            try {
                booking.setViewingDate(LocalDate.parse(request.getViewingDate().trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format: " + request.getViewingDate());
            }
        }
        if (request.getTimeSlot() != null && !request.getTimeSlot().isBlank()) {
            booking.setTimeSlot(request.getTimeSlot().trim());
        }
        if (request.getNotes() != null) {
            booking.setNotes(request.getNotes().trim());
        }

        Booking saved = bookingRepository.saveAndFlush(booking);
        invalidateCache();
        return toDto(saved);
    }

    /**
     * Update booking status: 'Approved', 'Rejected', 'Completed', 'Cancelled'.
     */
    public BookingDto updateBookingStatus(Long id, BookingStatusUpdateRequest request, String actorIdentifier) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));

        String newStatus = request.getStatus().trim();
        // Capitalize first letter to standardize: Pending, Approved, Rejected,
        // Completed, Cancelled
        String canonicalStatus = newStatus.substring(0, 1).toUpperCase() + newStatus.substring(1).toLowerCase();
        booking.setStatus(canonicalStatus);

        if (request.getReason() != null && !request.getReason().isBlank()) {
            booking.setReason(request.getReason().trim());
        }

        Booking saved = bookingRepository.saveAndFlush(booking);
        invalidateCache();
        return toDto(saved);
    }

    /**
     * Delete booking by ID.
     */
    public void deleteBooking(Long id, String actorIdentifier) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));
        bookingRepository.delete(booking);
        invalidateCache();
    }

    /**
     * Convert Booking entity to enriched BookingDto.
     */
    private BookingDto toDto(Booking b) {
        if (b == null) return null;
        BookingDto dto = new BookingDto(b);

        // Enrich customer info
        if (b.getCustomerId() != null) {
            userRepository.findById(b.getCustomerId()).ifPresent(user -> {
                dto.setCustomerName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                dto.setCustomerEmail(user.getEmail());
                dto.setCustomerPhone(user.getPhone());
            });
        }

        // Enrich property & agent info
        if (b.getPropertyId() != null) {
            propertyRepository.findById(b.getPropertyId()).ifPresent(p -> {
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyAddress(p.getAddress());
                dto.setPropertyPrice(p.getPrice());
                dto.setPropertyListingType(p.getListingType());
                dto.setPropertyImage(DEFAULT_IMAGE);

                // Find agent info
                if (p.getAgentId() != null) {
                    dto.setAgentId(p.getAgentId());
                    userRepository.findById(p.getAgentId()).ifPresent(agent -> {
                        dto.setAgentName(agent.getFullName() != null ? agent.getFullName() : agent.getUsername());
                        dto.setAgentEmail(agent.getEmail());
                        dto.setAgentPhone(agent.getPhone());
                    });
                }
            });
        } else {
            dto.setPropertyImage(DEFAULT_IMAGE);
        }

        return dto;
    }
}
