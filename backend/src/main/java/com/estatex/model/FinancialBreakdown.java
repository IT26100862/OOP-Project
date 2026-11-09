package com.estatex.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_breakdowns")
public class FinancialBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "property_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal propertyPrice;

    @Column(name = "advance_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal advancePayment;

    @Column(name = "legal_fees", precision = 15, scale = 2)
    private BigDecimal legalFees = BigDecimal.ZERO;

    @Column(name = "stamp_duty", precision = 15, scale = 2)
    private BigDecimal stampDuty = BigDecimal.ZERO;

    @Column(name = "other_fees", precision = 15, scale = 2)
    private BigDecimal otherFees = BigDecimal.ZERO;

    @Column(name = "total_initial_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInitialPayment;

    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public FinancialBreakdown() {
    }

    public FinancialBreakdown(Long customerId, Long propertyId, BigDecimal propertyPrice,
                              BigDecimal advancePayment, BigDecimal legalFees,
                              BigDecimal stampDuty, BigDecimal otherFees, String notes) {
        this.customerId = customerId;
        this.propertyId = propertyId;
        this.propertyPrice = propertyPrice;
        this.advancePayment = advancePayment;
        this.legalFees = legalFees != null ? legalFees : BigDecimal.ZERO;
        this.stampDuty = stampDuty != null ? stampDuty : BigDecimal.ZERO;
        this.otherFees = otherFees != null ? otherFees : BigDecimal.ZERO;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        recalculateTotals();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        recalculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateTotals();
    }

    public void recalculateTotals() {
        BigDecimal price = this.propertyPrice != null ? this.propertyPrice : BigDecimal.ZERO;
        BigDecimal advance = this.advancePayment != null ? this.advancePayment : BigDecimal.ZERO;
        BigDecimal legal = this.legalFees != null ? this.legalFees : BigDecimal.ZERO;
        BigDecimal stamp = this.stampDuty != null ? this.stampDuty : BigDecimal.ZERO;
        BigDecimal other = this.otherFees != null ? this.otherFees : BigDecimal.ZERO;

        BigDecimal totalFees = legal.add(stamp).add(other);
        this.totalInitialPayment = advance.add(totalFees);
        this.remainingBalance = price.subtract(advance);
        if (this.remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
            this.remainingBalance = BigDecimal.ZERO;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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
        recalculateTotals();
    }

    public BigDecimal getAdvancePayment() {
        return advancePayment;
    }

    public void setAdvancePayment(BigDecimal advancePayment) {
        this.advancePayment = advancePayment;
        recalculateTotals();
    }

    public BigDecimal getLegalFees() {
        return legalFees;
    }

    public void setLegalFees(BigDecimal legalFees) {
        this.legalFees = legalFees;
        recalculateTotals();
    }

    public BigDecimal getStampDuty() {
        return stampDuty;
    }

    public void setStampDuty(BigDecimal stampDuty) {
        this.stampDuty = stampDuty;
        recalculateTotals();
    }

    public BigDecimal getOtherFees() {
        return otherFees;
    }

    public void setOtherFees(BigDecimal otherFees) {
        this.otherFees = otherFees;
        recalculateTotals();
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
}
