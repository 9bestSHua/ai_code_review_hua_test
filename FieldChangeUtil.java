// Copyright (c) 1998-2019 Core Solutions Limited. All rights reserved.
// ============================================================================
// CURRENT VERSION CBX 9.2.0 GA
// ============================================================================
// CHANGE LOG
// ============================================================================

package com.cbxsoftware.rest.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.ReflectionUtils;

import com.cbxsoftware.rest.common.proxy.CustomTableProxy;
import com.cbxsoftware.rest.entity.common.CustomTable;
import com.cbxsoftware.rest.entity.common.EmbedCodelist;
import com.cbxsoftware.rest.entity.common.InnerEntity;
import com.cbxsoftware.rest.entity.common.MainEntity;
import com.cbxsoftware.rest.entity.trigger.TriggerListenerCriteria;
import com.cbxsoftware.rest.enums.CustomFieldType;
import com.cbxsoftware.rest.util.SpringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author johnson.chen
 *
 */
@Slf4j
public final class FieldChangeUtil {

    public static final String ANY = "$ANY";
    public static final String EMPTY = "$EMPTY";
    private static final String UNSUPPORTED_CONVERSION_ERROR = "Cannot convert %1$s to an object";
    private static final Pattern REGX_CUSTOM_FIELD_TYPE
    = Pattern.compile("^(cust)(Text|Date|Number|Decimal|Codelist|Dropdown|Hcl|MemoText|Checkbox|Selection)[0-9]+");

    private FieldChangeUtil() {
    }

    /**
     *
     * @param sourceId The source property name from the entity
     * @param beforeEntity The before changes value
     * @param afterEntity The after changes value
     * @param targetBeforeValue The condition for matching the before value
     * @param targetAfterValue The condition for matching the after value
     * @return True if the values represent a change event, false otherwise.
     */
    public static boolean isFieldChangeEvent(
            final String sourceId,
            final MainEntity beforeEntity, final MainEntity afterEntity,
            final String targetBeforeValue, final String targetAfterValue
            ) {

        // Remove by tim chan to fix the issue raised in CNT-8938
        /*if (beforeEntity == null || afterEntity == null) {
            LOGGER.debug(String.format("Return false as [beforeEntity == null: %1$s] or [afterEntity == null: %2$s]",
                    beforeEntity == null, afterEntity == null));
            return false;
        }*/

        if (StringUtils.isBlank(sourceId)) {
            log.info(String.format("Define error: sourceId should not be null"));
            return false;
        }

        final Object value1 = isCustomFieldId(sourceId) ? getCustomFieldValue(sourceId, beforeEntity) : getFieldValue(beforeEntity, sourceId);
        final Object value2 = isCustomFieldId(sourceId) ? getCustomFieldValue(sourceId, afterEntity) : getFieldValue(afterEntity, sourceId);

        log.debug(String.format("Running sourceId = %1$s, value 1 = %2$s, value2 = %3$s, targetBeforeValue = %4$s, "
                + "targetAfterValue = %5$s",
                sourceId, value1, value2, targetBeforeValue, targetAfterValue));

        return isFieldChangeEvent(value1, value2, targetBeforeValue, targetAfterValue);
    }
    public static boolean isChildLevelFieldChangeEvent(
        final String sourceId,
        final MainEntity beforeEntity, final MainEntity afterEntity,
        final String targetBeforeValue, final String targetAfterValue, String childLevel,  Map<String, String> extraCommandProps
    ) {
        boolean isChildFieldChanged = Boolean.FALSE;
        if (StringUtils.isBlank(sourceId)) {
            log.info(String.format("Define error: sourceId should not be null"));
            return false;
        }

        Map<String, Object> beforeMap = new HashMap<>();
        Map<String, Object> afterMap = new HashMap<>();
        Object beforeCollectionObj = null;
        Object afterCollectionObj = null;
        StringBuilder stringBuilder = new StringBuilder();
        String[] levelEntityName = StringUtils.split(childLevel, ".");
        if (levelEntityName.length >= 2) {
            beforeCollectionObj = getFieldValue(beforeEntity, levelEntityName[1] + "List");
            afterCollectionObj = getFieldValue(afterEntity, levelEntityName[1] + "List");
        }

        if (beforeCollectionObj instanceof Collection beforeCollection && afterCollectionObj instanceof Collection afterCollection) {
            for (Object beforeObj : beforeCollection) {
                if (beforeObj instanceof InnerEntity beforeInner) {
                    final Object beforeValue = isCustomFieldId(sourceId)
                        ? getChildCustomFieldValue(sourceId, beforeInner)
                        : getFieldValue(beforeObj, sourceId);
                    final Object duid = getFieldValue(beforeInner, "duid");
                    if (duid == null) {
                        log.debug(
                            "duid is empty, skip check child field change for sourceId: {}, childLevel: {}", sourceId, childLevel);
                        continue;
                    }
                    beforeMap.put(duid.toString(), beforeValue);
                }
            }
            for (Object afterObj : afterCollection) {
                if (afterObj instanceof InnerEntity afterInner) {
                    final Object object = isCustomFieldId(sourceId)
                        ? getChildCustomFieldValue(sourceId, afterInner)
                        : getFieldValue(afterObj, sourceId);
                    final Object duid = getFieldValue(afterInner, "duid");
                    if (duid == null) {
                        log.debug(
                            "duid is empty, skip check child field change for sourceId: {}, childLevel: {}", sourceId, childLevel);
                        continue;
                    }
                    afterMap.put(duid.toString(), object);
                }
            }
            for (Map.Entry<String, Object> entry : beforeMap.entrySet()) {
                Object value1 = beforeMap.get(entry.getKey());
                Object value2 = afterMap.get(entry.getKey());
                log.debug(
                    "Running sourceId = {}, childLevel = {}, value 1 = {}, value2 = {}, targetBeforeValue = {}, targetAfterValue = {}",
                    sourceId, childLevel, value1, value2, targetBeforeValue, targetAfterValue);
                if (isFieldChangeEvent(value1, value2, targetBeforeValue, targetAfterValue)) {
                    stringBuilder.append(entry.getKey() + ",");
                    isChildFieldChanged = Boolean.TRUE;
                }
            }
            if (isChildFieldChanged){
                extraCommandProps.put("childEntityName", levelEntityName[1]);
                extraCommandProps.put("duid", stringBuilder.toString());
            }
        } else {
            log.debug("childLevel:{} is not a collection field, skip check child field change", childLevel);
        }
        return isChildFieldChanged;
    }

