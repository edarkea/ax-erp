package com.odc.sales.service;
import com.google.inject.Inject;import com.odc.pricing.service.PriceResolverService;import com.odc.sales.db.*;
public class SalesInvoicePricingServiceImpl implements SalesInvoicePricingService{
 private final PriceResolverService resolver;private final SalesInvoiceLineService lines;
 @Inject public SalesInvoicePricingServiceImpl(PriceResolverService r,SalesInvoiceLineService l){resolver=r;lines=l;}
 public SalesInvoiceLine refreshPrice(SalesInvoiceLine l){lines.validate(l);if("MANUAL".equals(l.getPriceSource()))return l;var i=l.getSalesInvoice();var p=resolver.resolve(i.getCompany(),l.getItem(),i.getCurrency(),i.getInvoiceDate(),l.getQuantity(),i.getPriceList());l.setPriceListItem(p.priceListItem());l.setUnitPrice(p.price());l.setPriceSource("PRICE_LIST");return lines.save(l);}
 public void refreshPrices(SalesInvoice i){for(var l:lines.findActiveLines(i))if(!"MANUAL".equals(l.getPriceSource()))refreshPrice(l);}
}
