package com.sismics.docs.core.model.jpa;

import jakarta.persistence.*;

import java.util.Date;

/**
 * Registration request entity.
 * 
 */
@Entity
@Table(name = "T_REGISTER_REQUEST")
public class RegisterRequest implements Loggable {
    
    /**
     * Request ID.
     */
    @Id
    @Column(name = "RRQ_ID_C", length = 36)
    private String id;

    /**
     * Requester name.
     */
    @Column(name = "RRQ_NAME_C", nullable = false, length = 100)
    private String username;

    /**
     * Requester email.
     */
    @Column(name = "RRQ_EMAIL_C", nullable = false, unique = true, length = 100)
    private String email;


    /**
     * Requester password.
     */
    @Column(name = "RRQ_PASSWORD_C", nullable = false, length = 100)
    private String password;

    /**
     * Status: pending / accepted / rejected
     */
    @Column(name = "RRQ_STATUS_C", nullable = false, length = 20)
    private String status;

    /**
     * Request submission date.
     */
    @Column(name = "RRQ_CREATEDATE_D", nullable = false)
    private Date createDate;

    /**
     * Review date.
     */
    @Column(name = "RRQ_REVIEWDATE_D")
    private Date reviewDate;

    /**
     * Admin user ID who reviewed.
     */
    @Column(name = "RRQ_REVIEWUSERID_C", length = 36)
    private String reviewedBy;

    /**
     * Optional rejection reason.
     */
    @Column(name = "RRQ_REASON_C", length = 1000)
    private String reason;

    /**
     * Soft delete.
     */
    @Column(name = "RRQ_DELETEDATE_D")
    private Date deleteDate;

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public RegisterRequest setId(String id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public RegisterRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public RegisterRequest setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RegisterRequest setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public RegisterRequest setStatus(String status) {
        this.status = status;
        return this;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public RegisterRequest setCreateDate(Date createDate) {
        this.createDate = createDate;
        return this;
    }

    public Date getReviewDate() {
        return reviewDate;
    }

    public RegisterRequest setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
        return this;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public RegisterRequest setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public RegisterRequest setReason(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public Date getDeleteDate() {
        return deleteDate;
    }

    public RegisterRequest setDeleteDate(Date deleteDate) {
        this.deleteDate = deleteDate;
        return this;
    }

    @Override
    public String toMessage() {
        return username;
    }
}
