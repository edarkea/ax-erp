package com.odc.document.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.document.db.EmissionEstablishment;
import com.odc.document.db.PointOfSale;
import com.odc.document.db.repo.EmissionEstablishmentRepository;
import com.odc.document.db.repo.PointOfSaleRepository;
import com.odc.organization.service.BranchService;

public class EmissionConfigurationServiceImpl implements EmissionConfigurationService {
  private final EmissionEstablishmentRepository establishmentRepository;
  private final PointOfSaleRepository pointRepository;
  private final BranchService branchService;

  @Inject
  public EmissionConfigurationServiceImpl(
      EmissionEstablishmentRepository establishmentRepository,
      PointOfSaleRepository pointRepository,
      BranchService branchService) {
    this.establishmentRepository = establishmentRepository;
    this.pointRepository = pointRepository;
    this.branchService = branchService;
  }

  @Override @Transactional
  public EmissionEstablishment save(EmissionEstablishment value) {
    validate(value);
    return establishmentRepository.save(value);
  }

  @Override @Transactional
  public PointOfSale save(PointOfSale value) {
    validate(value);
    return pointRepository.save(value);
  }

  @Override
  public void validate(EmissionEstablishment value) {
    if (value == null || value.getBranch() == null) throw error("Branch is required.");
    branchService.requireUsable(value.getBranch());
    value.setCode(code(value.getCode()));
    value.setName(name(value.getName()));
    defaults(value);
    if (value.getPointsOfSale() != null) {
      value.getPointsOfSale().forEach(
          point -> {
            point.setEmissionEstablishment(value);
            defaults(point);
          });
    }
    if (findDuplicateEstablishment(value) != null)
      throw error("Establishment code already exists in this branch.");
    if (Boolean.TRUE.equals(value.getIsDefault()) && otherDefaultEstablishment(value) != null)
      throw error("Branch already has a default establishment.");
  }

  @Override
  public void validate(PointOfSale value) {
    if (value == null || value.getEmissionEstablishment() == null)
      throw error("Emission establishment is required.");
    requireUsable(value.getEmissionEstablishment());
    value.setCode(code(value.getCode()));
    value.setName(name(value.getName()));
    if (value.getType() == null || value.getType().isBlank()) throw error("Point type is required.");
    defaults(value);
    if (findDuplicatePoint(value) != null)
      throw error("Point code already exists in this establishment.");
    if (Boolean.TRUE.equals(value.getIsDefault()) && otherDefaultPoint(value) != null)
      throw error("Establishment already has a default point for this type.");
  }

  @Override
  public void requireUsable(EmissionEstablishment value) {
    if (value == null || Boolean.TRUE.equals(value.getArchived())
        || !Boolean.TRUE.equals(value.getActive())) throw error("Establishment must be active.");
    branchService.requireUsable(value.getBranch());
  }

  @Override
  public void requireUsable(PointOfSale value) {
    if (value == null || Boolean.TRUE.equals(value.getArchived())
        || !Boolean.TRUE.equals(value.getActive())) throw error("Point of sale must be active.");
    requireUsable(value.getEmissionEstablishment());
  }

  protected EmissionEstablishment otherDefaultEstablishment(EmissionEstablishment value) {
    String filter =
        "self.branch = :parent AND self.isDefault = true AND self.active = true "
            + "AND self.archived = false";
    var query = establishmentRepository.all().filter(filter).bind("parent", value.getBranch());
    if (value.getId() != null) query = establishmentRepository.all()
        .filter(filter + " AND self.id != :id").bind("parent", value.getBranch())
        .bind("id", value.getId());
    return query.fetchOne();
  }

  protected EmissionEstablishment findDuplicateEstablishment(EmissionEstablishment value) {
    String filter = "self.branch = :parent AND self.code = :code AND self.archived = false";
    var query = establishmentRepository.all().filter(filter)
        .bind("parent", value.getBranch()).bind("code", value.getCode());
    if (value.getId() != null) query = establishmentRepository.all()
        .filter(filter + " AND self.id != :id").bind("parent", value.getBranch())
        .bind("code", value.getCode()).bind("id", value.getId());
    return query.fetchOne();
  }

  protected PointOfSale findDuplicatePoint(PointOfSale value) {
    String filter =
        "self.emissionEstablishment = :parent AND self.code = :code AND self.archived = false";
    var query = pointRepository.all().filter(filter)
        .bind("parent", value.getEmissionEstablishment()).bind("code", value.getCode());
    if (value.getId() != null) query = pointRepository.all()
        .filter(filter + " AND self.id != :id").bind("parent", value.getEmissionEstablishment())
        .bind("code", value.getCode()).bind("id", value.getId());
    return query.fetchOne();
  }

  protected PointOfSale otherDefaultPoint(PointOfSale value) {
    String filter =
        "self.emissionEstablishment = :parent AND self.type = :type AND self.isDefault = true "
            + "AND self.active = true AND self.archived = false";
    var query = pointRepository.all().filter(filter)
        .bind("parent", value.getEmissionEstablishment()).bind("type", value.getType());
    if (value.getId() != null) query = pointRepository.all()
        .filter(filter + " AND self.id != :id").bind("parent", value.getEmissionEstablishment())
        .bind("type", value.getType()).bind("id", value.getId());
    return query.fetchOne();
  }

  private void defaults(EmissionEstablishment value) {
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getIsDefault() == null) value.setIsDefault(false);
  }
  private void defaults(PointOfSale value) {
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getIsDefault() == null) value.setIsDefault(false);
  }
  private String code(String value) {
    if (value == null || value.trim().isEmpty()) throw error("Code is required.");
    return value.trim().toUpperCase();
  }
  private String name(String value) {
    if (value == null || value.trim().isEmpty()) throw error("Name is required.");
    return value.trim();
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
