package com.hansana.helpdesk.dashboard.dto;

import java.util.UUID;

public class CategoryDistributionItem {

    private UUID categoryId;
    private String categoryName;
    private long ticketCount;

    public CategoryDistributionItem() {
    }

    public CategoryDistributionItem(UUID categoryId, String categoryName, long ticketCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.ticketCount = ticketCount;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(long ticketCount) {
        this.ticketCount = ticketCount;
    }
}
