package com.odc.sales.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.accounting.service.*;
import com.odc.organization.service.OrganizationAccessService;
import com.odc.sales.db.SalesInvoice;
import com.odc.sales.db.SalesInvoiceLine;
import com.odc.sales.service.SalesInvoiceCalculationService;
import com.odc.sales.service.SalesInvoiceLineService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;

public class SalesInvoicePostingValidatorImpl implements SalesInvoicePostingValidator {
  private final OrganizationAccessService access;
  private final AccountingPeriodService periods;
  private final AccountingRoleDefinitionService roles;
  private final AccountingSetupResolver setups;
  private final JournalEntryService entries;
  private final SalesInvoiceLineService lines;
  private final SalesInvoiceCalculationService calculation;

  @Inject
  public SalesInvoicePostingValidatorImpl(
      OrganizationAccessService access, AccountingPeriodService periods,
      AccountingRoleDefinitionService roles, AccountingSetupResolver setups,
      JournalEntryService entries, SalesInvoiceLineService lines,
      SalesInvoiceCalculationService calculation) {
    this.access = access; this.periods = periods; this.roles = roles; this.setups = setups;
    this.entries = entries; this.lines = lines; this.calculation = calculation;
  }

  @Override public void validateForPosting(SalesInvoice invoice) { validateAndResolve(invoice); }

  @Override public boolean isReadyForPosting(SalesInvoice invoice) {
    try { validateAndResolve(invoice); return true; }
    catch (IllegalArgumentException exception) { return false; }
  }

  @Override
  public SalesInvoicePostingContext validateAndResolve(SalesInvoice invoice) {
    requireHeader(invoice);
    var activeLines = lines.findActiveLines(invoice);
    if (activeLines.isEmpty()) throw error("Invoice must contain at least one active line.");
    var sequences = new HashSet<Integer>();
    BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO, total = BigDecimal.ZERO;
    for (SalesInvoiceLine line : activeLines) {
      requireLine(invoice, line, sequences);
      var values = calculation.calculateLine(line);
      if (values.lineSubtotal().compareTo(line.getLineSubtotal()) != 0
          || values.taxableBase().compareTo(line.getTaxableBase()) != 0
          || values.taxAmount().compareTo(line.getTaxAmount()) != 0
          || values.lineTotal().compareTo(line.getLineTotal()) != 0)
        throw error("Confirmed invoice line totals are inconsistent.");
      subtotal = subtotal.add(line.getLineSubtotal());
      tax = tax.add(line.getTaxAmount());
      total = total.add(line.getLineTotal());
    }
    if (subtotal.compareTo(invoice.getSubtotal()) != 0
        || tax.compareTo(invoice.getTaxTotal()) != 0
        || total.compareTo(invoice.getGrandTotal()) != 0
        || invoice.getSubtotal().add(invoice.getTaxTotal()).compareTo(invoice.getGrandTotal()) != 0)
      throw error("Confirmed invoice totals are inconsistent.");

    var existing = entries.findBySource(
        invoice.getCompany(), SalesAccountingConstants.SALES_INVOICE_SOURCE_MODEL, invoice.getId());
    if (existing.isPresent()) {
      var entry = existing.get();
      if (!Objects.equals(entry.getSourceDocumentNo(), invoice.getDocumentNo()))
        throw error("Journal entry source document number does not match the invoice.");
      if (!"POSTED".equals(entry.getStatus()))
        throw error("A non-posted journal entry already exists for this invoice.");
    }

    var period = periods.requireOpenPeriod(invoice.getCompany(), invoice.getInvoiceDate());
    var receivableRole = roles.requireByCode(SalesAccountingConstants.ACCOUNT_RECEIVABLE);
    var revenueRole = roles.requireByCode(SalesAccountingConstants.SALES_REVENUE);
    var receivable = setups.requireAccount(invoice.getCompany(), invoice.getBranch(),
        invoice.getCurrency(), SalesAccountingConstants.DOCUMENT_GROUP,
        SalesAccountingConstants.DOCUMENT_TYPE, receivableRole);
    var revenue = setups.requireAccount(invoice.getCompany(), invoice.getBranch(),
        invoice.getCurrency(), SalesAccountingConstants.DOCUMENT_GROUP,
        SalesAccountingConstants.DOCUMENT_TYPE, revenueRole);
    AccountingSetupResolution outputTax = null;
    if (invoice.getTaxTotal().signum() > 0) {
      var taxRole = roles.requireByCode(SalesAccountingConstants.OUTPUT_TAX);
      outputTax = setups.requireAccount(invoice.getCompany(), invoice.getBranch(),
          invoice.getCurrency(), SalesAccountingConstants.DOCUMENT_GROUP,
          SalesAccountingConstants.DOCUMENT_TYPE, taxRole);
    }
    return new SalesInvoicePostingContext(period, receivable, revenue, outputTax);
  }

