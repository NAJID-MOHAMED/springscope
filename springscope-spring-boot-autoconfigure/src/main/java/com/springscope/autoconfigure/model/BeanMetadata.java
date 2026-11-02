package com.springscope.autoconfigure.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BeanMetadata(
        String name,
        String type,
        String scope,
        boolean singleton,
        boolean prototype,
        Boolean aopProxy,
        boolean lazyInit,
        String role,
        String factoryBeanName,
        String factoryMethodName,
        Boolean factoryBean
) {
}
