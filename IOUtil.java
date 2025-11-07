// Copyright (c) 1998-2021 Core Solutions Limited. All rights reserved.
// ============================================================================
// CURRENT VERSION CNT.5.0.1
// ============================================================================
// CHANGE LOG
// CNT.5.0.1 : 2021-XX-XX, luson.lu, creation
// ============================================================================
package com.cbxsoftware.rest.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author luson.lu
 */
public final class IOUtil {

    private IOUtil() {
    }

    public static byte[] zip(final Map<String, byte[]> paramMap) throws IOException {
        try (ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream localZipOutputStream = new ZipOutputStream(localByteArrayOutputStream);) {
            final Iterator<?> iterator = paramMap.entrySet().iterator();
            while (iterator.hasNext()) {
                @SuppressWarnings("rawtypes") final Map.Entry localEntry = (Map.Entry) iterator.next();
                final byte[] arrayOfByte1 = (byte[]) localEntry.getValue();
                localZipOutputStream.setMethod(8);
                final byte[] arrayOfByte2 = new byte[2048];
                try (ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(arrayOfByte1);) {
                    localZipOutputStream.putNextEntry(new ZipEntry((String) localEntry.getKey()));
                    int i = 0;
                    while ((i = localByteArrayInputStream.read(arrayOfByte2, 0, 2048)) != -1) {
                        localZipOutputStream.write(arrayOfByte2, 0, i);
                    }
                }
                localZipOutputStream.closeEntry();
            }
            localZipOutputStream.finish();
            return localByteArrayOutputStream.toByteArray();
        }
    }

    public static List<Map<String, byte[]>> unzip(final byte[] files) throws IOException {
        if (files == null) {
            return null;
        }
        final List<Map<String, byte[]>> fileList = new ArrayList<>();
        ZipEntry ze;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(files);
             ZipInputStream zis = new ZipInputStream(bis);) {
            while ((ze = zis.getNextEntry()) != null) {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
                    final byte[] file = new byte[2048];
                    int count = 0;
                    while ((count = zis.read(file, 0, 2048)) != -1) {
                        bos.write(file, 0, count);
                    }
                    Map<String, byte[]> fileMap = new HashMap<>();
                    fileMap.put(ze.getName(), bos.toByteArray());
                    fileList.add(fileMap);
                    zis.closeEntry();
                }
            }
        }
        return fileList;
    }
}
