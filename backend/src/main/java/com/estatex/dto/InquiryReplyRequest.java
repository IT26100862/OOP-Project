package com.estatex.dto;

import jakarta.validation.constraints.NotBlank;

public class InquiryReplyRequest {

    @NotBlank(message = "Reply message is required")
    private String reply;

    private String agentUsername;

    public InquiryReplyRequest() {
    }

    public InquiryReplyRequest(String reply) {
        this.reply = reply;
    }

    public InquiryReplyRequest(String reply, String agentUsername) {
        this.reply = reply;
        this.agentUsername = agentUsername;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getAgentUsername() {
        return agentUsername;
    }

    public void setAgentUsername(String agentUsername) {
        this.agentUsername = agentUsername;
    }
}
