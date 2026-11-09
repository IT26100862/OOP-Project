package com.estatex.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class FinancialBreakdownRequest {

    private Long propertyId;

    @NotNull(message = "Property price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Property price must be greater than 0")
    private BigDecimal propertyPrice;

    @NotNull(message = "Advance payment is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Advance payment cannot be negative")
    private BigDecimal advancePayment;

    private BigDecimal legalFees;
    private BigDecimal stampDuty;
    private BigDecimal otherFees;
    private String notes;
    private String customerUsername;

    public FinancialBreakdownRequest() {
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }
}
