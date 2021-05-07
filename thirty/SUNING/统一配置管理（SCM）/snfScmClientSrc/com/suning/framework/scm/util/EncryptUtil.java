package com.suning.framework.scm.util;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class EncryptUtil {
    public static final String ENCODER_UTF8 = "UTF-8";
    public static final String ENCODER_ISO8859 = "ISO-8859-1";
    public static final String KEY_MAC = "HmacMD5";

    public static String encryptHMAC(String data, String key) throws Exception {
        String keyStr = URLDecoder.decode(key, "UTF-8");
        byte[] keyRaw = keyStr.getBytes("ISO-8859-1");
        SecretKey secretKey = new SecretKeySpec(keyRaw, "HmacMD5");
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        byte[] raw = mac.doFinal(data.getBytes("UTF-8"));
        return URLEncoder.encode(new String(raw, "ISO-8859-1"), "UTF-8");
    }
}
