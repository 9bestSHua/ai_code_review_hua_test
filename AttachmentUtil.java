// Copyright (c) 1998-2022 Core Solutions Limited. All rights reserved.
// ============================================================================
// CURRENT VERSION CBX 14.12 GA
// ============================================================================
// CHANGE LOG
// CBX 14.12 GA : 2024-05-30, arkle.liu, KIK-1036
// CBX 12.15 GA : 2022-07-29, nicholas.liu, LIDL-11328
// ...
// ============================================================================
package com.cbxsoftware.rest.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.cbxsoftware.rest.dto.common.AttachmentDto;
import com.cbxsoftware.rest.dto.common.CustomTableDto;
import com.cbxsoftware.rest.dto.common.EmbedCodelistDto;
import com.cbxsoftware.rest.entity.common.EmbedCodelist;
import com.cbxsoftware.rest.entity.common.EmbedHcl;
import com.cbxsoftware.rest.entity.custFieldDef.CustFieldDefItem;
import com.cbxsoftware.rest.enums.FieldDataType;
import com.cbxsoftware.rest.enums.FileProtocol;
import com.cbxsoftware.rest.service.file.FileStorageService;
import com.cbxsoftware.rest.service.file.thumbnail.ThumbnailGenerationService;

@Component
@Lazy(false)
public class AttachmentUtil {

    private static final String THUMBNAIL_STR = "thumbnail/";
    private static final Pattern FILE_PATH_INFO_PATTERN =
        Pattern.compile("^(S3|AZURE_BLOB):(thumbnail/)?[^/]+/[^/]+/[^/]+$");
    //DFM-544
    private static final Pattern NEW_FILE_PATH_INFO_PATTERN =
        Pattern.compile("^(S3|AZURE_BLOB):(thumbnail/)?[^/]+/[^/]+/[^/]+/[^/]+$");
    private static FileStorageService fileStorageService;

    public AttachmentUtil(final FileStorageService fileStorageService) {// KIK-1034
        AttachmentUtil.fileStorageService = fileStorageService;
    }

    /**
     * Construct protocol & filePath info which will be saved to ES index
     */
    public static String getFilePathInfo(final FileProtocol protocol, final String filePath) {
        return protocol + ":" + filePath;
    }

    /**
     * Retrieve access url by the protocol & filePath info
     */
    public static Optional<String> getFilePathInfoUrl(final String filePathInfo) {
        if (StringUtils.isNotBlank(filePathInfo) &&
            (NEW_FILE_PATH_INFO_PATTERN.matcher(filePathInfo).find() || FILE_PATH_INFO_PATTERN.matcher(filePathInfo).find())) {
            final int separator = filePathInfo.indexOf(':');
            final FileProtocol protocol = FileProtocol.valueOf(filePathInfo.substring(0, separator));
            final String filePath = filePathInfo.substring(separator + 1);
            return getFilePathUrl(protocol, filePath);
        }
        return Optional.empty();
    }

    /**
     * Retrieve access url by protocol and filePath
     */
    public static Optional<String> getFilePathUrl(final FileProtocol protocol, final String filePath) {
        return Optional.ofNullable(fileStorageService)
            .flatMap(service -> service.getAccessUrl(protocol, filePath));
    }

    /**
     * Convert protocol string to {@link FileProtocol}
     */
    public static FileProtocol toFileProtocol(final String protocolStr) {
        if (StringUtils.equalsIgnoreCase("S3", protocolStr)) {
            return FileProtocol.S3;
        } else if (StringUtils.equalsIgnoreCase("AzureBlob", protocolStr)) {
            return FileProtocol.AZURE_BLOB;
        } else if (StringUtils.equalsIgnoreCase("AI-S3", protocolStr)) {
            return FileProtocol.AI_S3;
        }
        return FileProtocol.S3;
    }

    public static boolean isCodelistUpdated(final EmbedCodelistDto sourceCodelist, final EmbedCodelistDto targetCodelist) {
        if (ObjectUtils.isEmpty(sourceCodelist) && ObjectUtils.isEmpty(targetCodelist)) {
            return false;
        }
        if (ObjectUtils.isNotEmpty(sourceCodelist) && ObjectUtils.isNotEmpty(targetCodelist)) {
            return !StringUtils.equals(sourceCodelist.getCode(), targetCodelist.getCode());
        }
        return true;
    }