    /**
     *
     * @param childPropertyName The source group property name
     * @param sourceGroupFieldId The source group field id
     * @param beforeEntity The before changes value entity
     * @param afterEntity The after changes value entity
     * @param listenerCriterias The criteria entity from TriggerListenerCriteria
     * @return True if the values represent a change event, false otherwise.
     */
    public static boolean isFieldChangeEvent(final TriggerListenerCriteria listenerCriterias,
            final MainEntity beforeEntity, final MainEntity afterEntity) {
        final String sourceId = listenerCriterias.getSourceId();
        final String targetBeforeValue = listenerCriterias.getFromValue();
        final String targetAfterValue = listenerCriterias.getToValue();

        if (StringUtils.isNotBlank(sourceId)) {
            return isFieldChangeEvent(sourceId, beforeEntity, afterEntity, targetBeforeValue, targetAfterValue);
        }

        if (beforeEntity == null || afterEntity == null) {
            log.debug(String.format("return false as [beforeEntity == null: %1$s] or [afterEntity == null: %2$s]",
                    beforeEntity == null, afterEntity == null));
            return false;
        }

        return false;
    }
    public static boolean isChildLevelFieldChangeEvent(final TriggerListenerCriteria listenerCriterias,
                                                       final MainEntity beforeEntity, final MainEntity afterEntity, final String childLevel, final Map<String, String> extraCommandProps) {
        final String sourceId = listenerCriterias.getSourceId();
        final String targetBeforeValue = listenerCriterias.getFromValue();
        final String targetAfterValue = listenerCriterias.getToValue();

        if (StringUtils.isNotBlank(sourceId)) {
            return isChildLevelFieldChangeEvent(sourceId, beforeEntity, afterEntity, targetBeforeValue, targetAfterValue, childLevel, extraCommandProps);
        }

        if (beforeEntity == null || afterEntity == null) {
            log.debug(String.format("return false as [beforeEntity == null: %1$s] or [afterEntity == null: %2$s]",
                beforeEntity == null, afterEntity == null));
            return false;
        }

        return false;
    }
    /**
     *
     * @param beforeValue The before changes value
     * @param afterValue The after changes value
     * @param targetBeforeValue The condition for matching the before value
     * @param targetAfterValue The condition for matching the after value
     * @return True if the values represent a change event, false otherwise.
     */
    public static boolean isFieldChangeEvent(
            final Object beforeValue, final Object afterValue,
            final String targetBeforeValue, final String targetAfterValue) {
        if (ObjectUtils.equals(beforeValue, afterValue)) {
            return false;
        }

        final boolean resultBefore = isValueMatched(beforeValue, targetBeforeValue);
        final boolean resultAfter = isValueMatched(afterValue, targetAfterValue);

        return resultBefore && resultAfter;
    }

