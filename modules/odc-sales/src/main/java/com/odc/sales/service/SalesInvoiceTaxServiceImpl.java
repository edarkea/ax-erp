package com.odc.sales.service;
import com.google.inject.Inject;import com.odc.tax.service.TaxRateService;import com.odc.sales.db.*;import java.math.BigDecimal;import java.util.Objects;
public class SalesInvoiceTaxServiceImpl implements SalesInvoiceTaxService{
 private final TaxRateService rates;private final SalesInvoiceLineService lines;
 @Inject public SalesInvoiceTaxServiceImpl(TaxRateService r,SalesInvoiceLineService l){rates=r;lines=l;}
 public SalesInvoiceLine refreshTax(SalesInvoiceLine l){lines.validate(l);var c=l.getTaxCategory();if(c==null){l.setTaxRate(null);l.setTaxRateSnapshot(BigDecimal.ZERO);l.setTaxCategoryCodeSnapshot(null);}else{var company=l.getSalesInvoice().getCompany();if(!same(c.getCountry(),company.getCountry())||Boolean.TRUE.equals(c.getArchived()))throw new IllegalArgumentException("Invalid tax category.");var rate=rates.requireApplicableRate(c,l.getSalesInvoice().getInvoiceDate());l.setTaxRate(rate);l.setTaxRateSnapshot(rate.getRate());l.setTaxCategoryCodeSnapshot(c.getCode());}return lines.save(l);}
 public void refreshTaxes(SalesInvoice i){for(var l:lines.findActiveLines(i))refreshTax(l);}
 private boolean same(com.axelor.db.Model a,com.axelor.db.Model b){return a==b||a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId());}
}
