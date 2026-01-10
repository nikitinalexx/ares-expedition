package com.terraforming.ares.services.ai.dto;

import com.terraforming.ares.model.payments.PaymentType;
import lombok.Data;

@Data
public class PaidDiscount {
    public static final PaidDiscount ANAEROBIC_DISCOUNT = new PaidDiscount(10, PaymentType.ANAEROBIC_MICROORGANISMS);
    public static final PaidDiscount RESTRUCTURED_DISCOUNT = new PaidDiscount(5, PaymentType.RESTRUCTURED_RESOURCES);

    private final int value;
    private final PaymentType applyPayment;

    PaidDiscount(int value, PaymentType applyPayment) {
        this.value = value;
        this.applyPayment = applyPayment;
    }
}
