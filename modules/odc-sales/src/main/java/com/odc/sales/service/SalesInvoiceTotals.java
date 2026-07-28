package com.odc.sales.service;
import java.math.BigDecimal;
public record SalesInvoiceTotals(BigDecimal subtotal, BigDecimal taxTotal, BigDecimal grandTotal, int lineCount) {}
