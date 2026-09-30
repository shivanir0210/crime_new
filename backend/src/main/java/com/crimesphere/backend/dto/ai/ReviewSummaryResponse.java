package com.crimesphere.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReviewSummaryResponse {

    @JsonProperty("total")
    private int total;

    @JsonProperty("pendingReview")
    private int pendingReview;

    @JsonProperty("approved")
    private int approved;

    @JsonProperty("edited")
    private int edited;

    @JsonProperty("rejected")
    private int rejected;

    @JsonProperty("isComplete")
    private boolean isComplete;

    @JsonProperty("message")
    private String message;

    public ReviewSummaryResponse() {
    }

    public ReviewSummaryResponse(int total, int pendingReview, int approved, int edited, int rejected,
                                 boolean isComplete, String message) {
        this.total = total;
        this.pendingReview = pendingReview;
        this.approved = approved;
        this.edited = edited;
        this.rejected = rejected;
        this.isComplete = isComplete;
        this.message = message;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPendingReview() {
        return pendingReview;
    }

    public void setPendingReview(int pendingReview) {
        this.pendingReview = pendingReview;
    }

    public int getApproved() {
        return approved;
    }

    public void setApproved(int approved) {
        this.approved = approved;
    }

    public int getEdited() {
        return edited;
    }

    public void setEdited(int edited) {
        this.edited = edited;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
