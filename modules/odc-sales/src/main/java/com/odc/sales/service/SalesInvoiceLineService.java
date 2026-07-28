package com.odc.sales.service;
import com.odc.sales.db.*;
import java.util.List;
public interface SalesInvoiceLineService {
 SalesInvoiceLine save(SalesInvoiceLine line); void validate(SalesInvoiceLine line);
 void archive(SalesInvoiceLine line); SalesInvoiceLine restore(SalesInvoiceLine line);
 List<SalesInvoiceLine> findActiveLines(SalesInvoice invoice); List<SalesInvoiceLine> lockActiveLines(SalesInvoice invoice);
 SalesInvoiceLine persistCalculated(SalesInvoiceLine line);
}
