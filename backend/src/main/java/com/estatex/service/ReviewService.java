package com.estatex.service;

import com.estatex.dto.ReviewDto;
import com.estatex.dto.ReviewRequest;
import com.estatex.model.Property;
import com.estatex.model.Review;
import com.estatex.model.Role;
import com.estatex.model.User;
import com.estatex.repository.PropertyRepository;
import com.estatex.repository.ReviewRepository;
import com.estatex.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    // In-memory cache with short TTL (10s)
    private final Map<String, List<ReviewDto>> reviewListCache = new ConcurrentHashMap<>();
    private final Map<String, Long> reviewListCacheExpiry = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10_000L;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository,
                         PropertyRepository propertyRepository,
                         UserRepository userRepository,
                         UserService userService,
                         JdbcTemplate jdbcTemplate) {
        this.reviewRepository = reviewRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS reviews (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  customer_id BIGINT NOT NULL," +
                "  property_id BIGINT NOT NULL," +
                "  rating INT NOT NULL," +
                "  comment TEXT," +
                "  status VARCHAR(50) NOT NULL DEFAULT 'Approved'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_rev_customer (customer_id)," +
                "  INDEX idx_rev_property (property_id)" +
                ")"
            );
            log.info("ReviewService: Verified 'reviews' table in database.");

            // Ensure Kavinda Senanayake exists if not present for realistic reviews
            User kavinda = userRepository.findByUsernameIgnoreCase("kavinda@user").orElse(null);
            if (kavinda == null) {
                kavinda = new User("kavinda@user", "userPass_kavinda123", "kavinda.s@estatex.lk", "Kavinda Senanayake", "+94 77 345 6789", Role.CUSTOMER);
                kavinda = userRepository.saveAndFlush(kavinda);
            }

            // Seed initial reviews if table is empty
            if (reviewRepository.count() == 0) {
                List<Property> properties = propertyRepository.findAll();
                if (!properties.isEmpty()) {
                    Property p1 = properties.get(0);
                    Property p2 = properties.size() > 1 ? properties.get(1) : p1;

                    User dilshan = userRepository.findByUsernameIgnoreCase("dilshan@user")
                            .orElseGet(() -> userRepository.findAll().stream()
                                    .filter(u -> u.getRole() == Role.CUSTOMER)
                                    .findFirst()
                                    .orElse(null));

                    Long kavindaId = kavinda.getId();
                    Long dilshanId = (dilshan != null) ? dilshan.getId() : kavindaId;

                    // Review 1: Kavinda on Property 1 (5 stars)
                    Review r1 = new Review(kavindaId, p1.getId(), 5,
                            "Spectacular ocean views and world-class craftsmanship. Agent Chaminda gave us an insightful and professional property walkthrough.");
                    r1.setStatus("Approved");
                    r1.setCreatedAt(LocalDateTime.of(2026, 9, 28, 10, 30));
                    reviewRepository.save(r1);

                    // Review 2: Dilshan on Property 1 (4 stars)
                    Review r2 = new Review(dilshanId, p1.getId(), 4,
                            "The architecture is sublime. Easy road access and private deed documentation verified. High investment potential.");
                    r2.setStatus("Approved");
                    r2.setCreatedAt(LocalDateTime.of(2026, 10, 2, 14, 15));
                    reviewRepository.save(r2);

                    // Review 3: Dilshan on Property 2 (4 stars)
                    Review r3 = new Review(dilshanId, p2.getId(), 4,
                            "Very quiet residential neighborhood. The solar integration was explained in detail by the agent. Great layout.");
                    r3.setStatus("Approved");
                    r3.setCreatedAt(LocalDateTime.of(2026, 9, 29, 16, 40));
                    reviewRepository.save(r3);

                    reviewRepository.flush();
                    log.info("ReviewService: Seeded 3 initial reviews.");
                }
            }
        } catch (Exception e) {
            log.warn("ReviewService: Table initialization warning: {}", e.getMessage());
        }
    }

    public void invalidateCache() {
        reviewListCache.clear();
        reviewListCacheExpiry.clear();
    }

    /**
     * Create a new review from a customer for a property.
     */
    public ReviewDto createReview(ReviewRequest req, String customerIdentifier) {
        if (req.getPropertyId() == null) {
            throw new IllegalArgumentException("Property ID is required.");
        }
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars.");
        }
        if (req.getComment() == null || req.getComment().trim().isBlank()) {
            throw new IllegalArgumentException("Review comment cannot be empty.");
        }

        Property property = propertyRepository.findById(req.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property listing not found with ID: " + req.getPropertyId()));

        // Resolve customer
        User customer = null;
        String ident = (customerIdentifier != null && !customerIdentifier.isBlank())
                ? customerIdentifier
                : req.getCustomerUsername();

        if (ident != null && !ident.isBlank()) {
            customer = userService.findUserByIdentifier(ident);
        }

        if (customer == null) {
            customer = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.CUSTOMER)
                    .findFirst()
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        }

        if (customer == null) {
            throw new IllegalArgumentException("Could not identify customer. Please log in before submitting a review.");
        }

        Review review = new Review(customer.getId(), property.getId(), req.getRating(), req.getComment().trim());
        review.setStatus("Approved");
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.saveAndFlush(review);
        invalidateCache();

        return toDto(saved);
    }

    /**
     * Update an existing review (rating and/or comment).
     */
    public ReviewDto updateReview(Long reviewId, ReviewRequest req, String customerIdentifier) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        if (req.getRating() != null) {
            if (req.getRating() < 1 || req.getRating() > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5 stars.");
            }
            review.setRating(req.getRating());
        }

        if (req.getComment() != null && !req.getComment().trim().isBlank()) {
            review.setComment(req.getComment().trim());
        }

        Review saved = reviewRepository.saveAndFlush(review);
        invalidateCache();
        return toDto(saved);
    }

    /**
     * Delete a review by ID.
     */
    public void deleteReview(Long reviewId, String userIdentifier) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        reviewRepository.delete(review);
        invalidateCache();
    }

    /**
     * Retrieve single review by ID.
     */
    @Transactional(readOnly = true)
    public ReviewDto getReviewById(Long id) {
        Review rev = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + id));
        return toDto(rev);
    }

    /**
     * Retrieve reviews with flexible filters.
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviews(Long propertyId, Long customerId, String customerUsername,
                                      Integer rating, String search) {

        String cacheKey = String.format("rev:%s:%s:%s:%s:%s",
                propertyId, customerId, customerUsername, rating, search);

        Long exp = reviewListCacheExpiry.get(cacheKey);
        if (exp != null && System.currentTimeMillis() < exp) {
            List<ReviewDto> cached = reviewListCache.get(cacheKey);
            if (cached != null) return cached;
        }

        List<Review> list;

        // Resolve Customer
        Long resolvedCustId = null;
        if (customerId != null && customerId > 0) {
            resolvedCustId = customerId;
        } else if (customerUsername != null && !customerUsername.isBlank()) {
            User u = userService.findUserByIdentifier(customerUsername);
            if (u != null) resolvedCustId = u.getId();
        }

        final Long finalCustId = resolvedCustId;
        if (propertyId != null && propertyId > 0 && finalCustId != null) {
            list = reviewRepository.findByPropertyIdOrderByIdDesc(propertyId).stream()
                    .filter(r -> r.getCustomerId().equals(finalCustId))
                    .collect(Collectors.toList());
        } else if (propertyId != null && propertyId > 0) {
            list = reviewRepository.findByPropertyIdOrderByIdDesc(propertyId);
        } else if (resolvedCustId != null) {
            list = reviewRepository.findByCustomerIdOrderByIdDesc(resolvedCustId);
        } else {
            list = reviewRepository.findAllByOrderByIdDesc();
        }

        if (list.isEmpty()) {
            reviewListCache.put(cacheKey, Collections.emptyList());
            reviewListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
            return Collections.emptyList();
        }

        // Batch fetch Customers, Properties, and Agents in single queries
        Set<Long> customerIds = list.stream().map(Review::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> propertyIds = list.stream().map(Review::getPropertyId).filter(Objects::nonNull).collect(Collectors.toSet());

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

        String q = (search != null) ? search.trim().toLowerCase() : "";

        List<ReviewDto> result = list.stream().map(rev -> {
            ReviewDto dto = new ReviewDto(rev);

            if (rev.getCustomerId() != null && customerMap.containsKey(rev.getCustomerId())) {
                User u = customerMap.get(rev.getCustomerId());
                dto.setCustomerName(u.getFullName() != null ? u.getFullName() : u.getUsername());
                dto.setCustomerUsername(u.getUsername());
                dto.setCustomerEmail(u.getEmail());
                dto.setCustomerPhone(u.getPhone());
            }

            if (rev.getPropertyId() != null && propertyMap.containsKey(rev.getPropertyId())) {
                Property p = propertyMap.get(rev.getPropertyId());
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyAddress(p.getAddress());

                if (p.getAgentId() != null && agentMap.containsKey(p.getAgentId())) {
                    User agent = agentMap.get(p.getAgentId());
                    dto.setAgentId(p.getAgentId());
                    dto.setAgentName(agent.getFullName() != null ? agent.getFullName() : agent.getUsername());
                }
            }

            return dto;
        }).filter(dto -> {
            boolean matchesRating = (rating == null) || (dto.getRating() != null && dto.getRating().equals(rating));

            boolean matchesQuery = q.isEmpty() ||
                    (dto.getReviewCode() != null && dto.getReviewCode().toLowerCase().contains(q)) ||
                    (dto.getCustomerName() != null && dto.getCustomerName().toLowerCase().contains(q)) ||
                    (dto.getPropertyTitle() != null && dto.getPropertyTitle().toLowerCase().contains(q)) ||
                    (dto.getComment() != null && dto.getComment().toLowerCase().contains(q)) ||
                    (dto.getPropertyLocation() != null && dto.getPropertyLocation().toLowerCase().contains(q));

            return matchesRating && matchesQuery;
        }).collect(Collectors.toList());

        reviewListCache.put(cacheKey, result);
        reviewListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
        return result;
    }

    private ReviewDto toDto(Review rev) {
        if (rev == null) return null;
        ReviewDto dto = new ReviewDto(rev);

        if (rev.getCustomerId() != null) {
            userRepository.findById(rev.getCustomerId()).ifPresent(user -> {
                dto.setCustomerName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                dto.setCustomerUsername(user.getUsername());
                dto.setCustomerEmail(user.getEmail());
                dto.setCustomerPhone(user.getPhone());
            });
        }

        if (rev.getPropertyId() != null) {
            propertyRepository.findById(rev.getPropertyId()).ifPresent(p -> {
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyAddress(p.getAddress());

                if (p.getAgentId() != null) {
                    dto.setAgentId(p.getAgentId());
                    userRepository.findById(p.getAgentId()).ifPresent(agent -> {
                        dto.setAgentName(agent.getFullName() != null ? agent.getFullName() : agent.getUsername());
                    });
                }
            });
        }

        return dto;
    }
}
