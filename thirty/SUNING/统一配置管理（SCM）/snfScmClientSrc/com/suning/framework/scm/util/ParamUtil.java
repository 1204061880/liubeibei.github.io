package com.suning.framework.scm.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.*;

public class ParamUtil {
    private static final Set<Class> BASEDATATYPE = new HashSet() {
    };

    public static String getParamFromMap(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("map is null!");
        }
        List<Map.Entry<String, Object>> mappingList = new ArrayList(map.entrySet());
        Collections.sort(mappingList, new Comparator<Map.Entry<String, Object>>() {
            @Override
            public int compare(Map.Entry<String, Object> entry1, Map.Entry<String, Object> entry2) {
                return entry1.getKey().compareTo(entry2.getKey());
            }
        });
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : mappingList) {
            Object obj = entry.getValue();
            if (BASEDATATYPE.contains(obj.getClass())) {
                builder.append((String) entry.getKey()).append("=").append(obj).append("&");
            } else if (obj.getClass().isArray()) {
                int arrLength = Array.getLength(obj);
                if (arrLength > 0) {
                    for (int i = 0; i < arrLength; i++) {
                        builder.append((String) entry.getKey()).append("=").append(Array.get(obj, i)).append("&");
                    }
                }
            } else if ((obj instanceof Collection)) {
                Collection collection = (Collection) obj;
                if (collection.size() > 0) {
                    for (Object o : collection) {
                        builder.append((String) entry.getKey()).append("=").append(o).append("&");
                    }
                }
            } else {
                throw new RuntimeException("Can't parse complex Object:" + obj.getClass());
            }
        }
        builder.deleteCharAt(builder.lastIndexOf("&"));

        return builder.toString();
    }

    public static String getEncodedParamFromMap(Map<String, Object> map)
            throws UnsupportedEncodingException {
        if (map == null) {
            throw new IllegalArgumentException("map is null!");
        }
        Map<String, Object> encodedMap = new HashMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object obj = entry.getValue();
            if ((obj instanceof String)) {
                encodedMap.put(entry.getKey(), URLEncoder.encode((String) obj, "UTF-8"));
            } else if (obj.getClass().isArray()) {
                int arrLength = Array.getLength(obj);
                Object[] arr = new Object[arrLength];
                for (int i = 0; i < arrLength; i++) {
                    if ((Array.get(obj, i) instanceof String)) {
                        arr[i] = URLEncoder.encode((String) Array.get(obj, i), "UTF-8");
                    } else {
                        arr[i] = Array.get(obj, i);
                    }
                }
                encodedMap.put(entry.getKey(), arr);
            } else if ((obj instanceof Collection)) {
                Collection collection = (Collection) obj;

                int arrLength = collection.size();
                Object[] arr = new Object[arrLength];
                int i = 0;
                for (Object o : collection) {
                    if ((o instanceof String)) {
                        arr[i] = URLEncoder.encode((String) o, "UTF-8");
                    } else {
                        arr[i] = o;
                    }
                    i++;
                }
                encodedMap.put(entry.getKey(), arr);
            } else {
                encodedMap.put(entry.getKey(), obj);
            }
        }
        return getParamFromMap(encodedMap);
    }
}
