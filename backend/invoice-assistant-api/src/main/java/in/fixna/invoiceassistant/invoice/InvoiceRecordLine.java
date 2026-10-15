package in.fixna.invoiceassistant.invoice;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One line item on a standalone invoice record (V2). */
@Entity
@Table(name = "invoice_record_lines")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceRecordLine {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "invoice_record_id", nullable = false)
    private UUID invoiceRecordId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
