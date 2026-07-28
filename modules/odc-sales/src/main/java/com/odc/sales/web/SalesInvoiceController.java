package com.odc.sales.web;

import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.catalog.db.repo.ItemRepository;
import com.odc.document.db.repo.DocumentSeriesRepository;
import com.odc.document.service.DocumentSequenceService;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.pricing.db.repo.PriceListRepository;
import com.odc.pricing.service.PriceResolverService;
import com.odc.sales.db.SalesInvoice;
import com.odc.sales.db.SalesInvoiceLine;
import com.odc.sales.db.repo.SalesInvoiceRepository;
import com.odc.sales.service.SalesInvoiceCalculationService;
import com.odc.sales.service.SalesInvoiceCancellationService;
import com.odc.sales.service.SalesInvoiceConfirmationService;
import com.odc.sales.service.SalesInvoiceService;
import com.odc.sales.service.SalesInvoiceTaxService;
import com.odc.sales.service.SalesInvoicePricingService;
import com.odc.tax.db.repo.TaxCategoryRepository;
import com.odc.tax.service.TaxRateService;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SalesInvoiceController {

  public void open(ActionRequest request, ActionResponse response) {
    var company = Beans.get(ActiveOrganizationService.class).requireActiveCompany();
    response.setView(
        ActionView.define("Sales invoices")
            .model(SalesInvoice.class.getName())
            .add("grid", "sales-invoice-grid")
            .add("form", "sales-invoice-form")
            .domain("self.company.id = " + company.getId())
            .map());
  }

  public void initialize(ActionRequest request, ActionResponse response) {
    response.setValue(
        "company", Beans.get(ActiveOrganizationService.class).requireActiveCompany());
    response.setValue("invoiceDate", LocalDate.now());
    response.setValue("status", "DRAFT");
    response.setValue("archived", false);
    response.setValue("exchangeRate", BigDecimal.ONE);
    response.setValue("subtotal", BigDecimal.ZERO);
    response.setValue("taxTotal", BigDecimal.ZERO);
    response.setValue("grandTotal", BigDecimal.ZERO);
  }

  public void initializeLine(ActionRequest request, ActionResponse response) {
    response.setValue("archived", false);
    response.setValue("quantity", BigDecimal.ONE);
    response.setValue("priceSource", "PRICE_LIST");
    response.setValue("unitPrice", BigDecimal.ZERO);
    response.setValue("taxRateSnapshot", BigDecimal.ZERO);
    response.setValue("lineSubtotal", BigDecimal.ZERO);
    response.setValue("taxableBase", BigDecimal.ZERO);
    response.setValue("taxAmount", BigDecimal.ZERO);
    response.setValue("lineTotal", BigDecimal.ZERO);
  }

  public void calculate(ActionRequest request, ActionResponse response) {
    var invoice = request.getContext().asType(SalesInvoice.class);
    Beans.get(SalesInvoiceCalculationService.class).recalculate(invoice);
    response.setReload(true);
  }

  public void calculateContext(ActionRequest request, ActionResponse response) {
    SalesInvoice invoice = request.getContext().asType(SalesInvoice.class);
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal taxTotal = BigDecimal.ZERO;
    BigDecimal grandTotal = BigDecimal.ZERO;
    if (invoice.getLines() != null) {
      var calculation = Beans.get(SalesInvoiceCalculationService.class);
      for (SalesInvoiceLine line : invoice.getLines()) {
        if (Boolean.TRUE.equals(line.getArchived())) continue;
        line.setSalesInvoice(invoice);
        var totals = calculation.calculateLine(line);
        subtotal = subtotal.add(totals.lineSubtotal());
        taxTotal = taxTotal.add(totals.taxAmount());
        grandTotal = grandTotal.add(totals.lineTotal());
      }
    }
    response.setValue("subtotal", subtotal);
    response.setValue("taxTotal", taxTotal);
    response.setValue("grandTotal", grandTotal);
  }

  public void prices(ActionRequest request, ActionResponse response) {
    Beans.get(SalesInvoicePricingService.class)
        .refreshPrices(request.getContext().asType(SalesInvoice.class));
    response.setReload(true);
  }

  public void taxes(ActionRequest request, ActionResponse response) {
    Beans.get(SalesInvoiceTaxService.class)
        .refreshTaxes(request.getContext().asType(SalesInvoice.class));
    response.setReload(true);
  }

  public void confirm(ActionRequest request, ActionResponse response) {
    Beans.get(SalesInvoiceConfirmationService.class)
        .confirm(request.getContext().asType(SalesInvoice.class));
    response.setReload(true);
  }

  public void cancel(ActionRequest request, ActionResponse response) {
    SalesInvoice invoice = request.getContext().asType(SalesInvoice.class);
    Beans.get(SalesInvoiceCancellationService.class).cancel(invoice, invoice.getCancelReason());
    response.setReload(true);
  }

  public void archive(ActionRequest request, ActionResponse response) {
    Beans.get(SalesInvoiceService.class).archive(request.getContext().asType(SalesInvoice.class));
    response.setReload(true);
  }

  public void restore(ActionRequest request, ActionResponse response) {
    Beans.get(SalesInvoiceService.class).restore(request.getContext().asType(SalesInvoice.class));
    response.setReload(true);
  }

  public void syncPriceListCurrency(ActionRequest request, ActionResponse response) {
    SalesInvoice invoice = request.getContext().asType(SalesInvoice.class);
    if (invoice.getPriceList() == null || invoice.getPriceList().getId() == null) {
      response.setValue("currency", null);
      return;
    }
    var priceList = Beans.get(PriceListRepository.class).find(invoice.getPriceList().getId());
    response.setValue("currency", priceList == null ? null : priceList.getCurrency());
  }

  public void previewDocumentNo(ActionRequest request, ActionResponse response) {
    SalesInvoice invoice = request.getContext().asType(SalesInvoice.class);
    if (invoice.getDocumentSeries() == null || invoice.getDocumentSeries().getId() == null) {
      response.setValue("documentNoPreview", null);
      return;
    }
    var series =
        Beans.get(DocumentSeriesRepository.class).find(invoice.getDocumentSeries().getId());
    if (series == null) {
      response.setValue("documentNoPreview", null);
      return;
    }
    long current = series.getCurrentSequence() == null ? 0L : series.getCurrentSequence();
    response.setValue(
        "documentNoPreview",
        Beans.get(DocumentSequenceService.class).formatDocumentNo(series, current + 1L));
  }

  public void syncLine(ActionRequest request, ActionResponse response) {
    updateLine(request, response, true, false);
  }

  public void changePriceSource(ActionRequest request, ActionResponse response) {
    updateLine(request, response, true, true);
  }

  public void calculateLine(ActionRequest request, ActionResponse response) {
    updateLine(request, response, false, false);
  }

  private void updateLine(
      ActionRequest request,
      ActionResponse response,
      boolean resolvePrice,
      boolean resetManualPrice) {
    SalesInvoiceLine line = request.getContext().asType(SalesInvoiceLine.class);
    boolean manual = "MANUAL".equals(line.getPriceSource());
    response.setAttr("unitPrice", "readonly", !manual);

    SalesInvoice invoice = resolveInvoice(request, line);
    var item =
        line.getItem() == null || line.getItem().getId() == null
            ? null
            : Beans.get(ItemRepository.class).find(line.getItem().getId());

    BigDecimal quantity =
        line.getQuantity() == null || line.getQuantity().signum() <= 0
            ? BigDecimal.ONE
            : line.getQuantity();
    BigDecimal unitPrice = line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice();

    if (manual && resetManualPrice) {
      unitPrice = BigDecimal.ZERO;
      response.setValue("unitPrice", unitPrice);
      response.setValue("priceListItem", null);
    } else if (!manual
        && resolvePrice
        && item != null
        && invoice != null
        && invoice.getPriceList() != null
        && invoice.getCurrency() != null
        && invoice.getInvoiceDate() != null) {
      var resolution =
          Beans.get(PriceResolverService.class)
              .resolve(
                  invoice.getCompany(),
                  item,
                  invoice.getCurrency(),
                  invoice.getInvoiceDate(),
                  quantity,
                  invoice.getPriceList());
      unitPrice = resolution.price();
      response.setValue("priceSource", "PRICE_LIST");
      response.setValue("priceListItem", resolution.priceListItem());
      response.setValue("unitPrice", unitPrice);
    }

    var category = line.getTaxCategory();
    if (category != null && category.getId() != null) {
      category = Beans.get(TaxCategoryRepository.class).find(category.getId());
    }
    if (item != null) {
      response.setValue("uom", item.getUom());
      if (category == null) {
        category = item.getTaxCategory();
        response.setValue("taxCategory", category);
      }
      if (line.getDescription() == null || line.getDescription().isBlank()) {
        response.setValue("description", item.getName());
      }
    }

    BigDecimal taxRate = BigDecimal.ZERO;
    if (category != null && invoice != null && invoice.getInvoiceDate() != null) {
      var rate =
          Beans.get(TaxRateService.class)
              .requireApplicableRate(category, invoice.getInvoiceDate());
      taxRate = rate.getRate();
      response.setValue("taxRate", rate);
    } else {
      response.setValue("taxRate", null);
    }
    response.setValue("taxRateSnapshot", taxRate);

    if (invoice != null) {
      line.setSalesInvoice(invoice);
      line.setQuantity(quantity);
      line.setUnitPrice(unitPrice);
      line.setTaxRateSnapshot(taxRate);
      var totals = Beans.get(SalesInvoiceCalculationService.class).calculateLine(line);
      response.setValue("lineSubtotal", totals.lineSubtotal());
      response.setValue("taxableBase", totals.taxableBase());
      response.setValue("taxAmount", totals.taxAmount());
      response.setValue("lineTotal", totals.lineTotal());
    }
  }

  private SalesInvoice resolveInvoice(ActionRequest request, SalesInvoiceLine line) {
    SalesInvoice invoice = line.getSalesInvoice();
    if (invoice == null && request.getContext().getParent() != null) {
      invoice = request.getContext().getParent().asType(SalesInvoice.class);
    }
    if (invoice == null || invoice.getId() == null) return invoice;
    if (invoice.getCompany() != null
        && invoice.getPriceList() != null
        && invoice.getCurrency() != null
        && invoice.getInvoiceDate() != null) {
      return invoice;
    }
    SalesInvoice persisted = Beans.get(SalesInvoiceRepository.class).find(invoice.getId());
    return persisted == null ? invoice : persisted;
  }
}
