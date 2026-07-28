package com.odc.sales.module;
import com.axelor.app.AxelorModule;import com.odc.sales.service.*;import com.odc.sales.observer.SalesSaveObserver;
public class OdcSalesModule extends AxelorModule{protected void configure(){
 bind(SalesInvoiceService.class).to(SalesInvoiceServiceImpl.class);bind(SalesInvoiceLineService.class).to(SalesInvoiceLineServiceImpl.class);
 bind(SalesInvoiceCalculationService.class).to(SalesInvoiceCalculationServiceImpl.class);bind(SalesInvoicePricingService.class).to(SalesInvoicePricingServiceImpl.class);
 bind(SalesInvoiceTaxService.class).to(SalesInvoiceTaxServiceImpl.class);bind(SalesInvoiceConfirmationService.class).to(SalesInvoiceConfirmationServiceImpl.class);
 bind(SalesInvoiceCancellationService.class).to(SalesInvoiceCancellationServiceImpl.class);
 bind(SalesSaveObserver.class);
}}
