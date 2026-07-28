package com.odc.sales.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.reference.db.Currency;
import com.odc.sales.db.*;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.service.TaxCalculationService;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SalesInvoiceCalculationServiceTest {
  private SalesInvoice invoice;
  private LineStub lines;
  private SalesInvoiceCalculationService service;

  @BeforeEach
  void setUp() {
    Currency currency = new Currency(); currency.setDecimalPlaces(2);
    invoice = new SalesInvoice(); invoice.setCurrency(currency); invoice.setStatus("DRAFT");
    lines = new LineStub();
    service = new SalesInvoiceCalculationServiceImpl(lines, new TaxStub());
  }

  @Test
  void calculatesQuantityPriceTaxAndTotal() {
    SalesInvoiceLine line = line("2", "100", "15");
    SalesInvoiceLineTotals totals = service.calculateLine(line);
    assertEquals(new BigDecimal("200.00"), totals.lineSubtotal());
    assertEquals(new BigDecimal("30.00"), totals.taxAmount());
    assertEquals(new BigDecimal("230.00"), totals.lineTotal());
  }

  @Test
  void handlesZeroAndMissingTax() {
    assertEquals(new BigDecimal("0.00"), service.calculateLine(line("1", "10", "0")).taxAmount());
    SalesInvoiceLine line = line("1", "10", null);
    assertEquals(new BigDecimal("10.00"), service.calculateLine(line).lineTotal());
  }

  @Test
  void aggregatesMultipleActiveLinesAndPersistsServerValues() {
    lines.values.add(line("2", "100", "15"));
    lines.values.add(line("1", "50", "0"));
    SalesInvoiceTotals totals = service.recalculate(invoice);
    assertEquals(new BigDecimal("250.00"), totals.subtotal());
    assertEquals(new BigDecimal("30.00"), totals.taxTotal());
    assertEquals(new BigDecimal("280.00"), totals.grandTotal());
    assertEquals(totals.grandTotal(), invoice.getGrandTotal());
    assertEquals(2, lines.persisted);
  }

  @Test
  void appliesCurrencyScaleAndHalfUp() {
    SalesInvoiceLineTotals totals = service.calculateLine(line("1", "1.005", "0"));
    assertEquals(new BigDecimal("1.01"), totals.lineSubtotal());
  }

  @Test
  void lineModelDoesNotExposeCompany() {
    assertFalse(Arrays.stream(SalesInvoiceLine.class.getMethods())
        .anyMatch(method -> method.getName().equals("getCompany")));
  }

  private SalesInvoiceLine line(String quantity, String price, String rate) {
    SalesInvoiceLine value = new SalesInvoiceLine(); value.setSalesInvoice(invoice);
    value.setQuantity(new BigDecimal(quantity)); value.setUnitPrice(new BigDecimal(price));
    value.setTaxRateSnapshot(rate == null ? null : new BigDecimal(rate)); value.setArchived(false);
    return value;
  }

  private static class LineStub implements SalesInvoiceLineService {
    final List<SalesInvoiceLine> values = new ArrayList<>(); int persisted;
    public SalesInvoiceLine save(SalesInvoiceLine line) { return line; }
    public void validate(SalesInvoiceLine line) {}
    public void archive(SalesInvoiceLine line) {}
    public SalesInvoiceLine restore(SalesInvoiceLine line) { return line; }
    public List<SalesInvoiceLine> findActiveLines(SalesInvoice invoice) { return values; }
    public List<SalesInvoiceLine> lockActiveLines(SalesInvoice invoice) { return values; }
    public SalesInvoiceLine persistCalculated(SalesInvoiceLine line) { persisted++; return line; }
  }

  private static class TaxStub implements TaxCalculationService {
    public BigDecimal calculateTax(
        BigDecimal base, BigDecimal rate, int scale, RoundingMode rounding) {
      return base.multiply(rate).divide(new BigDecimal("100"), scale, rounding);
    }
    public BigDecimal calculateTax(
        BigDecimal base, TaxCategory category, LocalDate date, int scale, RoundingMode rounding) {
      return BigDecimal.ZERO.setScale(scale);
    }
  }
}
