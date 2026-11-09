package com.estatex.service;

import com.estatex.dto.FinancialBreakdownDto;
import com.estatex.dto.FinancialBreakdownRequest;
import com.estatex.model.FinancialBreakdown;
import com.estatex.model.Property;
import com.estatex.model.Role;
import com.estatex.model.User;
import com.estatex.repository.FinancialBreakdownRepository;
import com.estatex.repository.PropertyRepository;
import com.estatex.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinancialBreakdownService {

    private static final Logger log = LoggerFactory.getLogger(FinancialBreakdownService.class);

    private final FinancialBreakdownRepository financialBreakdownRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    // Cache with short TTL (10s)
    private final Map<String, List<FinancialBreakdownDto>> breakdownCache = new ConcurrentHashMap<>();
    private final Map<String, Long> breakdownCacheExpiry = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10_000L;

    @Autowired
    public FinancialBreakdownService(FinancialBreakdownRepository financialBreakdownRepository,
                                     PropertyRepository propertyRepository,
                                     UserRepository userRepository,
                                     UserService userService,
                                     JdbcTemplate jdbcTemplate) {
        this.financialBreakdownRepository = financialBreakdownRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS financial_breakdowns (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  customer_id BIGINT NOT NULL," +
                "  property_id BIGINT NOT NULL," +
                "  property_price DECIMAL(15,2) NOT NULL," +
                "  advance_payment DECIMAL(15,2) NOT NULL," +
                "  legal_fees DECIMAL(15,2) DEFAULT 0.00," +
                "  stamp_duty DECIMAL(15,2) DEFAULT 0.00," +
                "  other_fees DECIMAL(15,2) DEFAULT 0.00," +
                "  total_initial_payment DECIMAL(15,2) NOT NULL," +
                "  remaining_balance DECIMAL(15,2) NOT NULL," +
                "  notes TEXT," +
                "  created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP NULL," +
                "  INDEX idx_fb_customer (customer_id)," +
                "  INDEX idx_fb_property (property_id)" +
                ")"
            );
            log.info("FinancialBreakdownService: Verified 'financial_breakdowns' table in database.");

            // Seed sample breakdown if table is empty
            if (financialBreakdownRepository.count() == 0) {
                List<Property> properties = propertyRepository.findAll();
                User customer = userRepository.findByUsernameIgnoreCase("dilshan@user")
                        .orElseGet(() -> userRepository.findAll().stream()
                                .filter(u -> u.getRole() == Role.CUSTOMER)
                                .findFirst()
                                .orElse(null));

                if (customer != null && !properties.isEmpty()) {
                    Property p1 = properties.get(0);
                    BigDecimal price1 = p1.getPrice() != null ? p1.getPrice() : BigDecimal.valueOf(38500000.00);
                    BigDecimal advance1 = price1.multiply(BigDecimal.valueOf(0.20)); // 20% down payment
                    BigDecimal legal1 = BigDecimal.valueOf(75000.00);
                    BigDecimal stamp1 = price1.multiply(BigDecimal.valueOf(0.04)); // 4% stamp duty
                    BigDecimal other1 = BigDecimal.valueOf(35000.00);

                    FinancialBreakdown fb1 = new FinancialBreakdown(
                            customer.getId(), p1.getId(), price1, advance1, legal1, stamp1, other1,
                            "Standard 20% initial equity plan with legal deed conveyance & local council transfer stamp duty."
                    );
                    fb1.setCreatedAt(LocalDateTime.of(2026, 10, 15, 11, 20));
                    financialBreakdownRepository.save(fb1);

                    if (properties.size() > 1) {
                        Property p2 = properties.get(1);
                        BigDecimal price2 = p2.getPrice() != null ? p2.getPrice() : BigDecimal.valueOf(250000.00);
                        BigDecimal advance2 = price2.multiply(BigDecimal.valueOf(0.25));
                        BigDecimal legal2 = BigDecimal.valueOf(50000.00);
                        BigDecimal stamp2 = price2.multiply(BigDecimal.valueOf(0.04));
                        BigDecimal other2 = BigDecimal.valueOf(25000.00);

                        FinancialBreakdown fb2 = new FinancialBreakdown(
                                customer.getId(), p2.getId(), price2, advance2, legal2, stamp2, other2,
                                "Pre-approved mortgage breakdown with 25% initial buyer down payment."
                        );
                        fb2.setCreatedAt(LocalDateTime.of(2026, 10, 18, 14, 45));
                        financialBreakdownRepository.save(fb2);
                    }

                    financialBreakdownRepository.flush();
                    log.info("FinancialBreakdownService: Seeded initial financial breakdowns.");
                }
            }
        } catch (Exception e) {
            log.warn("FinancialBreakdownService: Table initialization warning: {}", e.getMessage());
        }
    }

    public void invalidateCache() {
        breakdownCache.clear();
        breakdownCacheExpiry.clear();
    }

    /**
     * Create a new financial breakdown.
     */
    public FinancialBreakdownDto createBreakdown(FinancialBreakdownRequest req, String customerIdentifier) {
        if (req.getPropertyId() == null) {
            throw new IllegalArgumentException("Property must be selected.");
        }
        if (req.getPropertyPrice() == null || req.getPropertyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Property price must be greater than zero.");
        }
        if (req.getAdvancePayment() == null || req.getAdvancePayment().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Advance payment cannot be negative.");
        }

        Property property = propertyRepository.findById(req.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property listing not found with ID: " + req.getPropertyId()));

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
            throw new IllegalArgumentException("Could not identify customer. Please log in.");
        }

        FinancialBreakdown fb = new FinancialBreakdown(
                customer.getId(),
                property.getId(),
                req.getPropertyPrice(),
                req.getAdvancePayment(),
                req.getLegalFees(),
                req.getStampDuty(),
                req.getOtherFees(),
                req.getNotes() != null ? req.getNotes().trim() : ""
        );

        FinancialBreakdown saved = financialBreakdownRepository.saveAndFlush(fb);
        invalidateCache();
        return toDto(saved);
    }

    /**
     * Update an existing financial breakdown.
     */
    public FinancialBreakdownDto updateBreakdown(Long id, FinancialBreakdownRequest req, String customerIdentifier) {
        FinancialBreakdown fb = financialBreakdownRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Financial breakdown not found with ID: " + id));

        if (req.getPropertyId() != null) {
            Property property = propertyRepository.findById(req.getPropertyId())
                    .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + req.getPropertyId()));
            fb.setPropertyId(property.getId());
        }

        if (req.getPropertyPrice() != null) {
            if (req.getPropertyPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Property price must be greater than zero.");
            }
            fb.setPropertyPrice(req.getPropertyPrice());
        }

        if (req.getAdvancePayment() != null) {
            if (req.getAdvancePayment().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Advance payment cannot be negative.");
            }
            fb.setAdvancePayment(req.getAdvancePayment());
        }

        if (req.getLegalFees() != null) {
            fb.setLegalFees(req.getLegalFees());
        }

        if (req.getStampDuty() != null) {
            fb.setStampDuty(req.getStampDuty());
        }

        if (req.getOtherFees() != null) {
            fb.setOtherFees(req.getOtherFees());
        }

        if (req.getNotes() != null) {
            fb.setNotes(req.getNotes().trim());
        }

        fb.recalculateTotals();
        fb.setUpdatedAt(LocalDateTime.now());

        FinancialBreakdown saved = financialBreakdownRepository.saveAndFlush(fb);
        invalidateCache();
        return toDto(saved);
    }

    /**
     * Delete a financial breakdown by ID.
     */
    public void deleteBreakdown(Long id, String customerIdentifier) {
        FinancialBreakdown fb = financialBreakdownRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Financial breakdown not found with ID: " + id));

        financialBreakdownRepository.delete(fb);
        invalidateCache();
    }

    /**
     * Retrieve single financial breakdown by ID.
     */
    @Transactional(readOnly = true)
    public FinancialBreakdownDto getBreakdownById(Long id) {
        FinancialBreakdown fb = financialBreakdownRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Financial breakdown not found with ID: " + id));
        return toDto(fb);
    }

    /**
     * Retrieve financial breakdowns with optional filters.
     */
    @Transactional(readOnly = true)
    public List<FinancialBreakdownDto> getBreakdowns(Long customerId, String customerUsername,
                                                     Long propertyId, String search) {

        String cacheKey = String.format("fb:%s:%s:%s:%s", customerId, customerUsername, propertyId, search);
        Long exp = breakdownCacheExpiry.get(cacheKey);
        if (exp != null && System.currentTimeMillis() < exp) {
            List<FinancialBreakdownDto> cached = breakdownCache.get(cacheKey);
            if (cached != null) return cached;
        }

        List<FinancialBreakdown> list;

        Long resolvedCustId = null;
        if (customerId != null && customerId > 0) {
            resolvedCustId = customerId;
        } else if (customerUsername != null && !customerUsername.isBlank()) {
            User u = userService.findUserByIdentifier(customerUsername);
            if (u != null) resolvedCustId = u.getId();
        }

        final Long finalCustId = resolvedCustId;
        if (propertyId != null && propertyId > 0 && finalCustId != null) {
            list = financialBreakdownRepository.findByPropertyIdOrderByIdDesc(propertyId).stream()
                    .filter(fb -> fb.getCustomerId().equals(finalCustId))
                    .collect(Collectors.toList());
        } else if (propertyId != null && propertyId > 0) {
            list = financialBreakdownRepository.findByPropertyIdOrderByIdDesc(propertyId);
        } else if (finalCustId != null) {
            list = financialBreakdownRepository.findByCustomerIdOrderByIdDesc(finalCustId);
        } else {
            list = financialBreakdownRepository.findAllByOrderByIdDesc();
        }

        if (list.isEmpty()) {
            breakdownCache.put(cacheKey, Collections.emptyList());
            breakdownCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
            return Collections.emptyList();
        }

        // Batch fetch Customers and Properties
        Set<Long> customerIds = list.stream().map(FinancialBreakdown::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> propertyIds = list.stream().map(FinancialBreakdown::getPropertyId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> customerMap = customerIds.isEmpty() ? Collections.emptyMap() :
                userRepository.findAllById(customerIds).stream()
                        .collect(Collectors.toMap(User::getId, java.util.function.Function.identity(), (a, b) -> a));

        Map<Long, Property> propertyMap = propertyIds.isEmpty() ? Collections.emptyMap() :
                propertyRepository.findAllById(propertyIds).stream()
                        .collect(Collectors.toMap(Property::getId, java.util.function.Function.identity(), (a, b) -> a));

        String q = (search != null) ? search.trim().toLowerCase() : "";

        List<FinancialBreakdownDto> result = list.stream().map(fb -> {
            FinancialBreakdownDto dto = new FinancialBreakdownDto(fb);

            if (fb.getCustomerId() != null && customerMap.containsKey(fb.getCustomerId())) {
                User u = customerMap.get(fb.getCustomerId());
                dto.setCustomerName(u.getFullName() != null ? u.getFullName() : u.getUsername());
                dto.setCustomerUsername(u.getUsername());
                dto.setCustomerEmail(u.getEmail());
                dto.setCustomerPhone(u.getPhone());
            }

            if (fb.getPropertyId() != null && propertyMap.containsKey(fb.getPropertyId())) {
                Property p = propertyMap.get(fb.getPropertyId());
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyAddress(p.getAddress());
                dto.setPropertyType(p.getPropertyType());
                dto.setListingType(p.getListingType());
            }

            return dto;
        }).filter(dto -> {
            boolean matchesQuery = q.isEmpty() ||
                    (dto.getBreakdownCode() != null && dto.getBreakdownCode().toLowerCase().contains(q)) ||
                    (dto.getPropertyTitle() != null && dto.getPropertyTitle().toLowerCase().contains(q)) ||
                    (dto.getPropertyLocation() != null && dto.getPropertyLocation().toLowerCase().contains(q)) ||
                    (dto.getNotes() != null && dto.getNotes().toLowerCase().contains(q));

            return matchesQuery;
        }).collect(Collectors.toList());

        breakdownCache.put(cacheKey, result);
        breakdownCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
        return result;
    }

    private FinancialBreakdownDto toDto(FinancialBreakdown fb) {
        if (fb == null) return null;
        FinancialBreakdownDto dto = new FinancialBreakdownDto(fb);

        if (fb.getCustomerId() != null) {
            userRepository.findById(fb.getCustomerId()).ifPresent(user -> {
                dto.setCustomerName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                dto.setCustomerUsername(user.getUsername());
                dto.setCustomerEmail(user.getEmail());
                dto.setCustomerPhone(user.getPhone());
            });
        }

        if (fb.getPropertyId() != null) {
            propertyRepository.findById(fb.getPropertyId()).ifPresent(p -> {
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyAddress(p.getAddress());
                dto.setPropertyType(p.getPropertyType());
                dto.setListingType(p.getListingType());
            });
        }

        return dto;
    }
}
