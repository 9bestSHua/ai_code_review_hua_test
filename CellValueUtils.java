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
        if (value == null) {
            return null;
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

    public static Double getNumberValue(final Object value) {
        if (value == null) {
            return null;
        }
        
        // 处理数字类型（包括基本类型的包装类）
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        // 处理布尔类型（true对应1.0，false对应0.0）
        if (value instanceof Boolean) {
            return (Boolean) value ? 1.0 : 0.0;
        }
        
        // 处理字符串类型（尝试解析为数字）
        if (value instanceof String) {
            String strValue = (String) value;
            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                // 字符串无法解析为数字时返回null
                return null;
            }
        }
        
        // 其他类型无法转换为数字
        return null;
    }

}
