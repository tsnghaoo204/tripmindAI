package com.tripmind.enums;

/** Chế độ ăn của nhóm đi. Lưu trong {@code trips.group_profile} (JSONB) nên không có CHECK. */
public enum DietaryRestriction {
    VEGETARIAN,
    VEGAN,
    HALAL,
    NO_SEAFOOD,
    NO_PORK
}
