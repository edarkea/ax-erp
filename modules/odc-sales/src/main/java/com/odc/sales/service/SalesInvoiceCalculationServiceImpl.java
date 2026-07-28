package com.odc.sales.service;
import com.google.inject.Inject;
import com.odc.sales.db.*;
import com.odc.tax.service.TaxCalculationService;
import java.math.*;
import java.util.List;
public class SalesInvoiceCalculationServiceImpl implements SalesInvoiceCalculationService {
  private final SalesInvoiceLineService lineService;
  private final TaxCalculationService taxCalculationService;
  @Inject public SalesInvoiceCalculationServiceImpl(SalesInvoiceLineService lines, TaxCalculationService taxes) {
    lineService=lines; taxCalculationService=taxes;
  }
  public SalesInvoiceLineTotals calculateLine(SalesInvoiceLine line) {
    int scale=scale(line.getSalesInvoice()); BigDecimal quantity=nz(line.getQuantity()), price=nz(line.getUnitPrice());
    BigDecimal subtotal=quantity.multiply(price).setScale(scale,RoundingMode.HALF_UP);
    BigDecimal rate=nz(line.getTaxRateSnapshot());
    BigDecimal tax=taxCalculationService.calculateTax(subtotal,rate,scale,RoundingMode.HALF_UP);
    return new SalesInvoiceLineTotals(subtotal,subtotal,tax,subtotal.add(tax).setScale(scale,RoundingMode.HALF_UP));
  }
  public SalesInvoiceTotals calculate(SalesInvoice invoice) {
    List<SalesInvoiceLine> lines=lineService.findActiveLines(invoice);
    BigDecimal sub=BigDecimal.ZERO,tax=BigDecimal.ZERO,total=BigDecimal.ZERO;
    for(SalesInvoiceLine line:lines){var t=calculateLine(line);sub=sub.add(t.lineSubtotal());tax=tax.add(t.taxAmount());total=total.add(t.lineTotal());}
    int s=scale(invoice); return new SalesInvoiceTotals(sub.setScale(s,RoundingMode.HALF_UP),tax.setScale(s,RoundingMode.HALF_UP),total.setScale(s,RoundingMode.HALF_UP),lines.size());
  }
  public SalesInvoiceTotals recalculate(SalesInvoice invoice) {
    for(SalesInvoiceLine line:lineService.findActiveLines(invoice)){var t=calculateLine(line);line.setLineSubtotal(t.lineSubtotal());line.setTaxableBase(t.taxableBase());line.setTaxAmount(t.taxAmount());line.setLineTotal(t.lineTotal());lineService.persistCalculated(line);}
    var t=calculate(invoice);invoice.setSubtotal(t.subtotal());invoice.setTaxTotal(t.taxTotal());invoice.setGrandTotal(t.grandTotal());return t;
  }
  private int scale(SalesInvoice i){return i.getCurrency()==null||i.getCurrency().getDecimalPlaces()==null?2:i.getCurrency().getDecimalPlaces();}
  private BigDecimal nz(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
}
