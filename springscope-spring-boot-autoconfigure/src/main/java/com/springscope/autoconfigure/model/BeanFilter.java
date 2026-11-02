package com.springscope.autoconfigure.model;

public record BeanFilter(
        String name,
        String type,
        String scope,
        Boolean lazy
) {

    public static BeanFilter unconstrained() {
        return new BeanFilter(null, null, null, null);
    }

    public boolean isUnconstrained() {
        return name == null && type == null && scope == null && lazy == null;
    }

    public boolean matches(BeanMetadata bean) {
        if (name != null && !containsIgnoreCase(bean.name(), name)) {
            return false;
        }
        if (type != null && !matchesType(bean.type(), type)) {
            return false;
        }
        if (scope != null && !equalsIgnoreCase(bean.scope(), scope)) {
            return false;
        }
        return lazy == null || bean.lazyInit() == lazy;
    }

    private static boolean matchesType(String actualType, String expectedType) {
        if (actualType == null) {
            return false;
        }
        if (actualType.equalsIgnoreCase(expectedType) || actualType.contains(expectedType)) {
            return true;
        }
        int lastDot = actualType.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? actualType.substring(lastDot + 1) : actualType;
        return simpleName.equalsIgnoreCase(expectedType);
    }

    private static boolean containsIgnoreCase(String value, String fragment) {
        return value != null && value.toLowerCase().contains(fragment.toLowerCase());
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}
