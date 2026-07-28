package com.odc.sales.accounting.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.sales.db.SalesInvoice;
import org.junit.jupiter.api.Test;

class SalesInvoicePostingValidatorTest {
  private final SalesInvoicePostingValidator validator =
      new SalesInvoicePostingValidatorImpl(null, null, null, null, null, null, null);

  @Test
  void rejectsDraftCancelledArchivedAndUnpersistedInvoicesWithoutMutation() {
    SalesInvoice invoice = new SalesInvoice();
    assertFalse(validator.isReadyForPosting(invoice));
    invoice.setId(1L); invoice.setStatus("DRAFT");
    assertThrows(IllegalArgumentException.class, () -> validator.validateForPosting(invoice));
    assertEquals("DRAFT", invoice.getStatus());
    invoice.setStatus("CANCELLED");
    assertThrows(IllegalArgumentException.class, () -> validator.validateForPosting(invoice));
    invoice.setStatus("CONFIRMED"); invoice.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> validator.validateForPosting(invoice));
    assertEquals("CONFIRMED", invoice.getStatus());
  }
}
