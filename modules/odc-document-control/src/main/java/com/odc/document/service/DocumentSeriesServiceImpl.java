package com.odc.document.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.document.db.DocumentSeries;
import com.odc.document.db.repo.DocumentSeriesRepository;
import com.odc.document.db.repo.DocumentSequenceReservationRepository;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentSeriesServiceImpl implements DocumentSeriesService {
  private static final Pattern TOKEN = Pattern.compile("\\{([A-Z]+)}");
  private static final Set<String> ALLOWED = Set.of("EST", "POS", "TYPE", "SEQ");
  private final DocumentSeriesRepository repository;
  private final DocumentSequenceReservationRepository reservationRepository;
  private final EmissionConfigurationService configurationService;

  @Inject
  public DocumentSeriesServiceImpl(
      DocumentSeriesRepository repository,
      DocumentSequenceReservationRepository reservationRepository,
      EmissionConfigurationService configurationService) {
    this.repository = repository;
    this.reservationRepository = reservationRepository;
    this.configurationService = configurationService;
  }

  @Override @Transactional
  public DocumentSeries save(DocumentSeries value) {
    validate(value);
    return repository.save(value);
  }

  @Override
  public void validate(DocumentSeries value) {
    if (value == null || value.getEmissionEstablishment() == null || value.getPointOfSale() == null)
      throw error("Establishment and point of sale are required.");
    configurationService.requireUsable(value.getEmissionEstablishment());
    configurationService.requireUsable(value.getPointOfSale());
    if (!same(value.getEmissionEstablishment(), value.getPointOfSale().getEmissionEstablishment()))
      throw error("Point of sale belongs to another establishment.");
    if (value.getDocumentType() == null || value.getDocumentType().isBlank())
      throw error("Document type is required.");
    defaults(value);
    if (value.getCurrentSequence() < 0) throw error("Current sequence cannot be negative.");
    if (value.getPaddingLength() < 1 || value.getPaddingLength() > 20)
      throw error("Padding length must be between 1 and 20.");
    validatePattern(value.getDisplayPattern());
    if (findDuplicate(value) != null)
      throw error("An active series already exists for this context and document type.");
    DocumentSeries persisted = value.getId() == null ? null : repository.find(value.getId());
    if (persisted != null
        && Boolean.TRUE.equals(persisted.getIsAutomatic())
        && !Objects.equals(persisted.getCurrentSequence(), value.getCurrentSequence())) {
      throw error("Automatic series counter can only be changed by the sequence service.");
    }
    if (persisted != null && hasReservations(value.getId())
        && (!same(persisted.getEmissionEstablishment(), value.getEmissionEstablishment())
            || !same(persisted.getPointOfSale(), value.getPointOfSale())
            || !Objects.equals(persisted.getDocumentType(), value.getDocumentType())))
      throw error("A used series cannot change its context or document type.");
  }

  @Override
  public void requireUsable(DocumentSeries value) {
    if (value == null || Boolean.TRUE.equals(value.getArchived())
        || !Boolean.TRUE.equals(value.getActive())) throw error("Document series must be active.");
    validate(value);
  }

  protected DocumentSeries findDuplicate(DocumentSeries value) {
    String filter =
        "self.emissionEstablishment = :establishment AND self.pointOfSale = :point "
            + "AND self.documentType = :type AND self.archived = false";
    var query = repository.all().filter(filter)
        .bind("establishment", value.getEmissionEstablishment())
        .bind("point", value.getPointOfSale()).bind("type", value.getDocumentType());
    if (value.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("establishment", value.getEmissionEstablishment())
        .bind("point", value.getPointOfSale()).bind("type", value.getDocumentType())
        .bind("id", value.getId());
    return query.fetchOne();
  }
  protected boolean hasReservations(Long seriesId) {
    return reservationRepository.all()
        .filter("self.documentSeries.id = :id").bind("id", seriesId).count() > 0;
  }

  private void validatePattern(String pattern) {
    if (pattern == null || pattern.isBlank() || !pattern.contains("{SEQ}"))
      throw error("Display pattern must contain the {SEQ} token.");
    Matcher matcher = TOKEN.matcher(pattern);
    while (matcher.find()) if (!ALLOWED.contains(matcher.group(1)))
      throw error("Display pattern contains an unsupported token.");
    String remaining = TOKEN.matcher(pattern).replaceAll("");
    if (remaining.contains("{") || remaining.contains("}"))
      throw error("Display pattern contains an unsupported token.");
  }
  private void defaults(DocumentSeries value) {
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getCurrentSequence() == null) value.setCurrentSequence(0L);
    if (value.getPaddingLength() == null) value.setPaddingLength(9);
    if (value.getIsAutomatic() == null) value.setIsAutomatic(true);
    if (value.getDisplayPattern() == null) value.setDisplayPattern("{EST}-{POS}-{SEQ}");
  }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || (left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId()));
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
