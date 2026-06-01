package com.fpoly.marcusstore.utils;

import java.util.ArrayList;
import java.util.List;

public class SkuGeneratorUtils {

    // Thuật toán đệ quy Cartesian Product
    public static <T> List<List<T>> generateCombinations(List<List<T>> lists) {
        List<List<T>> result = new ArrayList<>();
        if (lists == null || lists.isEmpty()) {
            result.add(new ArrayList<>());
            return result;
        }

        List<T> firstList = lists.get(0);
        List<List<T>> remainingLists = generateCombinations(lists.subList(1, lists.size()));

        for (T condition : firstList) {
            for (List<T> remainingList : remainingLists) {
                List<T> combination = new ArrayList<>();
                combination.add(condition);
                combination.addAll(remainingList);
                result.add(combination);
            }
        }
        return result;
    }

    public static String buildSkuCode(String productCode, List<String> optionCodes) {
        StringBuilder skuBuilder = new StringBuilder(productCode);
        for (String option : optionCodes) {
            skuBuilder.append("-").append(option.toUpperCase());
        }
        return skuBuilder.toString();
    }
}