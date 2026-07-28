package com.odc.catalog.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.catalog.db.UnitOfMeasure;
import com.odc.catalog.db.repo.UnitOfMeasureRepository;
import java.util.Locale;

public class UnitOfMeasureServiceImpl implements UnitOfMeasureService {
  private final UnitOfMeasureRepository repository;
  @Inject public UnitOfMeasureServiceImpl(UnitOfMeasureRepository repository){this.repository=repository;}
  @Override @Transactional public UnitOfMeasure save(UnitOfMeasure unit){
    if(unit!=null&&unit.getId()==null){
      UnitOfMeasure archived=findArchived(unit.getCode());
      if(archived!=null){archived.setArchived(false);archived.setActive(true);
        archived.setName(unit.getName());archived.setSymbol(unit.getSymbol());
        archived.setDecimalPrecision(unit.getDecimalPrecision());unit=archived;}
    }
    validate(unit);return persist(unit);
  }
  @Override public void validate(UnitOfMeasure unit){
    if(unit==null)throw error("Unit of measure is required.");
    unit.setCode(required(unit.getCode(),"Unit code is required.").toUpperCase(Locale.ROOT));
    unit.setName(required(unit.getName(),"Unit name is required."));
    unit.setSymbol(trim(unit.getSymbol()));
    if(unit.getDecimalPrecision()==null)unit.setDecimalPrecision(4);
    if(unit.getDecimalPrecision()<0||unit.getDecimalPrecision()>8)throw error("Decimal precision must be between 0 and 8.");
    if(unit.getArchived()==null)unit.setArchived(false);if(unit.getActive()==null)unit.setActive(true);
    if(Boolean.TRUE.equals(unit.getArchived())&&hasActiveItems(unit))
      throw error("Cannot archive a unit of measure used by active items.");
    if(!Boolean.TRUE.equals(unit.getArchived())&&findDuplicate(unit)!=null)throw error("Unit code already exists.");
  }
  @Override @Transactional public void archive(UnitOfMeasure unit){
    if(unit==null||unit.getId()==null)throw error("Unit of measure is required.");
    if(hasActiveItems(unit))throw error("Cannot archive a unit of measure used by active items.");
    unit.setArchived(true);unit.setActive(false);persist(unit);
  }
  @Override @Transactional public UnitOfMeasure restore(UnitOfMeasure unit){
    if(unit==null||unit.getId()==null)throw error("Unit of measure is required.");
    unit.setArchived(false);unit.setActive(true);validate(unit);return persist(unit);
  }
  @Override public void requireUsable(UnitOfMeasure unit){
    if(unit==null||Boolean.TRUE.equals(unit.getArchived())||!Boolean.TRUE.equals(unit.getActive()))
      throw error("Unit of measure is archived.");
  }
  protected UnitOfMeasure findDuplicate(UnitOfMeasure u){String f="self.code = :code AND self.archived = false";
    var q=repository.all().filter(f).bind("code",u.getCode());if(u.getId()!=null)q=repository.all()
      .filter(f+" AND self.id != :id").bind("code",u.getCode()).bind("id",u.getId());return q.fetchOne();}
  protected UnitOfMeasure findArchived(String code){if(code==null)return null;return repository.all()
      .filter("self.code = :code AND self.archived = true").bind("code",code.trim().toUpperCase(Locale.ROOT)).fetchOne();}
  protected boolean hasActiveItems(UnitOfMeasure unit){return unit.getId()!=null&&((Number)JPA.em().createQuery(
      "SELECT COUNT(self) FROM Item self WHERE self.uom = :unit AND self.archived = false AND self.active = true")
      .setParameter("unit",unit).getSingleResult()).longValue()>0;}
  protected UnitOfMeasure persist(UnitOfMeasure u){return repository.save(u);}
  private String required(String v,String k){String x=trim(v);if(x==null)throw error(k);return x;}
  private String trim(String v){return v==null||v.trim().isEmpty()?null:v.trim();}
  private IllegalArgumentException error(String k){return new IllegalArgumentException(I18n.get(k));}
}
