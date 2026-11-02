package com.springscope.springscope.support;

import java.util.concurrent.atomic.AtomicBoolean;

public class LazyGreetingService {

    public static final AtomicBoolean INSTANTIATED = new AtomicBoolean(false);

    public LazyGreetingService() {
        INSTANTIATED.set(true);
    }

    public String greet() {
        return "hello";
    }
}
