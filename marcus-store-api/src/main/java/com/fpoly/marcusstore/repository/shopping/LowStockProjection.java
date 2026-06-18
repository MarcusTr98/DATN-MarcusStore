package com.fpoly.marcusstore.repository.shopping;

public interface LowStockProjection {
    String getSkuCode();
    String getProductName();
    String getBrand();
    Integer getStockQuantity();
}