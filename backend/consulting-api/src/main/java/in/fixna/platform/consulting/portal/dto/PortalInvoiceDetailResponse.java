package in.fixna.platform.consulting.portal.dto;

import java.util.List;

import in.fixna.platform.consulting.invoice.dto.InvoiceResponse;
import in.fixna.platform.consulting.invoice.dto.PaymentResponse;

public record PortalInvoiceDetailResponse(InvoiceResponse invoice, List<PaymentResponse> payments) {}
