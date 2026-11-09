package com.estatex.dto;

import com.estatex.model.Property;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PropertyDto {

    private Long id;
    private Long agentId;
    private String agentName;
    private String agentEmail;
    private String agentPhone;
    private String agentAgency;
    private String agentProfileImage;
    private String title;
    private String propertyType;
    private String listingType;
    private BigDecimal price;
    private String location;
    private String address;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer floorAreaSqft;
    private String description;
    private String amenities;
    private String status;
    private String primaryImage;
    private List<String> images = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PropertyDto() {
    }

    public PropertyDto(Property p) {
        if (p != null) {
            this.id = p.getId();
            this.agentId = p.getAgentId();
            this.title = p.getTitle();
            this.propertyType = p.getPropertyType();
            this.listingType = p.getListingType();
            this.price = p.getPrice();
            this.location = p.getLocation();
            this.address = p.getAddress();
            this.bedrooms = p.getBedrooms();
            this.bathrooms = p.getBathrooms();
            this.floorAreaSqft = p.getFloorAreaSqft();
            this.description = p.getDescription();
            this.amenities = p.getAmenities();
            this.status = p.getStatus() != null ? p.getStatus() : "Active";
            this.createdAt = p.getCreatedAt();
            this.updatedAt = p.getUpdatedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentEmail() {
        return agentEmail;
    }

    public void setAgentEmail(String agentEmail) {
        this.agentEmail = agentEmail;
    }

    public String getAgentPhone() {
        return agentPhone;
    }

    public void setAgentPhone(String agentPhone) {
        this.agentPhone = agentPhone;
    }

    public String getAgentAgency() {
        return agentAgency;
    }

    public void setAgentAgency(String agentAgency) {
        this.agentAgency = agentAgency;
    }

    public String getAgentProfileImage() {
        return agentProfileImage;
    }

    public void setAgentProfileImage(String agentProfileImage) {
        this.agentProfileImage = agentProfileImage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getListingType() {
        return listingType;
    }

    public void setListingType(String listingType) {
        this.listingType = listingType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public Integer getFloorAreaSqft() {
        return floorAreaSqft;
    }

    public void setFloorAreaSqft(Integer floorAreaSqft) {
        this.floorAreaSqft = floorAreaSqft;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryImage() {
        return primaryImage;
    }

    public void setPrimaryImage(String primaryImage) {
        this.primaryImage = primaryImage;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
