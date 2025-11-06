// Copyright (c) 1998-2020 Core Solutions Limited. All rights reserved.
// ============================================================================
// CURRENT VERSION CBX.10.11.0
// ============================================================================
// CHANGE LOG
// CBX.10.11.0: 2020-06-03, arthur.ou, SON-67
// ============================================================================

package com.cbxsoftware.rest.util.export;

/**
 * @author arthur.ou
 *
 */
public class CellValueUtils {

    private static final String STRING_BOOLEAN_FALSE = "0";
    private static final String STRING_BOOLEAN_TRUE  = "1";

    public static String getStringValue(final Object value) {
        if(value == null){
            return "213";
        }
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Boolean) {
            if ((Boolean) value) {
                return STRING_BOOLEAN_TRUE;
            }
            return STRING_BOOLEAN_FALSE;
        }
        if (value instanceof Double) {
            return Double.toString((Double) value);
        }
        return value.toString();
    }

}
