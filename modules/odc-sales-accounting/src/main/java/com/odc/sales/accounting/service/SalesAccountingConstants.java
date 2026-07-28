package com.odc.sales.accounting.service;

import com.odc.sales.db.SalesInvoice;

public final class SalesAccountingConstants {
  public static final String SALES_INVOICE_SOURCE_MODEL = SalesInvoice.class.getName();
  public static final String SOURCE_MODULE = "sales";
  public static final String DOCUMENT_GROUP = "SALES";
  public static final String DOCUMENT_TYPE = "SALES_INVOICE";
  public static final String ACCOUNT_RECEIVABLE = "ACCOUNT_RECEIVABLE";
  public static final String SALES_REVENUE = "SALES_REVENUE";
  public static final String OUTPUT_TAX = "OUTPUT_TAX";

  private SalesAccountingConstants() {}
}
