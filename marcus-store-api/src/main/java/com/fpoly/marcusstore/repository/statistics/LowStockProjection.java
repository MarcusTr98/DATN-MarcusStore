package com.fpoly.marcusstore.repository.statistics;

public interface LowStockProjection {
    String getSkuCode();
    String getProductName();
    String getBrand();
    Integer getStockQuantity();
}