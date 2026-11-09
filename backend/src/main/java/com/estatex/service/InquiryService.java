package com.estatex.service;

import com.estatex.dto.InquiryDto;
import com.estatex.dto.InquiryReplyRequest;
import com.estatex.dto.InquiryRequest;
import com.estatex.model.Inquiry;
import com.estatex.model.Property;
import com.estatex.model.User;
import com.estatex.repository.InquiryRepository;
import com.estatex.repository.PropertyRepository;
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
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final String DEFAULT_IMAGE = "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80";

    private final InquiryRepository inquiryRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;

    // Fast in-memory cache with short TTL (10 seconds)
    private final Map<String, List<InquiryDto>> inquiryListCache = new ConcurrentHashMap<>();
    private final Map<String, Long> inquiryListCacheExpiry = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10_000L;

    @Autowired
    public InquiryService(InquiryRepository inquiryRepository,
                          PropertyRepository propertyRepository,
                          UserRepository userRepository,
                          UserService userService,
                          EmailService emailService,
                          JdbcTemplate jdbcTemplate) {
        this.inquiryRepository = inquiryRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS inquiries (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  customer_id BIGINT NOT NULL," +
                "  property_id BIGINT NOT NULL," +
                "  message TEXT NOT NULL," +
                "  agent_reply TEXT," +
                "  status VARCHAR(50) NOT NULL DEFAULT 'Pending'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  replied_at TIMESTAMP NULL," +
                "  INDEX idx_inq_customer (customer_id)," +
                "  INDEX idx_inq_property (property_id)" +
                ")"
            );
            log.info("InquiryService: Verified 'inquiries' table in database.");
        } catch (Exception e) {
            log.warn("InquiryService: Table initialization warning: {}", e.getMessage());
        }
    }

    public void invalidateCache() {
        inquiryListCache.clear();
        inquiryListCacheExpiry.clear();
    }

    /**
     * Create a new inquiry from a customer for a property listing.
     */
    public InquiryDto createInquiry(InquiryRequest req, String customerIdentifier) {
        if (req.getPropertyId() == null) {
            throw new IllegalArgumentException("Property ID is required.");
        }
        if (req.getMessage() == null || req.getMessage().trim().isBlank()) {
            throw new IllegalArgumentException("Inquiry message cannot be empty.");
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
            // Find any customer or fallback
            customer = userRepository.findAll().stream()
                    .filter(u -> "CUSTOMER".equalsIgnoreCase(u.getRole().name()))
                    .findFirst()
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        }

        if (customer == null) {
            throw new IllegalArgumentException("Could not identify customer. Please log in before sending an inquiry.");
        }

        Inquiry inquiry = new Inquiry(customer.getId(), property.getId(), req.getMessage().trim());
        inquiry.setStatus("Pending");
        inquiry.setCreatedAt(LocalDateTime.now());

        Inquiry saved = inquiryRepository.saveAndFlush(inquiry);
        invalidateCache();

        // Optional non-blocking notification to agent
        try {
            if (property.getAgentId() != null) {
                userRepository.findById(property.getAgentId()).ifPresent(agent -> {
                    log.info("New inquiry #{} for agent {} regarding property '{}'",
                            saved.getId(), agent.getEmail(), property.getTitle());
                });
            }
        } catch (Exception e) {
            log.debug("Agent email notification skipped: {}", e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Agent replies to a customer inquiry.
     */
    public InquiryDto replyInquiry(Long inquiryId, InquiryReplyRequest req, String agentIdentifier) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found with ID: " + inquiryId));

        if (req.getReply() == null || req.getReply().trim().isBlank()) {
            throw new IllegalArgumentException("Reply message cannot be empty.");
        }

        inquiry.setAgentReply(req.getReply().trim());
        inquiry.setStatus("Replied");
        inquiry.setRepliedAt(LocalDateTime.now());

        Inquiry saved = inquiryRepository.saveAndFlush(inquiry);
        invalidateCache();

        // Optional notification to customer
        try {
            userRepository.findById(saved.getCustomerId()).ifPresent(cust -> {
                log.info("Inquiry #{} replied. Customer: {}", saved.getId(), cust.getEmail());
            });
        } catch (Exception e) {
            log.debug("Customer email notification skipped: {}", e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Retrieve inquiries with flexible filters (customer, agent, property, status, search).
     */
    @Transactional(readOnly = true)
    public List<InquiryDto> getInquiries(Long customerId, String customerUsername,
                                         Long agentId, String agentUsername,
                                         Long propertyId, String status, String search) {

        String cacheKey = String.format("inq:%s:%s:%s:%s:%s:%s:%s",
                customerId, customerUsername, agentId, agentUsername, propertyId, status, search);

        Long exp = inquiryListCacheExpiry.get(cacheKey);
        if (exp != null && System.currentTimeMillis() < exp) {
            List<InquiryDto> cached = inquiryListCache.get(cacheKey);
            if (cached != null) return cached;
        }

        List<Inquiry> list;

        // 1. Resolve Customer
        Long resolvedCustId = null;
        if (customerId != null && customerId > 0) {
            resolvedCustId = customerId;
        } else if (customerUsername != null && !customerUsername.isBlank()) {
            User u = userService.findUserByIdentifier(customerUsername);
            if (u != null) resolvedCustId = u.getId();
        }

        // 2. Resolve Agent
        Long resolvedAgentId = null;
        if (agentId != null && agentId > 0) {
            resolvedAgentId = agentId;
        } else if (agentUsername != null && !agentUsername.isBlank()) {
            User u = userService.findUserByIdentifier(agentUsername);
            if (u != null) resolvedAgentId = u.getId();
        }

        if (resolvedCustId != null) {
            list = inquiryRepository.findByCustomerIdOrderByIdDesc(resolvedCustId);
        } else if (resolvedAgentId != null) {
            List<Property> agentProps = propertyRepository.findByAgentIdOrderByIdDesc(resolvedAgentId);
            if (agentProps.isEmpty()) {
                inquiryListCache.put(cacheKey, Collections.emptyList());
                inquiryListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
                return Collections.emptyList();
            }
            List<Long> propIds = agentProps.stream().map(Property::getId).collect(Collectors.toList());
            list = inquiryRepository.findByPropertyIdInOrderByIdDesc(propIds);
        } else if (propertyId != null && propertyId > 0) {
            list = inquiryRepository.findByPropertyIdOrderByIdDesc(propertyId);
        } else {
            list = inquiryRepository.findAllByOrderByIdDesc();
        }

        if (list.isEmpty()) {
            inquiryListCache.put(cacheKey, Collections.emptyList());
            inquiryListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
            return Collections.emptyList();
        }

        // Batch fetch Customers, Properties, and Agents in single queries
        Set<Long> customerIds = list.stream().map(Inquiry::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> propertyIds = list.stream().map(Inquiry::getPropertyId).filter(Objects::nonNull).collect(Collectors.toSet());

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

        String stFilter = (status != null) ? status.trim().toLowerCase() : "";
        String q = (search != null) ? search.trim().toLowerCase() : "";

        List<InquiryDto> result = list.stream().map(inq -> {
            InquiryDto dto = new InquiryDto(inq);

            if (inq.getCustomerId() != null && customerMap.containsKey(inq.getCustomerId())) {
                User u = customerMap.get(inq.getCustomerId());
                dto.setCustomerName(u.getFullName() != null ? u.getFullName() : u.getUsername());
                dto.setCustomerEmail(u.getEmail());
                dto.setCustomerPhone(u.getPhone());
            }

            if (inq.getPropertyId() != null && propertyMap.containsKey(inq.getPropertyId())) {
                Property p = propertyMap.get(inq.getPropertyId());
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyPrice(p.getPrice());
                dto.setPropertyListingType(p.getListingType());
                dto.setPropertyImage(DEFAULT_IMAGE);

                if (p.getAgentId() != null && agentMap.containsKey(p.getAgentId())) {
                    User agent = agentMap.get(p.getAgentId());
                    dto.setAgentId(p.getAgentId());
                    dto.setAgentName(agent.getFullName() != null ? agent.getFullName() : agent.getUsername());
                    dto.setAgentEmail(agent.getEmail());
                    dto.setAgentPhone(agent.getPhone());
                    dto.setAgentAgency(agent.getAddress() != null ? agent.getAddress() : "EstateX Partner");
                }
            } else {
                dto.setPropertyImage(DEFAULT_IMAGE);
            }

            return dto;
        }).filter(dto -> {
            boolean matchesStatus = stFilter.isEmpty() ||
                    (dto.getStatus() != null && dto.getStatus().toLowerCase().equals(stFilter));

            boolean matchesQuery = q.isEmpty() ||
                    (dto.getInquiryCode() != null && dto.getInquiryCode().toLowerCase().contains(q)) ||
                    (dto.getCustomerName() != null && dto.getCustomerName().toLowerCase().contains(q)) ||
                    (dto.getPropertyTitle() != null && dto.getPropertyTitle().toLowerCase().contains(q)) ||
                    (dto.getMessage() != null && dto.getMessage().toLowerCase().contains(q)) ||
                    (dto.getAgentReply() != null && dto.getAgentReply().toLowerCase().contains(q));

            return matchesStatus && matchesQuery;
        }).collect(Collectors.toList());

        inquiryListCache.put(cacheKey, result);
        inquiryListCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_TTL_MS);
        return result;
    }

    /**
     * Retrieve single inquiry by ID.
     */
    @Transactional(readOnly = true)
    public InquiryDto getInquiryById(Long id) {
        Inquiry inq = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found with ID: " + id));
        return toDto(inq);
    }

    /**
     * Delete an inquiry.
     */
    public void deleteInquiry(Long id, String username) {
        Inquiry inq = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found with ID: " + id));
        inquiryRepository.delete(inq);
        invalidateCache();
    }

    private InquiryDto toDto(Inquiry inq) {
        if (inq == null) return null;
        InquiryDto dto = new InquiryDto(inq);

        if (inq.getCustomerId() != null) {
            userRepository.findById(inq.getCustomerId()).ifPresent(user -> {
                dto.setCustomerName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                dto.setCustomerEmail(user.getEmail());
                dto.setCustomerPhone(user.getPhone());
            });
        }

        if (inq.getPropertyId() != null) {
            propertyRepository.findById(inq.getPropertyId()).ifPresent(p -> {
                dto.setPropertyTitle(p.getTitle());
                dto.setPropertyLocation(p.getLocation());
                dto.setPropertyPrice(p.getPrice());
                dto.setPropertyListingType(p.getListingType());
                dto.setPropertyImage(DEFAULT_IMAGE);

                if (p.getAgentId() != null) {
                    dto.setAgentId(p.getAgentId());
                    userRepository.findById(p.getAgentId()).ifPresent(agent -> {
                        dto.setAgentName(agent.getFullName() != null ? agent.getFullName() : agent.getUsername());
                        dto.setAgentEmail(agent.getEmail());
                        dto.setAgentPhone(agent.getPhone());
                        dto.setAgentAgency(agent.getAddress() != null ? agent.getAddress() : "EstateX Partner");
                    });
                }
            });
        }

        return dto;
    }
}
