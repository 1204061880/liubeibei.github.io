package com.suning.framework.scm.util;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class CompressUtils {
    private static final byte[] COMPRESS_PREFIX = {0, 0, 0};

    public static byte[] serialize(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        try {
            byte[] compressedData = compress(str.getBytes("UTF-8"));
            byte[] result = Arrays.copyOf(COMPRESS_PREFIX, COMPRESS_PREFIX.length + compressedData.length);
            System.arraycopy(compressedData, 0, result, COMPRESS_PREFIX.length, compressedData.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("serialize error", e);
        }
    }

    public static String deserialize(byte[] data) {
        try {
            if ((data == null) || (data.length == 0)) {
                return null;
            }
            if (data.length < COMPRESS_PREFIX.length) {
                return new String(data, "UTF-8");
            }
            byte[] prefix = Arrays.copyOfRange(data, 0, COMPRESS_PREFIX.length);
            byte[] originData = data;
            if (Arrays.equals(COMPRESS_PREFIX, prefix)) {
                originData = Arrays.copyOfRange(data, COMPRESS_PREFIX.length, data.length);
                if (originData.length == 0) {
                    return null;
                }
                originData = uncompress(originData);
            }
            return new String(originData, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[] compress(byte[] inputByte) throws Exception {
        Deflater deflater = new Deflater();
        deflater.setInput(inputByte);
        deflater.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] outputByte = new byte[1024];
        try {
            while (!deflater.finished()) {
                int len = deflater.deflate(outputByte);
                bos.write(outputByte, 0, len);
            }
        } finally {
            deflater.end();
            bos.close();
        }
        return bos.toByteArray();
    }

    private static byte[] uncompress(byte[] inputByte) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(inputByte);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] outByte = new byte[1024];
        try {
            while (!inflater.finished()) {
                int len = inflater.inflate(outByte);
                if (len == 0) {
                    break;
                }
                bos.write(outByte, 0, len);
            }
        } finally {
            inflater.end();
            bos.close();
        }
        return bos.toByteArray();
    }
}
