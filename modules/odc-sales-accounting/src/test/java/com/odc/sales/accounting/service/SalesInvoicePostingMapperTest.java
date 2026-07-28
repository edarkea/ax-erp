package com.odc.sales.accounting.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.*;
import com.odc.accounting.service.AccountingSetupResolution;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.party.db.Party;
import com.odc.reference.db.Currency;
import com.odc.sales.db.SalesInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SalesInvoicePostingMapperTest {
  private SalesInvoice invoice;
  private SalesInvoicePostingMapper mapper;

  @BeforeEach
  void setUp() {
    Company company = new Company(); company.setId(1L);
    Branch branch = new Branch(); branch.setId(2L); branch.setCompany(company);
    Currency currency = new Currency(); currency.setId(3L);
    Party customer = new Party(); customer.setId(4L); customer.setCompany(company);
    AccountingPeriod period = new AccountingPeriod(); period.setId(5L); period.setCompany(company);
    invoice = new SalesInvoice(); invoice.setId(10L); invoice.setCompany(company);
    invoice.setBranch(branch); invoice.setCurrency(currency); invoice.setCustomer(customer);
    invoice.setInvoiceDate(LocalDate.of(2026, 7, 28));
    invoice.setDueDate(LocalDate.of(2026, 8, 28)); invoice.setExchangeRate(BigDecimal.ONE);
    invoice.setDocumentNo("001-001-000000001");
    invoice.setSubtotal(new BigDecimal("200.00")); invoice.setTaxTotal(new BigDecimal("30.00"));
    invoice.setGrandTotal(new BigDecimal("230.00"));
    SalesInvoicePostingContext context = new SalesInvoicePostingContext(
        period, resolution("ACCOUNT_RECEIVABLE"), resolution("SALES_REVENUE"),
        resolution("OUTPUT_TAX"));
    mapper = new SalesInvoicePostingMapperImpl(new ValidatorStub(context));
  }

  @Test
  void mapsBalancedThreeLinePostingFromSnapshots() {
    SalesInvoicePostingPlan plan = mapper.map(invoice);
    assertEquals("SALES", plan.journalEntry().getEntryType());
    assertEquals("DRAFT", plan.journalEntry().getStatus());
    assertEquals(invoice.getInvoiceDate(), plan.journalEntry().getAccountingDate());
    assertEquals(SalesAccountingConstants.SALES_INVOICE_SOURCE_MODEL,
        plan.journalEntry().getSourceModel());
    assertEquals(invoice.getId(), plan.journalEntry().getSourceRecordId());
    assertEquals(invoice.getDocumentNo(), plan.journalEntry().getSourceDocumentNo());
    assertNull(plan.journalEntry().getEntryNumber());
    assertEquals(3, plan.lineCount());
    assertEquals(new BigDecimal("230.00"), plan.journalLines().get(0).getDebit());
    assertEquals(new BigDecimal("200.00"), plan.journalLines().get(1).getCredit());
    assertEquals(new BigDecimal("30.00"), plan.journalLines().get(2).getCredit());
    assertEquals(0, plan.totalDebit().compareTo(plan.totalCredit()));
  }

  @Test
  void omitsOutputTaxLineWhenTaxIsZero() {
    invoice.setTaxTotal(BigDecimal.ZERO);
    invoice.setGrandTotal(new BigDecimal("200.00"));
    assertEquals(2, mapper.map(invoice).lineCount());
  }

  @Test
  void modelsHaveNoDirectSalesAccountingRelationship() throws Exception {
    assertFalse(java.util.Arrays.stream(SalesInvoice.class.getMethods())
        .anyMatch(method -> method.getName().toLowerCase().contains("journalentry")));
    assertFalse(java.util.Arrays.stream(JournalEntry.class.getMethods())
        .anyMatch(method -> method.getName().toLowerCase().contains("salesinvoice")));
    assertEquals(Long.class,
        JournalEntry.class.getMethod("getSourceRecordId").getReturnType());
    assertEquals(String.class,
        JournalEntry.class.getMethod("getSourceDocumentNo").getReturnType());
  }

  private AccountingSetupResolution resolution(String code) {
    AccountingRoleDefinition role = new AccountingRoleDefinition(); role.setCode(code);
    AccountingSetupEntry setup = new AccountingSetupEntry();
    ChartAccount account = new ChartAccount(); account.setCode(code);
    setup.setAccount(account); setup.setAccountingRoleDefinition(role);
    return new AccountingSetupResolution(setup, account, role, null, null, null, 0);
  }

  private record ValidatorStub(SalesInvoicePostingContext context)
      implements SalesInvoicePostingValidator {
    public void validateForPosting(SalesInvoice invoice) {}
    public boolean isReadyForPosting(SalesInvoice invoice) { return true; }
    public SalesInvoicePostingContext validateAndResolve(SalesInvoice invoice) { return context; }
  }
}
