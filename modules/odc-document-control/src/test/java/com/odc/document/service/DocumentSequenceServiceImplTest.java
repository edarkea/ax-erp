package com.odc.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.auth.db.User;
import com.odc.document.db.DocumentSequenceReservation;
import com.odc.document.db.DocumentSeries;
import com.odc.document.db.EmissionEstablishment;
import com.odc.document.db.PointOfSale;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentSequenceServiceImplTest {
  private DocumentSeries series;
  private TestService service;

  @BeforeEach
  void setUp() {
    EmissionEstablishment establishment = new EmissionEstablishment();
    establishment.setCode("001");
    PointOfSale point = new PointOfSale();
    point.setCode("002"); point.setEmissionEstablishment(establishment);
    series = new DocumentSeries();
    series.setId(1L); series.setEmissionEstablishment(establishment); series.setPointOfSale(point);
    series.setDocumentType("SALES_INVOICE"); series.setCurrentSequence(0L);
    series.setPaddingLength(4); series.setDisplayPattern("{EST}-{POS}-{SEQ}");
    series.setActive(true); series.setArchived(false);
    service = new TestService(series);
  }

  @Test
  void shouldReserveDifferentNeverReusedNumbers() {
    DocumentSequenceReservation first = service.reserve(series, "Invoice", null, "A");
    service.voidReservation(first, "Cancelled");
    DocumentSequenceReservation second = service.reserve(series, "Invoice", null, "B");
    assertEquals(1L, first.getSequenceNumber());
    assertEquals(2L, second.getSequenceNumber());
    assertEquals("001-002-0002", second.getDocumentNo());
  }

  @Test
  void shouldBeIdempotentByCorrelation() {
    DocumentSequenceReservation first = service.reserve(series, "Invoice", null, "A");
    assertSame(first, service.reserve(series, "Invoice", null, "A"));
    assertEquals(1L, series.getCurrentSequence());
  }

  @Test
  void shouldEnforceIrreversibleTransitionsAndVoidReason() {
    DocumentSequenceReservation reservation = service.reserve(series, "Invoice", null, "A");
    assertThrows(IllegalArgumentException.class,
        () -> service.voidReservation(reservation, " "));
    service.consume(reservation, 10L);
    assertThrows(IllegalArgumentException.class,
        () -> service.voidReservation(reservation, "Cannot void consumed"));
  }

  private static class TestService extends DocumentSequenceServiceImpl {
    private final DocumentSeries locked;
    private final Map<String, DocumentSequenceReservation> byCorrelation = new HashMap<>();
    private long id;
    TestService(DocumentSeries locked) {
      super(null, null, new SeriesStub());
      this.locked = locked;
    }
    @Override protected DocumentSeries lockSeries(Long ignored) { return locked; }
    @Override protected DocumentSeries saveSeries(DocumentSeries value) { return value; }
    @Override protected User currentUser() { User user = new User(); user.setId(1L); return user; }
    @Override protected DocumentSequenceReservation saveReservation(
        DocumentSequenceReservation value) {
      if (value.getId() == null) value.setId(++id);
      if (value.getCorrelationKey() != null)
        byCorrelation.put(value.getDocumentModel() + "|" + value.getCorrelationKey(), value);
      return value;
    }
    @Override protected DocumentSequenceReservation findByCorrelation(
        DocumentSeries series, String model, String correlation) {
      return byCorrelation.get(model + "|" + correlation);
    }
  }
  private static class SeriesStub implements DocumentSeriesService {
    public DocumentSeries save(DocumentSeries value) { return value; }
    public void validate(DocumentSeries value) {}
    public void requireUsable(DocumentSeries value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
  }
}
