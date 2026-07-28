package com.odc.document.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.auth.db.User;
import com.odc.document.db.DocumentSequenceReservation;
import com.odc.document.db.DocumentSeries;
import com.odc.document.db.EmissionEstablishment;
import com.odc.document.db.PointOfSale;
import com.odc.document.db.UserPointAssignment;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.AccessValidationService;
import com.odc.organization.service.BranchService;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DocumentControlValidationTest {

  @Test
  void shouldRejectPointFromAnotherEstablishment() {
    EmissionEstablishment first = establishment(1L);
    EmissionEstablishment second = establishment(2L);
    PointOfSale point = point(first);
    DocumentSeries series = series(second, point);
    assertThrows(IllegalArgumentException.class,
        () -> new TestSeriesService().validate(series));
  }

  @Test
  void shouldRejectInvalidPaddingAndPattern() {
    EmissionEstablishment establishment = establishment(1L);
    DocumentSeries series = series(establishment, point(establishment));
    series.setPaddingLength(21);
    assertThrows(IllegalArgumentException.class,
        () -> new TestSeriesService().validate(series));
    series.setPaddingLength(9);
    series.setDisplayPattern("{EST}-{BAD}-{SEQ}");
    assertThrows(IllegalArgumentException.class,
        () -> new TestSeriesService().validate(series));
  }

  @Test
  void shouldAcceptValidSeriesPattern() {
    EmissionEstablishment establishment = establishment(1L);
    assertDoesNotThrow(
        () -> new TestSeriesService().validate(series(establishment, point(establishment))));
  }

  @Test
  void shouldRejectUserWithoutDerivedAccess() {
    UserPointAssignment assignment = new UserPointAssignment();
    assignment.setUser(new User());
    assignment.setPointOfSale(point(establishment(1L)));
    UserPointAssignmentServiceImpl service =
        new UserPointAssignmentServiceImpl(
            null, null, null, new ConfigurationStub(), new AccessValidationService()) {
          @Override protected boolean hasCompanyAccess(UserPointAssignment value, Company company) {
            return false;
          }
          @Override protected boolean hasBranchAccess(UserPointAssignment value, Branch branch) {
            return false;
          }
        };
    assertThrows(IllegalArgumentException.class, () -> service.validate(assignment));
  }

  @Test
  void shouldKeepCompanyOutOfEveryChild() {
    assertFalse(hasCompany(EmissionEstablishment.class));
    assertFalse(hasCompany(PointOfSale.class));
    assertFalse(hasCompany(UserPointAssignment.class));
    assertFalse(hasCompany(DocumentSeries.class));
    assertFalse(hasCompany(DocumentSequenceReservation.class));
  }

  @Test
  void shouldInitializeEmbeddedPointArchiveState() {
    EmissionEstablishment establishment = establishment(1L);
    PointOfSale point = new PointOfSale();
    point.setCode("  p01 ");
    point.setName("Point");
    point.setType("PHYSICAL");
    establishment.setPointsOfSale(new ArrayList<>());
    establishment.getPointsOfSale().add(point);

    new TestConfigurationService().validate(establishment);

    assertEquals(Boolean.FALSE, point.getArchived());
    assertEquals(Boolean.TRUE, point.getActive());
    assertEquals(Boolean.FALSE, point.getIsDefault());
    assertSame(establishment, point.getEmissionEstablishment());
  }

  private boolean hasCompany(Class<?> type) {
    return Arrays.stream(type.getMethods()).anyMatch(method -> method.getName().equals("getCompany"));
  }
  private static Company company() {
    Company value = new Company();
    value.setId(1L); value.setActive(true); value.setArchived(false);
    return value;
  }
  private static EmissionEstablishment establishment(Long id) {
    Branch branch = new Branch();
    branch.setId(id); branch.setCompany(company()); branch.setActive(true); branch.setArchived(false);
    EmissionEstablishment value = new EmissionEstablishment();
    value.setId(id); value.setBranch(branch); value.setCode("001"); value.setName("Main");
    value.setActive(true); value.setArchived(false);
    return value;
  }
  private static PointOfSale point(EmissionEstablishment establishment) {
    PointOfSale value = new PointOfSale();
    value.setId(1L); value.setEmissionEstablishment(establishment);
    value.setCode("001"); value.setName("Point"); value.setType("PHYSICAL");
    value.setActive(true); value.setArchived(false);
    return value;
  }
  private static DocumentSeries series(
      EmissionEstablishment establishment, PointOfSale point) {
    DocumentSeries value = new DocumentSeries();
    value.setEmissionEstablishment(establishment); value.setPointOfSale(point);
    value.setDocumentType("SALES_INVOICE"); value.setCurrentSequence(0L);
    value.setPaddingLength(9); value.setDisplayPattern("{EST}-{POS}-{SEQ}");
    value.setActive(true); value.setArchived(false);
    return value;
  }
  private static class TestSeriesService extends DocumentSeriesServiceImpl {
    TestSeriesService() { super(null, null, new ConfigurationStub()); }
    @Override protected DocumentSeries findDuplicate(DocumentSeries value) { return null; }
    @Override protected boolean hasReservations(Long seriesId) { return false; }
  }
  private static class ConfigurationStub implements EmissionConfigurationService {
    public EmissionEstablishment save(EmissionEstablishment value) { return value; }
    public PointOfSale save(PointOfSale value) { return value; }
    public void validate(EmissionEstablishment value) {}
    public void validate(PointOfSale value) {}
    public void requireUsable(EmissionEstablishment value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
    public void requireUsable(PointOfSale value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
  }

  private static class TestConfigurationService extends EmissionConfigurationServiceImpl {
    TestConfigurationService() { super(null, null, new BranchStub()); }
    @Override protected EmissionEstablishment findDuplicateEstablishment(
        EmissionEstablishment value) { return null; }
    @Override protected PointOfSale findDuplicatePoint(PointOfSale value) { return null; }
    @Override protected EmissionEstablishment otherDefaultEstablishment(
        EmissionEstablishment value) { return null; }
    @Override protected PointOfSale otherDefaultPoint(PointOfSale value) { return null; }
  }

  private static class BranchStub implements BranchService {
    public Branch save(Branch value) { return value; }
    public void validate(Branch value) {}
    public void validate(Branch value, Company company) {}
    public void setDefault(Branch value) {}
    public void archive(Branch value) {}
    public void requireUsable(Branch value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
  }
}
