package com.estatex.dto;

import com.estatex.model.FinancialBreakdown;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FinancialBreakdownDto {

    private Long id;
    private String breakdownCode;

    // Customer Info
    private Long customerId;
    private String customerName;
    private String customerUsername;
    private String customerEmail;
    private String customerPhone;

    // Property Info
    private Long propertyId;
    private String propertyTitle;
    private String propertyLocation;
    private String propertyAddress;
    private String propertyType;
    private String listingType;

    // Financial Values
    private BigDecimal propertyPrice;
    private BigDecimal advancePayment;
    private BigDecimal legalFees;
    private BigDecimal stampDuty;
    private BigDecimal otherFees;
    private BigDecimal totalOtherFees;
    private BigDecimal totalInitialPayment;
    private BigDecimal remainingBalance;
    private Double advancePercentage;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String formattedDate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LONG_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    public FinancialBreakdownDto() {
    }

    public FinancialBreakdownDto(FinancialBreakdown fb) {
        if (fb != null) {
            this.id = fb.getId();
            long displayCode = (fb.getId() != null && fb.getId() < 100) ? (200 + fb.getId()) : (fb.getId() != null ? fb.getId() : 0);
            this.breakdownCode = "#FIN-" + displayCode;
            this.customerId = fb.getCustomerId();
            this.propertyId = fb.getPropertyId();
            this.propertyPrice = fb.getPropertyPrice() != null ? fb.getPropertyPrice() : BigDecimal.ZERO;
            this.advancePayment = fb.getAdvancePayment() != null ? fb.getAdvancePayment() : BigDecimal.ZERO;
            this.legalFees = fb.getLegalFees() != null ? fb.getLegalFees() : BigDecimal.ZERO;
            this.stampDuty = fb.getStampDuty() != null ? fb.getStampDuty() : BigDecimal.ZERO;
            this.otherFees = fb.getOtherFees() != null ? fb.getOtherFees() : BigDecimal.ZERO;

            this.totalOtherFees = this.legalFees.add(this.stampDuty).add(this.otherFees);
            this.totalInitialPayment = fb.getTotalInitialPayment() != null ? fb.getTotalInitialPayment() : this.advancePayment.add(this.totalOtherFees);
            this.remainingBalance = fb.getRemainingBalance() != null ? fb.getRemainingBalance() : this.propertyPrice.subtract(this.advancePayment);

            if (this.propertyPrice.compareTo(BigDecimal.ZERO) > 0) {
                this.advancePercentage = this.advancePayment
                        .multiply(BigDecimal.valueOf(100))
                        .divide(this.propertyPrice, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            } else {
                this.advancePercentage = 0.0;
            }

            this.notes = fb.getNotes();
            this.createdAt = fb.getCreatedAt();
            this.updatedAt = fb.getUpdatedAt();

            if (fb.getCreatedAt() != null) {
                this.formattedDate = fb.getCreatedAt().format(DATE_FMT);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBreakdownCode() {
        return breakdownCode;
    }

    public void setBreakdownCode(String breakdownCode) {
        this.breakdownCode = breakdownCode;
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

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
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

    public String getPropertyAddress() {
        return propertyAddress;
    }

    public void setPropertyAddress(String propertyAddress) {
        this.propertyAddress = propertyAddress;
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

    public BigDecimal getPropertyPrice() {
        return propertyPrice;
    }

    public void setPropertyPrice(BigDecimal propertyPrice) {
        this.propertyPrice = propertyPrice;
    }

    public BigDecimal getAdvancePayment() {
        return advancePayment;
    }

    public void setAdvancePayment(BigDecimal advancePayment) {
        this.advancePayment = advancePayment;
    }

    public BigDecimal getLegalFees() {
        return legalFees;
    }

    public void setLegalFees(BigDecimal legalFees) {
        this.legalFees = legalFees;
    }

    public BigDecimal getStampDuty() {
        return stampDuty;
    }

    public void setStampDuty(BigDecimal stampDuty) {
        this.stampDuty = stampDuty;
    }

    public BigDecimal getOtherFees() {
        return otherFees;
    }

    public void setOtherFees(BigDecimal otherFees) {
        this.otherFees = otherFees;
    }

    public BigDecimal getTotalOtherFees() {
        return totalOtherFees;
    }

    public void setTotalOtherFees(BigDecimal totalOtherFees) {
        this.totalOtherFees = totalOtherFees;
    }

    public BigDecimal getTotalInitialPayment() {
        return totalInitialPayment;
    }

    public void setTotalInitialPayment(BigDecimal totalInitialPayment) {
        this.totalInitialPayment = totalInitialPayment;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public Double getAdvancePercentage() {
        return advancePercentage;
    }

    public void setAdvancePercentage(Double advancePercentage) {
        this.advancePercentage = advancePercentage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }
}
