package com.cbxsoftware.rest.util;

import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

public final class ListUtil {

    private ListUtil() {
    }

    public static boolean isEmptyOrOnlyContainsNullElements(final List<Object> list) {
        if (CollectionUtils.isEmpty(list)) {
            return true;
        }
        final boolean containsAnyNonNullElements = list.stream().anyMatch(Objects::nonNull);
        return !containsAnyNonNullElements;
    }
}
