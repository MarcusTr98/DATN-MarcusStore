package com.fpoly.marcusstore.utils;

public final class BannerPositionRules {

    private BannerPositionRules() {}

    public static boolean allowsOrder(String positionCode) {
        return "HOME_SLIDER".equals(positionCode);
    }

    public static int maxSlots(String positionCode) {
        return allowsOrder(positionCode) ? 5 : 1;
    }
}