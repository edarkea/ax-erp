package com.odc.sales.accounting.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import com.odc.accounting.service.AccountingSetupResolution;
import com.odc.sales.db.SalesInvoice;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SalesInvoicePostingMapperImpl implements SalesInvoicePostingMapper {
  private final SalesInvoicePostingValidator validator;

  @Inject
  public SalesInvoicePostingMapperImpl(SalesInvoicePostingValidator validator) {
    this.validator = validator;
  }

  @Override
  public SalesInvoicePostingPlan map(SalesInvoice invoice) {
    SalesInvoicePostingContext context = validator.validateAndResolve(invoice);
    JournalEntry entry = header(invoice, context);
    List<JournalLine> lines = new ArrayList<>();
    lines.add(line(entry, 10, context.receivable(), invoice.getCustomer(), invoice.getDueDate(),
        invoice.getGrandTotal(), BigDecimal.ZERO, "Cuenta por cobrar - " + invoice.getDocumentNo()));
    lines.add(line(entry, 20, context.revenue(), null, null, BigDecimal.ZERO,
        invoice.getSubtotal(), "Ingreso por venta - " + invoice.getDocumentNo()));
    if (invoice.getTaxTotal().signum() > 0) {
      lines.add(line(entry, 30, context.outputTax(), null, null, BigDecimal.ZERO,
          invoice.getTaxTotal(), "Impuesto generado - " + invoice.getDocumentNo()));
    }
    BigDecimal debit = lines.stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal credit = lines.stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (debit.compareTo(credit) != 0 || debit.compareTo(invoice.getGrandTotal()) != 0)
      throw new IllegalArgumentException(I18n.get("Sales invoice posting plan is not balanced."));
    return new SalesInvoicePostingPlan(invoice, context, entry, List.copyOf(lines),
        context.receivable(), context.revenue(), context.outputTax(), debit, credit, lines.size());
  }

  private JournalEntry header(SalesInvoice invoice, SalesInvoicePostingContext context) {
    JournalEntry entry = new JournalEntry();
    entry.setCompany(invoice.getCompany()); entry.setBranch(invoice.getBranch());
    entry.setAccountingPeriod(context.period()); entry.setAccountingDate(invoice.getInvoiceDate());
    entry.setDocumentDate(invoice.getInvoiceDate()); entry.setCurrency(invoice.getCurrency());
    entry.setExchangeRate(invoice.getExchangeRate()); entry.setParty(invoice.getCustomer());
    entry.setEntryType("SALES"); entry.setStatus("DRAFT"); entry.setReference(invoice.getDocumentNo());
    entry.setDescription("Factura de venta " + invoice.getDocumentNo());
    entry.setSourceModule(SalesAccountingConstants.SOURCE_MODULE);
    entry.setSourceModel(SalesAccountingConstants.SALES_INVOICE_SOURCE_MODEL);
    entry.setSourceRecordId(invoice.getId()); entry.setSourceDocumentNo(invoice.getDocumentNo());
    entry.setArchived(false);
    return entry;
  }

  private JournalLine line(
      JournalEntry entry, int sequence, AccountingSetupResolution resolution,
      com.odc.party.db.Party party, java.time.LocalDate dueDate,
      BigDecimal debit, BigDecimal credit, String description) {
    JournalLine line = new JournalLine();
    line.setJournalEntry(entry); line.setSequence(sequence); line.setAccount(resolution.account());
    line.setAccountingRoleDefinition(resolution.roleDefinition());
    line.setAccountingSetupEntry(resolution.setupEntry()); line.setParty(party);
    line.setDueDate(dueDate); line.setDebit(debit); line.setCredit(credit);
    line.setReference(entry.getReference()); line.setDescription(description); line.setArchived(false);
    return line;
  }
}
