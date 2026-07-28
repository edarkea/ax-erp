package com.odc.sales.accounting.web;

import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.accounting.db.JournalEntry;
import com.odc.sales.db.SalesInvoice;
import com.odc.sales.db.repo.SalesInvoiceRepository;
import com.odc.sales.accounting.service.SalesInvoiceAccountingService;
import java.time.LocalDate;

public class SalesInvoiceAccountingController {
  public void loadStatus(ActionRequest request, ActionResponse response) {
    SalesInvoice context = request.getContext().asType(SalesInvoice.class);
    if (context == null || context.getId() == null) {
      response.setValue("$accountingStatus", "NOT_POSTED");
      response.setValue("$accountingEntryNumber", null);
      return;
    }
    SalesInvoice invoice = Beans.get(SalesInvoiceRepository.class).find(context.getId());
    var posting = Beans.get(SalesInvoiceAccountingService.class).findPosting(invoice);
    String status = posting.map(JournalEntry::getStatus).orElse("NOT_POSTED");
    response.setValue("$accountingStatus", status);
    response.setValue("$accountingEntryNumber",
        posting.map(JournalEntry::getEntryNumber).orElse(null));
  }

  public void post(ActionRequest request, ActionResponse response) {
    var result = Beans.get(SalesInvoiceAccountingService.class).postInvoice(invoice(request));
    response.setInfo(result.alreadyPosted()
        ? "La factura ya estaba contabilizada." : "La factura fue contabilizada correctamente.");
    open(result.journalEntry(), response);
  }

  public void openPosting(ActionRequest request, ActionResponse response) {
    open(Beans.get(SalesInvoiceAccountingService.class).requirePosting(invoice(request)), response);
  }

  public void reverse(ActionRequest request, ActionResponse response) {
    LocalDate date = (LocalDate) request.getContext().get("$reversalDate");
    String reason = (String) request.getContext().get("$reversalReason");
    Beans.get(SalesInvoiceAccountingService.class)
        .reverseInvoicePosting(invoice(request), date, reason);
    response.setInfo("La contabilización fue reversada correctamente.");
    response.setReload(true);
  }

  private SalesInvoice invoice(ActionRequest request) {
    SalesInvoice context = request.getContext().asType(SalesInvoice.class);
    if (context == null || context.getId() == null)
      throw new IllegalArgumentException("La factura debe estar guardada.");
    return Beans.get(SalesInvoiceRepository.class).find(context.getId());
  }

  private void open(JournalEntry entry, ActionResponse response) {
    response.setView(ActionView.define("Journal entry")
        .model(JournalEntry.class.getName()).add("form", "journal-entry-form")
        .context("_showRecord", entry.getId()).map());
  }
}
