package com.tripmind.services.ai.tools;

import com.tripmind.domains.models.GroupProfile;

/** Mở hàm nội bộ của {@link PlaceTools} cho test ở package khác. */
public final class PlaceToolsAccess {

    private PlaceToolsAccess() {
    }

    public static String withDietaryKeywords(String query, GroupProfile group) {
        return PlaceTools.withDietaryKeywords(query, group);
    }
}
