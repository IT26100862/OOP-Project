package com.estatex.service;

import com.estatex.dto.PropertyDto;
import com.estatex.dto.PropertyRequest;
import com.estatex.model.Property;
import com.estatex.model.PropertyImage;
import com.estatex.model.Role;
import com.estatex.model.User;
import com.estatex.repository.PropertyImageRepository;
import com.estatex.repository.PropertyRepository;
import com.estatex.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public PropertyService(PropertyRepository propertyRepository,
                           PropertyImageRepository propertyImageRepository,
                           UserRepository userRepository,
                           UserService userService) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Resolves Agent ID from given identifier (ID, username, email).
     */
    public Long resolveAgentId(Long directAgentId, String agentIdentifier) {
        if (directAgentId != null && directAgentId > 0) {
            return directAgentId;
        }
        if (agentIdentifier != null && !agentIdentifier.isBlank()) {
            User user = userService.findUserByIdentifier(agentIdentifier);
            if (user != null) {
                return user.getId();
            }
        }
        // Fallback to first available agent in database
        List<User> agents = userRepository.findByRole(Role.AGENT);
        if (!agents.isEmpty()) {
            return agents.get(0).getId();
        }
        return 1L;
    }

    /**
     * List properties with optional filters.
     */
    @Transactional(readOnly = true)
    public List<PropertyDto> getProperties(Long agentId, String agentIdentifier, String search, String type, String status) {
        Long resolvedAgentId = null;
        if (agentId != null && agentId > 0) {
            resolvedAgentId = agentId;
        } else if (agentIdentifier != null && !agentIdentifier.isBlank()) {
            User u = userService.findUserByIdentifier(agentIdentifier);
            if (u != null) {
                resolvedAgentId = u.getId();
            }
        }

        List<Property> list;
        if (resolvedAgentId != null) {
            list = propertyRepository.findByAgentIdOrderByIdDesc(resolvedAgentId);
        } else {
            list = propertyRepository.findAllByOrderByIdDesc();
        }

        String q = (search != null) ? search.trim().toLowerCase() : "";
        String typeFilter = (type != null) ? type.trim().toLowerCase() : "";
        String statusFilter = (status != null) ? status.trim().toLowerCase() : "";

        return list.stream().filter(p -> {
            boolean matchesQuery = q.isEmpty() ||
                    (p.getTitle() != null && p.getTitle().toLowerCase().contains(q)) ||
                    (p.getLocation() != null && p.getLocation().toLowerCase().contains(q)) ||
                    (p.getAddress() != null && p.getAddress().toLowerCase().contains(q)) ||
                    ("prop-" + p.getId()).contains(q) ||
                    String.valueOf(p.getId()).equals(q);

            boolean matchesType = typeFilter.isEmpty() ||
                    (p.getPropertyType() != null && p.getPropertyType().toLowerCase().equals(typeFilter));

            boolean matchesStatus = statusFilter.isEmpty() ||
                    (p.getStatus() != null && p.getStatus().toLowerCase().equals(statusFilter));

            return matchesQuery && matchesType && matchesStatus;
        }).map(this::toDto).collect(Collectors.toList());
    }

    // High-performance in-memory cache to eliminate remote WAN database latency
    private final Map<Long, PropertyDto> propertyCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, Long> propertyCacheExpiry = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L; // 10 minutes TTL

    public void invalidateCache(Long propertyId) {
        if (propertyId != null) {
            propertyCache.remove(propertyId);
            propertyCacheExpiry.remove(propertyId);
        } else {
            propertyCache.clear();
            propertyCacheExpiry.clear();
        }
    }

    /**
     * Retrieve single property by ID.
     */
    @Transactional(readOnly = true)
    public PropertyDto getPropertyById(Long id) {
        Long exp = propertyCacheExpiry.get(id);
        if (exp != null && System.currentTimeMillis() < exp) {
            PropertyDto cached = propertyCache.get(id);
            if (cached != null) {
                return cached;
            }
        }

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + id));
        PropertyDto dto = toDto(property);
        propertyCache.put(id, dto);
        propertyCacheExpiry.put(id, System.currentTimeMillis() + CACHE_TTL_MS);
        return dto;
    }

    /**
     * Create new property listing.
     */
    public PropertyDto createProperty(PropertyRequest request, String agentIdentifier) {
        Long agentId = resolveAgentId(request.getAgentId(),
                request.getAgentUsername() != null ? request.getAgentUsername() : agentIdentifier);

        Property p = new Property();
        p.setAgentId(agentId);
        p.setTitle(request.getTitle().trim());
        p.setPropertyType(request.getPropertyType().trim());
        p.setListingType(request.getListingType().trim());
        p.setPrice(request.getPrice());
        p.setLocation(request.getLocation().trim());
        p.setAddress(request.getAddress().trim());
        p.setBedrooms(request.getBedrooms());
        p.setBathrooms(request.getBathrooms());
        p.setFloorAreaSqft(request.getFloorAreaSqft());
        p.setDescription(request.getDescription());
        p.setAmenities(request.getAmenities());
        p.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim() : "Active");

        Property saved = propertyRepository.saveAndFlush(p);

        // Save images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            boolean first = true;
            for (String imgUrl : request.getImages()) {
                if (imgUrl != null && !imgUrl.isBlank()) {
                    propertyImageRepository.save(new PropertyImage(saved.getId(), imgUrl.trim(), first));
                    first = false;
                }
            }
        }

        invalidateCache(null);
        return toDto(saved);
    }

    /**
     * Update existing property listing.
     */
    public PropertyDto updateProperty(Long id, PropertyRequest request, String agentIdentifier) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + id));

        p.setTitle(request.getTitle().trim());
        p.setPropertyType(request.getPropertyType().trim());
        p.setListingType(request.getListingType().trim());
        p.setPrice(request.getPrice());
        p.setLocation(request.getLocation().trim());
        p.setAddress(request.getAddress().trim());
        p.setBedrooms(request.getBedrooms());
        p.setBathrooms(request.getBathrooms());
        p.setFloorAreaSqft(request.getFloorAreaSqft());
        p.setDescription(request.getDescription());
        if (request.getAmenities() != null) p.setAmenities(request.getAmenities());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            p.setStatus(request.getStatus().trim());
        }

        Property saved = propertyRepository.saveAndFlush(p);

        // Update images if provided
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            propertyImageRepository.deleteByPropertyId(saved.getId());
            boolean first = true;
            for (String imgUrl : request.getImages()) {
                if (imgUrl != null && !imgUrl.isBlank()) {
                    propertyImageRepository.save(new PropertyImage(saved.getId(), imgUrl.trim(), first));
                    first = false;
                }
            }
        }

        invalidateCache(id);
        return toDto(saved);
    }

    /**
     * Delete property listing.
     */
    public void deleteProperty(Long id, String agentIdentifier) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + id));

        // Delete associated images
        propertyImageRepository.deleteByPropertyId(id);
        propertyRepository.delete(p);
        invalidateCache(id);
    }

    /**
     * Updates listing status (e.g. Active, Under Review, Sold).
     */
    public PropertyDto updatePropertyStatus(Long id, String status) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with ID: " + id));
        p.setStatus(status != null && !status.isBlank() ? status.trim() : "Active");
        Property saved = propertyRepository.saveAndFlush(p);
        invalidateCache(id);
        return toDto(saved);
    }

    /**
     * Calculates summary metrics for agent dashboard.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAgentStats(Long agentId, String agentIdentifier) {
        Long resolvedId = resolveAgentId(agentId, agentIdentifier);
        long total = propertyRepository.countByAgentId(resolvedId);
        long active = propertyRepository.countByAgentIdAndStatus(resolvedId, "Active");
        long underReview = propertyRepository.countByAgentIdAndStatus(resolvedId, "Under Review");
        long sold = propertyRepository.countByAgentIdAndStatus(resolvedId, "Sold");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProperties", total);
        stats.put("activeListings", active);
        stats.put("underReviewListings", underReview);
        stats.put("soldListings", sold);
        return stats;
    }

    private PropertyDto toDto(Property p) {
        if (p == null || p.getId() == null) return null;
        Long exp = propertyCacheExpiry.get(p.getId());
        if (exp != null && System.currentTimeMillis() < exp) {
            PropertyDto cached = propertyCache.get(p.getId());
            if (cached != null) {
                return cached;
            }
        }

        PropertyDto dto = new PropertyDto(p);

        // Lookup agent info
        if (p.getAgentId() != null) {
            userRepository.findById(p.getAgentId()).ifPresent(user -> {
                dto.setAgentName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                dto.setAgentEmail(user.getEmail());
                dto.setAgentPhone(user.getPhone());
                dto.setAgentAgency(user.getAddress() != null && !user.getAddress().isBlank() ? user.getAddress() : "EstateX Premier Partner");
                dto.setAgentProfileImage(user.getProfileImage());
            });
        }

        // Attach images
        List<PropertyImage> images = propertyImageRepository.findByPropertyId(p.getId());
        if (!images.isEmpty()) {
            dto.setImages(images.stream().map(PropertyImage::getImageUrl).collect(Collectors.toList()));
            String primary = images.stream().filter(PropertyImage::getIsPrimary)
                    .map(PropertyImage::getImageUrl).findFirst().orElse(images.get(0).getImageUrl());
            dto.setPrimaryImage(primary);
        } else {
            // Default elegant property placeholder
            dto.setPrimaryImage("https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=800&q=80");
        }

        propertyCache.put(p.getId(), dto);
        propertyCacheExpiry.put(p.getId(), System.currentTimeMillis() + CACHE_TTL_MS);
        return dto;
    }
}