  private void requireHeader(SalesInvoice invoice) {
    if (invoice == null || invoice.getId() == null) throw error("Persisted invoice is required.");
    if (Boolean.TRUE.equals(invoice.getArchived())) throw error("Invoice is archived.");
    if (!"CONFIRMED".equals(invoice.getStatus()))
      throw error("Invoice must be confirmed before posting.");
    if (invoice.getCompany() == null || Boolean.TRUE.equals(invoice.getCompany().getArchived())
        || !Boolean.TRUE.equals(invoice.getCompany().getActive())) throw error("Company must be active.");
    access.requireCompanyAccess(AuthUtils.getUser(), invoice.getCompany());
    if (invoice.getBranch() == null || !same(invoice.getBranch().getCompany(), invoice.getCompany())
        || Boolean.TRUE.equals(invoice.getBranch().getArchived())
        || !Boolean.TRUE.equals(invoice.getBranch().getActive())) throw error("Branch is invalid.");
    access.requireBranchAccess(AuthUtils.getUser(), invoice.getBranch());
    if (invoice.getCustomer() == null || !same(invoice.getCustomer().getCompany(), invoice.getCompany())
        || Boolean.TRUE.equals(invoice.getCustomer().getArchived())
        || !Boolean.TRUE.equals(invoice.getCustomer().getActive())) throw error("Customer is invalid.");
    if (invoice.getInvoiceDate() == null || invoice.getDueDate() == null
        || invoice.getDueDate().isBefore(invoice.getInvoiceDate())) throw error("Invoice dates are invalid.");
    if (invoice.getCurrency() == null || Boolean.TRUE.equals(invoice.getCurrency().getArchived()))
      throw error("Currency must be active.");
    if (invoice.getExchangeRate() == null || invoice.getExchangeRate().signum() <= 0)
      throw error("Exchange rate must be greater than zero.");
    if (blank(invoice.getDocumentNo())) throw error("Invoice has no document number.");
    if (invoice.getDocumentSequenceReservation() == null
        || !"CONSUMED".equals(invoice.getDocumentSequenceReservation().getStatus()))
      throw error("Invoice document reservation must be consumed.");
    if (invoice.getConfirmedAt() == null || invoice.getConfirmedBy() == null)
      throw error("Invoice confirmation data is incomplete.");
    if (invoice.getSubtotal() == null || invoice.getSubtotal().signum() < 0
        || invoice.getTaxTotal() == null || invoice.getTaxTotal().signum() < 0
        || invoice.getGrandTotal() == null || invoice.getGrandTotal().signum() <= 0)
      throw error("Invoice totals are invalid.");
  }

  private void requireLine(SalesInvoice invoice, SalesInvoiceLine line, HashSet<Integer> sequences) {
    if (line == null || !same(line.getSalesInvoice(), invoice) || Boolean.TRUE.equals(line.getArchived()))
      throw error("Invoice line is invalid.");
    if (line.getItem() == null || blank(line.getItemCodeSnapshot()) || blank(line.getItemNameSnapshot())
        || line.getQuantity() == null || line.getQuantity().signum() <= 0
        || line.getUnitPrice() == null || line.getUnitPrice().signum() < 0
        || line.getTaxRateSnapshot() == null || line.getTaxRateSnapshot().signum() < 0
        || line.getLineSubtotal() == null || line.getLineSubtotal().signum() < 0
        || line.getTaxableBase() == null || line.getTaxableBase().signum() < 0
        || line.getTaxAmount() == null || line.getTaxAmount().signum() < 0
        || line.getLineTotal() == null || line.getLineTotal().signum() < 0
        || line.getSequence() == null || line.getSequence() <= 0
        || !sequences.add(line.getSequence())) throw error("Invoice line snapshot is invalid.");
  }

  private boolean blank(String value) { return value == null || value.isBlank(); }
  private boolean same(com.axelor.db.Model a, com.axelor.db.Model b) {
    return a == b || a != null && b != null && a.getId() != null && Objects.equals(a.getId(), b.getId());
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
