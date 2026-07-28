package com.odc.document.service;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.document.db.DocumentSequenceReservation;
import com.odc.document.db.DocumentSeries;
import com.odc.document.db.repo.DocumentSequenceReservationRepository;
import com.odc.document.db.repo.DocumentSeriesRepository;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;

public class DocumentSequenceServiceImpl implements DocumentSequenceService {
  private final DocumentSeriesRepository seriesRepository;
  private final DocumentSequenceReservationRepository reservationRepository;
  private final DocumentSeriesService seriesService;

  @Inject
  public DocumentSequenceServiceImpl(
      DocumentSeriesRepository seriesRepository,
      DocumentSequenceReservationRepository reservationRepository,
      DocumentSeriesService seriesService) {
    this.seriesRepository = seriesRepository;
    this.reservationRepository = reservationRepository;
    this.seriesService = seriesService;
  }

  @Override @Transactional
  public DocumentSequenceReservation reserve(
      DocumentSeries requested, String model, Long documentId, String correlationKey) {
    if (requested == null || requested.getId() == null)
      throw error("A persisted document series is required.");
    if (model == null || model.trim().isEmpty()) throw error("Document model is required.");
    if (documentId == null && (correlationKey == null || correlationKey.trim().isEmpty()))
      throw error("Document ID or correlation key is required.");
    String normalizedCorrelation =
        correlationKey == null || correlationKey.trim().isEmpty() ? null : correlationKey.trim();
    if (normalizedCorrelation != null) {
      DocumentSequenceReservation existing =
          findByCorrelation(requested, model.trim(), normalizedCorrelation);
      if (existing != null) return existing;
    }
    DocumentSeries series = lockSeries(requested.getId());
    seriesService.requireUsable(series);
    long next = Math.addExact(series.getCurrentSequence(), 1L);
    series.setCurrentSequence(next);
    saveSeries(series);
    DocumentSequenceReservation reservation = new DocumentSequenceReservation();
    reservation.setDocumentSeries(series);
    reservation.setDocumentModel(model.trim());
    reservation.setDocumentId(documentId);
    reservation.setCorrelationKey(normalizedCorrelation);
    reservation.setSequenceNumber(next);
    reservation.setDocumentNo(formatDocumentNo(series, next));
    reservation.setStatus("RESERVED");
    var user = currentUser();
    if (user == null) throw error("Authenticated user is required to reserve a sequence.");
    reservation.setReservedBy(user);
    reservation.setReservedAt(LocalDateTime.now());
    reservation.setArchived(false);
    return saveReservation(reservation);
  }

  @Override @Transactional
  public DocumentSequenceReservation consume(
      DocumentSequenceReservation reservation, Long documentId) {
    requireReserved(reservation);
    if (documentId == null) throw error("Document ID is required to consume a reservation.");
    reservation.setDocumentId(documentId);
    reservation.setStatus("CONSUMED");
    reservation.setConsumedAt(LocalDateTime.now());
    return saveReservation(reservation);
  }

  @Override @Transactional
  public DocumentSequenceReservation voidReservation(
      DocumentSequenceReservation reservation, String reason) {
    requireReserved(reservation);
    if (reason == null || reason.trim().isEmpty())
      throw error("Void reason is required.");
    reservation.setStatus("VOID");
    reservation.setVoidReason(reason.trim());
    reservation.setVoidedAt(LocalDateTime.now());
    return saveReservation(reservation);
  }

  @Override
  public String formatDocumentNo(DocumentSeries series, long sequence) {
    if (series == null || sequence <= 0) throw error("Series and positive sequence are required.");
    String padded = String.format("%0" + series.getPaddingLength() + "d", sequence);
    return series.getDisplayPattern()
        .replace("{EST}", series.getEmissionEstablishment().getCode())
        .replace("{POS}", series.getPointOfSale().getCode())
        .replace("{TYPE}", series.getDocumentType())
        .replace("{SEQ}", padded);
  }

  protected DocumentSeries lockSeries(Long id) {
    return JPA.em().find(DocumentSeries.class, id, LockModeType.PESSIMISTIC_WRITE);
  }
  protected DocumentSeries saveSeries(DocumentSeries series) {
    return seriesRepository.save(series);
  }
  protected DocumentSequenceReservation saveReservation(
      DocumentSequenceReservation reservation) {
    return reservationRepository.save(reservation);
  }
  protected com.axelor.auth.db.User currentUser() {
    return AuthUtils.getUser();
  }
  protected DocumentSequenceReservation findByCorrelation(
      DocumentSeries series, String model, String correlation) {
    return reservationRepository.all()
        .filter("self.documentSeries = :series AND self.documentModel = :model "
            + "AND self.correlationKey = :key")
        .bind("series", series).bind("model", model).bind("key", correlation).fetchOne();
  }
  private void requireReserved(DocumentSequenceReservation reservation) {
    if (reservation == null || reservation.getId() == null
        || !"RESERVED".equals(reservation.getStatus()))
      throw error("Only a reserved sequence can make this transition.");
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
