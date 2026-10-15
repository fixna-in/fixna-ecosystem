package in.fixna.platform.localboost.billing;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import in.fixna.platform.billing.PlanCode;
import in.fixna.platform.billing.Subscription;
import in.fixna.platform.billing.SubscriptionService;
import in.fixna.platform.billing.WebhookEvent;
import in.fixna.platform.billing.WebhookEventRepository;
import in.fixna.platform.common.web.FixnaException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Razorpay webhook handler for subscription events.
 * Idempotency is ensured by tracking processed event IDs.
 */
@RestController
@RequestMapping("/api/v1/billing/webhooks")
@Tag(name = "billing-webhooks", description = "Razorpay webhook endpoints")
public class RazorpayWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayPaymentProvider razorpayProvider;
    private final SubscriptionService subscriptionService;
    private final WebhookEventRepository webhookEvents;

    public RazorpayWebhookController(
            RazorpayPaymentProvider razorpayProvider,
            SubscriptionService subscriptionService,
            WebhookEventRepository webhookEvents) {
        this.razorpayProvider = razorpayProvider;
        this.subscriptionService = subscriptionService;
        this.webhookEvents = webhookEvents;
    }

    @Operation(summary = "Handle Razorpay webhook events")
    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        // Verify webhook signature
        if (!razorpayProvider.verifyWebhookSignature(payload, signature)) {
            LOG.warn("Invalid Razorpay webhook signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Parse event ID from payload for idempotency
        String eventId = extractEventId(payload);
        if (eventId == null) {
            LOG.warn("Could not extract event ID from Razorpay webhook payload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Idempotency check
        if (webhookEvents.existsByEventId(eventId)) {
            LOG.info("Duplicate webhook event ignored: {}", eventId);
            return ResponseEntity.ok().build();
        }

        // Record event before processing (to ensure idempotency)
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setEventId(eventId);
        webhookEvent.setEventType(extractEventType(payload));
        webhookEvent.setPayload(payload);
        webhookEvent.setProcessedAt(OffsetDateTime.now());
        webhookEvents.save(webhookEvent);

        // Process event
        try {
            processEvent(payload);
        } catch (Exception e) {
            LOG.error("Failed to process webhook event {}: {}", eventId, e.getMessage(), e);
            // Mark as failed but don't retry automatically - manual intervention needed
            webhookEvent.setError(e.getMessage());
            webhookEvents.save(webhookEvent);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok().build();
    }

    private void processEvent(String payload) {
        // Parse event type from payload
        String eventType = extractEventType(payload);
        LOG.info("Processing Razorpay event: {}", eventType);

        switch (eventType) {
            case "subscription.activated",
                 "subscription.charged" -> handleSubscriptionActivatedOrCharged(payload);
            case "subscription.completed" -> handleSubscriptionCompleted(payload);
            case "subscription.cancelled" -> handleSubscriptionCancelled(payload);
            case "subscription.paused" -> handleSubscriptionPaused(payload);
            case "subscription.resumed" -> handleSubscriptionResumed(payload);
            case "subscription.halted" -> handleSubscriptionHalted(payload);
            case "payment.captured" -> handlePaymentCaptured(payload);
            case "payment.failed" -> handlePaymentFailed(payload);
            default -> LOG.info("Unhandled Razorpay event type: {}", eventType);
        }
    }

    private void handleSubscriptionActivatedOrCharged(String payload) {
        try {
            // Parse the subscription entity from payload
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject subscriptionObj = payloadObj.getJSONObject("subscription");
            JSONObject subscriptionEntity = subscriptionObj.getJSONObject("entity");
            
            String subscriptionId = subscriptionEntity.getString("id");
            String planId = subscriptionEntity.getString("plan_id");
            String status = subscriptionEntity.getString("status");
            
            // Get tenant_id from customer notes or metadata
            String tenantIdStr = null;
            if (subscriptionEntity.has("customer") && !subscriptionEntity.isNull("customer")) {
                JSONObject customer = subscriptionEntity.getJSONObject("customer");
                if (customer.has("notes") && !customer.isNull("notes")) {
                    JSONObject notes = customer.getJSONObject("notes");
                    if (notes.has("tenant_id")) {
                        tenantIdStr = notes.getString("tenant_id");
                    }
                }
            }
            
            if (tenantIdStr == null) {
                LOG.warn("Subscription {} missing tenant_id in customer notes", subscriptionId);
                return;
            }

            UUID tenantId = UUID.fromString(tenantIdStr);
            PlanCode plan = mapPlanIdToPlan(planId);
            String mappedStatus = mapRazorpayStatus(status);

            subscriptionService.updatePlan(tenantId, plan);
            Optional<Subscription> localSub = subscriptionService.getSubscription(tenantId);
            localSub.ifPresent(sub -> {
                sub.setStatus(mappedStatus);
                subscriptionService.save(sub);
            });

            LOG.info("Updated subscription for tenant {}: plan={}, status={}", tenantId, plan, mappedStatus);
        } catch (Exception e) {
            LOG.error("Error handling subscription event: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleSubscriptionCompleted(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject subscriptionObj = payloadObj.getJSONObject("subscription");
            JSONObject subscriptionEntity = subscriptionObj.getJSONObject("entity");
            
            String tenantIdStr = extractTenantIdFromSubscription(subscriptionEntity);
            if (tenantIdStr == null) return;

            UUID tenantId = UUID.fromString(tenantIdStr);
            Optional<Subscription> localSub = subscriptionService.getSubscription(tenantId);
            localSub.ifPresent(sub -> {
                sub.setStatus("COMPLETED");
                sub.setPlanCode(PlanCode.FREE.name());
                subscriptionService.save(sub);
            });

            LOG.info("Subscription completed for tenant {}, reverted to FREE", tenantId);
        } catch (Exception e) {
            LOG.error("Error handling subscription completed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleSubscriptionCancelled(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject subscriptionObj = payloadObj.getJSONObject("subscription");
            JSONObject subscriptionEntity = subscriptionObj.getJSONObject("entity");
            
            String tenantIdStr = extractTenantIdFromSubscription(subscriptionEntity);
            if (tenantIdStr == null) return;

            UUID tenantId = UUID.fromString(tenantIdStr);
            Optional<Subscription> localSub = subscriptionService.getSubscription(tenantId);
            localSub.ifPresent(sub -> {
                sub.setStatus("CANCELED");
                sub.setPlanCode(PlanCode.FREE.name());
                subscriptionService.save(sub);
            });

            LOG.info("Subscription cancelled for tenant {}, reverted to FREE", tenantId);
        } catch (Exception e) {
            LOG.error("Error handling subscription cancelled: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleSubscriptionPaused(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject subscriptionObj = payloadObj.getJSONObject("subscription");
            JSONObject subscriptionEntity = subscriptionObj.getJSONObject("entity");
            
            String tenantIdStr = extractTenantIdFromSubscription(subscriptionEntity);
            if (tenantIdStr == null) return;

            UUID tenantId = UUID.fromString(tenantIdStr);
            Optional<Subscription> localSub = subscriptionService.getSubscription(tenantId);
            localSub.ifPresent(sub -> {
                sub.setStatus("PAUSED");
                subscriptionService.save(sub);
            });

            LOG.info("Subscription paused for tenant {}", tenantId);
        } catch (Exception e) {
            LOG.error("Error handling subscription paused: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleSubscriptionResumed(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject subscriptionObj = payloadObj.getJSONObject("subscription");
            JSONObject subscriptionEntity = subscriptionObj.getJSONObject("entity");
            
            String tenantIdStr = extractTenantIdFromSubscription(subscriptionEntity);
            if (tenantIdStr == null) return;

            UUID tenantId = UUID.fromString(tenantIdStr);
            Optional<Subscription> localSub = subscriptionService.getSubscription(tenantId);
            localSub.ifPresent(sub -> {
                sub.setStatus("ACTIVE");
                subscriptionService.save(sub);
            });

            LOG.info("Subscription resumed for tenant {}", tenantId);
        } catch (Exception e) {
            LOG.error("Error handling subscription resumed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handleSubscriptionHalted(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject subscriptionObj = payloadObj.getJSONObject("subscription");
            JSONObject subscriptionEntity = subscriptionObj.getJSONObject("entity");
            
            String tenantIdStr = extractTenantIdFromSubscription(subscriptionEntity);
            if (tenantIdStr == null) return;

            UUID tenantId = UUID.fromString(tenantIdStr);
            Optional<Subscription> localSub = subscriptionService.getSubscription(tenantId);
            localSub.ifPresent(sub -> {
                sub.setStatus("HALTED");
                sub.setPlanCode(PlanCode.FREE.name());
                subscriptionService.save(sub);
            });

            LOG.info("Subscription halted for tenant {}, reverted to FREE", tenantId);
        } catch (Exception e) {
            LOG.error("Error handling subscription halted: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void handlePaymentCaptured(String payload) {
        LOG.info("Payment captured: {}", extractPaymentId(payload));
        // Could trigger receipt email, etc.
    }

    private void handlePaymentFailed(String payload) {
        LOG.warn("Payment failed: {}", extractPaymentId(payload));
        // Implement dunning: notify user, retry schedule, eventual cancellation
    }

    private String extractEventId(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            return event.getString("id");
        } catch (Exception e) {
            return null;
        }
    }

    private String extractEventType(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            return event.getString("event");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractTenantIdFromSubscription(JSONObject subscription) {
        if (subscription.has("customer") && !subscription.isNull("customer")) {
            JSONObject customer = subscription.getJSONObject("customer");
            if (customer.has("notes") && !customer.isNull("notes")) {
                JSONObject notes = customer.getJSONObject("notes");
                if (notes.has("tenant_id")) {
                    return notes.getString("tenant_id");
                }
            }
        }
        return null;
    }

    private String extractPaymentId(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            JSONObject payloadObj = event.getJSONObject("payload");
            JSONObject paymentObj = payloadObj.getJSONObject("payment");
            JSONObject paymentEntity = paymentObj.getJSONObject("entity");
            return paymentEntity.getString("id");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private PlanCode mapPlanIdToPlan(String planId) {
        if (planId.equals(razorpayProvider.getPlanFree())) return PlanCode.FREE;
        if (planId.equals(razorpayProvider.getPlanStarter())) return PlanCode.STARTER;
        if (planId.equals(razorpayProvider.getPlanGrowth())) return PlanCode.GROWTH;
        return PlanCode.FREE;
    }

    private String mapRazorpayStatus(String razorpayStatus) {
        return switch (razorpayStatus) {
            case "active" -> "ACTIVE";
            case "authenticated" -> "ACTIVE";
            case "pending" -> "PENDING";
            case "halted" -> "HALTED";
            case "cancelled" -> "CANCELED";
            case "completed" -> "COMPLETED";
            case "expired" -> "EXPIRED";
            default -> razorpayStatus.toUpperCase();
        };
    }
}