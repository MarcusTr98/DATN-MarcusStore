package com.fpoly.marcusstore.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Marcus thêm: bật cache nhẹ trong RAM cho cấu hình public của một instance
 * DATN.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
