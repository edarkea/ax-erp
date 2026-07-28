package com.odc.document.service;

import com.odc.document.db.DocumentSequenceReservation;
import com.odc.document.db.DocumentSeries;

public interface DocumentSequenceService {
  DocumentSequenceReservation reserve(
      DocumentSeries series, String documentModel, Long documentId, String correlationKey);
  DocumentSequenceReservation consume(DocumentSequenceReservation reservation, Long documentId);
  DocumentSequenceReservation voidReservation(
      DocumentSequenceReservation reservation, String reason);
  String formatDocumentNo(DocumentSeries series, long sequence);
}