    public static boolean isCodelistsUpdated(final List<EmbedCodelistDto> sourceCodelists,
                                      final List<EmbedCodelistDto> targetCodelists) {
        if (CollectionUtils.isEmpty(sourceCodelists) && CollectionUtils.isEmpty(targetCodelists)) {
            return false;
        }
        if (CollectionUtils.isEmpty(sourceCodelists) && CollectionUtils.isNotEmpty(targetCodelists)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(sourceCodelists) && CollectionUtils.isEmpty(targetCodelists)) {
            return true;
        }
        if (CollectionUtils.size(sourceCodelists) != CollectionUtils.size(targetCodelists)) {
            return true;
        }
        for (final EmbedCodelistDto source : sourceCodelists) {
            final Optional<EmbedCodelistDto> targetOpt = CommonUtil.safeStream(targetCodelists)
                .filter(tc -> org.apache.commons.lang.StringUtils.equals(tc.getCode(), source.getCode())).findFirst();
            if (!targetOpt.isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAttachmentUpdated(final AttachmentDto sourceAttachment, final AttachmentDto targetAttachment) {
        if (sourceAttachment == null && targetAttachment == null) {
            return false;
        }
        if (sourceAttachment == null && targetAttachment != null) {
            return true;
        }
        if (sourceAttachment != null && targetAttachment == null) {
            return true;
        }
        if (sourceAttachment != null) {
            if (!org.apache.commons.lang.StringUtils.equals(sourceAttachment.getId(), targetAttachment.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCustomFieldUpdated(final CustomTableDto sourceCustomFields,
    		final CustomTableDto targetCustomFields, final List<CustFieldDefItem> custFieldDefItems) {
    	if (ObjectUtils.isEmpty(custFieldDefItems)) {
    		return false;
    	}
        if (sourceCustomFields == null && targetCustomFields == null) {
            return false;
        }
        final Map<String, Object> sourceDynamicModelMap = sourceCustomFields != null ? sourceCustomFields.getDynamicModelMap() : null;
        final Map<String, Object> targetDynamicModelMap = targetCustomFields != null ? targetCustomFields.getDynamicModelMap() : null;
        if (sourceDynamicModelMap == null && targetDynamicModelMap == null) {
            return false;
        }
        for (final CustFieldDefItem custFieldDefItem : custFieldDefItems) {
    		final String fieldType = custFieldDefItem.getFieldType();
    		final String fieldId = custFieldDefItem.getFieldId();
    		final Object sourceValue = sourceDynamicModelMap != null ? sourceDynamicModelMap.get(fieldId) : null;
    		final Object targetValue = targetDynamicModelMap != null ? targetDynamicModelMap.get(fieldId) : null;
			if (sourceValue == null && targetValue == null) {
	            return false;
	        }
	        if (sourceValue == null && targetValue != null) {
	            return true;
	        }
	        if (sourceValue != null && targetValue == null) {
	            return true;
	        }
    		if (StringUtils.equalsIgnoreCase(fieldType, FieldDataType.CODELIST.getValue())) {
    			if (ObjectUtils.notEqual(((EmbedCodelist) sourceValue).getCode(), ((EmbedCodelist) targetValue).getCode())) {
	        		return true;
	        	}
    		} else if (StringUtils.equalsIgnoreCase(fieldType, "HclGroup")) {
    			if (ObjectUtils.notEqual(((EmbedHcl) sourceValue).getHclNodeFullCode(), ((EmbedHcl) targetValue).getHclNodeFullCode())) {
	        		return true;
	        	}
    		} else if (StringUtils.equalsIgnoreCase(fieldType, FieldDataType.DECIMAL.getValue())) {
    			final BigDecimal sourceDecimalValue= ((BigDecimal)sourceValue).setScale(5, RoundingMode.HALF_UP);
    			final BigDecimal targetDecimalValue= ((BigDecimal)targetValue).setScale(5, RoundingMode.HALF_UP);
    			if (ObjectUtils.notEqual(sourceDecimalValue, targetDecimalValue)) {
        			return true;
        		}
    		} else if (ObjectUtils.notEqual(sourceValue, targetValue)) {
    			return true;
    		}
    	}
        return false;
    }

}
