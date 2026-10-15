package in.fixna.platform.localboost.billing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import in.fixna.platform.billing.PaymentProvider;
import in.fixna.platform.billing.PlanCode;
import in.fixna.platform.common.web.FixnaException;

/**
 * Razorpay payment provider for subscription billing.
 * All amounts in minor units (paise) as required by Razorpay.
 */
@Component
public class RazorpayPaymentProvider implements PaymentProvider {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String planFree;
    private final String planStarter;
    private final String planGrowth;
    private RazorpayClient razorpayClient;

    public RazorpayPaymentProvider(
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            @Value("${razorpay.webhook-secret:}") String webhookSecret,
            @Value("${razorpay.plan.free:}") String planFree,
            @Value("${razorpay.plan.starter:}") String planStarter,
            @Value("${razorpay.plan.growth:}") String planGrowth) throws RazorpayException {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.planFree = planFree;
        this.planStarter = planStarter;
        this.planGrowth = planGrowth;
        if (keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank()) {
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
        }
    }

    @Override
    public String providerId() {
        return "razorpay";
    }

    @Override
    public void validatePaymentAmount(BigDecimal amount, BigDecimal remainingBalance) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FixnaException(
                    "INVALID_PAYMENT_AMOUNT",
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero");
        }
    }

    /**
     * Creates a Razorpay customer for the tenant.
     */
    public String createCustomer(UUID tenantId, String email, String name, String contact) {
        if (razorpayClient == null) {
            throw new FixnaException(
                    "PAYMENT_PROVIDER_NOT_CONFIGURED",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay is not configured");
        }
        try {
            JSONObject params = new JSONObject();
            params.put("email", email);
            params.put("name", name);
            params.put("contact", contact);
            JSONObject notes = new JSONObject();
            notes.put("tenant_id", tenantId.toString());
            params.put("notes", notes);
            com.razorpay.Customer customer = razorpayClient.customers.create(params);
            return customer.get("id");
        } catch (RazorpayException e) {
            throw new FixnaException(
                    "RAZORPAY_CUSTOMER_CREATE_FAILED",
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create Razorpay customer: " + e.getMessage());
        }
    }

    /**
     * Creates a subscription for the tenant.
     */
    public String createSubscription(String customerId, PlanCode plan) {
        if (razorpayClient == null) {
            throw new FixnaException(
                    "PAYMENT_PROVIDER_NOT_CONFIGURED",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay is not configured");
        }
        String planId = getPlanId(plan);
        if (planId == null || planId.isBlank()) {
            throw new FixnaException(
                    "PLAN_NOT_CONFIGURED",
                    HttpStatus.BAD_REQUEST,
                    "Plan ID not configured for plan: " + plan);
        }
        try {
            JSONObject params = new JSONObject();
            params.put("plan_id", planId);
            params.put("customer_id", customerId);
            params.put("total_count", 0); // Infinite billing cycles
            params.put("quantity", 1);
            JSONObject notes = new JSONObject();
            notes.put("tenant_id", customerId);
            params.put("notes", notes);
            com.razorpay.Subscription subscription = razorpayClient.subscriptions.create(params);
            return subscription.get("id");
        } catch (RazorpayException e) {
            throw new FixnaException(
                    "RAZORPAY_SUBSCRIPTION_CREATE_FAILED",
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create Razorpay subscription: " + e.getMessage());
        }
    }

    /**
     * Creates an order for one-time payment (if needed).
     */
    public String createOrder(BigDecimal amount, String currency, String receipt, Map<String, String> notes) {
        if (razorpayClient == null) {
            throw new FixnaException(
                    "PAYMENT_PROVIDER_NOT_CONFIGURED",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay is not configured");
        }
        try {
            // Convert to paise (minor units)
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
            JSONObject params = new JSONObject();
            params.put("amount", amountInPaise);
            params.put("currency", currency);
            params.put("receipt", receipt);
            if (notes != null && !notes.isEmpty()) {
                JSONObject notesJson = new JSONObject(notes);
                params.put("notes", notesJson);
            }
            Order order = razorpayClient.orders.create(params);
            return order.get("id");
        } catch (RazorpayException e) {
            throw new FixnaException(
                    "RAZORPAY_ORDER_CREATE_FAILED",
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create Razorpay order: " + e.getMessage());
        }
    }

    /**
     * Verifies a webhook signature.
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false; // Not configured - reject in production
        }
        try {
            com.razorpay.Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    private String getPlanId(PlanCode plan) {
        return switch (plan) {
            case FREE -> planFree;
            case STARTER -> planStarter;
            case GROWTH -> planGrowth;
        };
    }

    public String getPlanFree() {
        return planFree;
    }

    public String getPlanStarter() {
        return planStarter;
    }

    public String getPlanGrowth() {
        return planGrowth;
    }
}