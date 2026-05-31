package com.bookstore.utils;

import com.bookstore.config.OSSProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OssUrlBuilder {

    private final OSSProperties props;

    public String toFullUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        String prefix = props.getPublicPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return key;
        }
        boolean prefixSlash = prefix.endsWith("/");
        boolean keySlash = key.startsWith("/");
        if (prefixSlash && keySlash) {
            return prefix + key.substring(1);
        }
        if (!prefixSlash && !keySlash) {
            return prefix + "/" + key;
        }
        return prefix + key;
    }
}
