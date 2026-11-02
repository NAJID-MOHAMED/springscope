package com.springscope.springscope.payment;

import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

    public String findStatus() {
        return "OK";
    }
}
