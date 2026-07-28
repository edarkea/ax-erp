package com.odc.sales.observer;
import static com.odc.common.rpc.RequestEntityUtils.process;
import com.axelor.event.Observes;import com.axelor.events.*;import com.axelor.events.qualifiers.EntityType;import com.odc.sales.db.*;import com.odc.sales.service.*;import jakarta.inject.Inject;import jakarta.inject.Named;
public class SalesSaveObserver{
 private final SalesInvoiceService invoices;private final SalesInvoiceLineService lines;
 @Inject public SalesSaveObserver(SalesInvoiceService i,SalesInvoiceLineService l){invoices=i;lines=l;}
 public void invoice(@Observes @Named(RequestEvent.SAVE) @EntityType(SalesInvoice.class) PreRequest e){process(e,SalesInvoice.class,invoices::validateDraft);}
 public void line(@Observes @Named(RequestEvent.SAVE) @EntityType(SalesInvoiceLine.class) PreRequest e){process(e,SalesInvoiceLine.class,lines::validate);}
}
