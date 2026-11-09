package com.estatex.dto;

import com.estatex.model.Inquiry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InquiryDto {

    private Long id;
    private String inquiryCode;

    // Customer
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Property
    private Long propertyId;
    private String propertyTitle;
    private String propertyLocation;
    private BigDecimal propertyPrice;
    private String propertyListingType;
    private String propertyImage;

    // Agent
    private Long agentId;
    private String agentName;
    private String agentEmail;
    private String agentPhone;
    private String agentAgency;

    // Inquiry content
    private String message;
    private String agentReply;
    private String status;
    private LocalDateTime createdAt;
    private String sentDate;
    private LocalDateTime repliedAt;
    private String repliedDate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter NICE_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter REPLY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy - hh:mm a");

    public InquiryDto() {
    }

    public InquiryDto(Inquiry inq) {
        if (inq != null) {
            this.id = inq.getId();
            long displayCode = (inq.getId() != null && inq.getId() < 100) ? (100 + inq.getId()) : (inq.getId() != null ? inq.getId() : 0);
            this.inquiryCode = "#INQ-" + displayCode;
            this.customerId = inq.getCustomerId();
            this.propertyId = inq.getPropertyId();
            this.message = inq.getMessage();
            this.agentReply = inq.getAgentReply();
            this.status = inq.getStatus();
            this.createdAt = inq.getCreatedAt();
            this.repliedAt = inq.getRepliedAt();

            if (inq.getCreatedAt() != null) {
                this.sentDate = inq.getCreatedAt().format(DATE_FMT);
            }
            if (inq.getRepliedAt() != null) {
                this.repliedDate = inq.getRepliedAt().format(REPLY_FMT);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInquiryCode() {
        return inquiryCode;
    }

    public void setInquiryCode(String inquiryCode) {
        this.inquiryCode = inquiryCode;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyTitle() {
        return propertyTitle;
    }

    public void setPropertyTitle(String propertyTitle) {
        this.propertyTitle = propertyTitle;
    }

    public String getPropertyLocation() {
        return propertyLocation;
    }

    public void setPropertyLocation(String propertyLocation) {
        this.propertyLocation = propertyLocation;
    }

    public BigDecimal getPropertyPrice() {
        return propertyPrice;
    }

    public void setPropertyPrice(BigDecimal propertyPrice) {
        this.propertyPrice = propertyPrice;
    }

    public String getPropertyListingType() {
        return propertyListingType;
    }

    public void setPropertyListingType(String propertyListingType) {
        this.propertyListingType = propertyListingType;
    }

    public String getPropertyImage() {
        return propertyImage;
    }

    public void setPropertyImage(String propertyImage) {
        this.propertyImage = propertyImage;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAgentReply() {
        return agentReply;
    }

    public void setAgentReply(String agentReply) {
        this.agentReply = agentReply;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSentDate() {
        return sentDate;
    }

    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    public String getRepliedDate() {
        return repliedDate;
    }

    public void setRepliedDate(String repliedDate) {
        this.repliedDate = repliedDate;
    }
}
