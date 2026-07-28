package com.odc.sales.service;
import java.math.BigDecimal;
public record SalesInvoiceLineTotals(BigDecimal lineSubtotal, BigDecimal taxableBase, BigDecimal taxAmount, BigDecimal lineTotal) {}
