package com.odc.sales.service;
import com.axelor.auth.db.User;import com.odc.document.db.DocumentSequenceReservation;import com.odc.sales.db.SalesInvoice;import java.time.LocalDateTime;
public record SalesInvoiceConfirmationResult(SalesInvoice invoice,String documentNo,DocumentSequenceReservation reservation,LocalDateTime confirmedAt,User confirmedBy,SalesInvoiceTotals totals){}
