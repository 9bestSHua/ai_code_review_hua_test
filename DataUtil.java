package com.cbxsoftware.rest.util;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DataUtil {

    public static Integer bigDec2Int(final Object obj, @Nullable final Integer defaultValue) {
        if (obj == null) return defaultValue;
        try {
            final BigDecimal bigDec = (BigDecimal) obj;
            return bigDec.intValueExact();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static LocalDateTime timestamp2LocalDateTime(final Object obj) {
        if (obj == null) return null;
        try {
            Timestamp ts = (Timestamp) obj;
            return ts.toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}
