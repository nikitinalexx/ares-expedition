package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.payments.Payment;

import java.util.List;

/**
 * One exact way to pay for a project: optional Restructured / Anaerobic discounts, then MC and (for Helion) heat that
 * sum to the discounted price.
 *
 * @param id       packed as {@code (heat << 2) | (anaerobic ? 2 : 0) | (restructured ? 1 : 0)}
 * @param payments list the engine accepts for this card and input
 */
public record BuildPaymentChoice(int id, List<Payment> payments) {
}