    /**
     * @param value
     * @param targetValue
     * @return
     */
    private static boolean isValueMatched(Object value, String targetValue) {
        if (value == null) {
            value = StringUtils.EMPTY;
        }
        if (ANY.equals(targetValue)) {
            return true;
        }
        if (StringUtils.equals(EMPTY, targetValue)) {
            targetValue = StringUtils.EMPTY;
        }

        if (value instanceof EmbedCodelist) {
            return ObjectUtils.equals(((EmbedCodelist) value).getCode(), targetValue);
        }

        if (value instanceof LocalDateTime || value instanceof LocalDate) {
            return ObjectUtils.equals(value.toString(), targetValue);
        }

        return ObjectUtils.equals(value, castToTargetValue(value, targetValue));
    };

    /**
     *
     * @param fromValue The value for checking the type
     * @param toValue The string value for target value to be convert into fromValue type
     * @return The object that was cast from the toValue
     */
    private static Object castToTargetValue(final Object fromValue, final String toValue) {

        if (toValue == null) {
            return null;
        }

        if (fromValue == null) {
            return toValue;
        }

        if (fromValue instanceof String) {
            return toValue;
        }

        try {
            if (fromValue instanceof Long) {
                return new Long(toValue);
            }

            if (fromValue instanceof BigDecimal) {
                return new BigDecimal(toValue);
            }

            if (fromValue instanceof Boolean) {
                return new Boolean(toValue);
            }

            return null;

        } catch (final Exception e) {
            log.error(String.format(UNSUPPORTED_CONVERSION_ERROR, fromValue.getClass().getSimpleName()), e);
            return null;
        }
    }

    public static <T> T getFieldValue(final Object obj, final String fieldName) {
        if (StringUtils.isBlank(fieldName) || obj == null) {
            return null;
        }
        try {
            return (T) SpringUtils.getFieldValueByReflection(obj, fieldName);
        } catch (final Throwable e) {
            return null;
        }
    }
    private static Object getChildCustomFieldValue(final String fieldId, final InnerEntity doc) {
        if (doc != null) {
            final Method getCustomFields = ReflectionUtils.findMethod(doc.getClass(), "getCustomFields");
            final CustomTable customTable = (CustomTable) ReflectionUtils.invokeMethod(getCustomFields, doc);
            final Optional<CustomFieldType> fieldTypeOpt = CustomFieldType.findTypeByColumnName(fieldId);
            if (fieldTypeOpt.isPresent()) {
                return CustomTableProxy.invokeGetter(fieldTypeOpt.get(), customTable, fieldId.replaceAll("[a-zA-Z]*", ""));
            }
        }
        return null;
    }
	private static Object getCustomFieldValue(final String fieldId, final MainEntity doc) {
		if (doc != null) {
			final Method getCustomFields = ReflectionUtils.findMethod(doc.getClass(), "getCustomFields");
	        final CustomTable customTable = (CustomTable) ReflectionUtils.invokeMethod(getCustomFields, doc);
	        final Optional<CustomFieldType> fieldTypeOpt = CustomFieldType.findTypeByColumnName(fieldId);
	        if (fieldTypeOpt.isPresent()) {
	            return CustomTableProxy.invokeGetter(fieldTypeOpt.get(), customTable, fieldId.replaceAll("[a-zA-Z]*", ""));
	        }
		}
        return null;
	}

    public static boolean isCustomFieldId(final String fieldId) {
        return REGX_CUSTOM_FIELD_TYPE.matcher(fieldId).matches();
    }
}
