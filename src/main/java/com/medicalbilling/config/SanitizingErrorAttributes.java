package com.medicalbilling.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Component
public class SanitizingErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
        attributes.remove("trace");
        attributes.remove("exception");
        attributes.remove("message");
        attributes.remove("errors");
        attributes.remove("bindingErrors");
        attributes.put("path", sanitizePath(attributes.get("path")));
        return attributes;
    }

    private String sanitizePath(Object pathValue) {
        if (!(pathValue instanceof String path) || path.isBlank()) {
            return "/";
        }
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        return path;
    }
}
