package com.odc.catalog.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.catalog.db.ItemCategory;
import com.odc.catalog.db.repo.ItemCategoryRepository;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ItemCategoryServiceImpl implements ItemCategoryService {
  private static final int MAX_DEPTH=100;
  private final ItemCategoryRepository repository;
  private final ActiveOrganizationService organizationService;
  @Inject public ItemCategoryServiceImpl(ItemCategoryRepository repository,ActiveOrganizationService organizationService){
    this.repository=repository;this.organizationService=organizationService;}
  @Override @Transactional public ItemCategory save(ItemCategory category){
    if(category==null)throw error("Item category is required.");
    Company active=organizationService.requireActiveCompany();if(category.getId()==null)category.setCompany(active);
    validate(category);return persist(category);}
  @Override public void validate(ItemCategory category){
    if(category==null)throw error("Item category is required.");
    Company active=organizationService.requireActiveCompany();
    if(category.getId()==null)category.setCompany(active);
    requireCompany(category.getCompany());
    if(!same(category.getCompany(),active))throw error("You do not have access to the record company.");
    category.setCode(required(category.getCode(),"Category code is required.").toUpperCase(Locale.ROOT));
    category.setName(required(category.getName(),"Category name is required."));
    if(category.getSequence()==null)category.setSequence(0);if(category.getSequence()<0)throw error("Sequence cannot be negative.");
    if(category.getArchived()==null)category.setArchived(false);if(category.getActive()==null)category.setActive(true);
    validateHierarchy(category);
    if(Boolean.TRUE.equals(category.getArchived())&&hasActiveChildren(category))
      throw error("Cannot archive a category with active children.");
    if(Boolean.TRUE.equals(category.getArchived())&&hasActiveItems(category))
      throw error("Cannot archive a category with active items.");
    if(!Boolean.TRUE.equals(category.getArchived())&&findDuplicate(category)!=null)throw error("Category code already exists.");
    Company persisted=findPersistedCompany(category.getId());if(persisted!=null&&!same(persisted,category.getCompany()))
      throw error("Item category company cannot be changed.");
  }
  @Override public void validateHierarchy(ItemCategory category){
    ItemCategory parent=category.getParent();Set<ItemCategory> visited=Collections.newSetFromMap(new IdentityHashMap<>());
    for(int depth=0;parent!=null&&depth<MAX_DEPTH;depth++){
      if(parent==category||(category.getId()!=null&&Objects.equals(category.getId(),parent.getId())))
        throw error("A category cannot be its own parent.");
      if(!visited.add(parent))throw error("Category hierarchy contains a cycle.");
      if(!same(category.getCompany(),parent.getCompany()))throw error("Category belongs to another company.");
      if(Boolean.TRUE.equals(parent.getArchived())||!Boolean.TRUE.equals(parent.getActive()))
        throw error("Parent category must be active.");
      parent=parent.getParent();
    }
    if(parent!=null)throw error("Category hierarchy contains a cycle.");
  }
  @Override @Transactional public void archive(ItemCategory category){
    if(category==null||category.getId()==null)throw error("Item category is required.");
    if(hasActiveChildren(category))throw error("Cannot archive a category with active children.");
    if(hasActiveItems(category))throw error("Cannot archive a category with active items.");
    category.setArchived(true);category.setActive(false);persist(category);}
  @Override @Transactional public ItemCategory restore(ItemCategory category){
    if(category==null||category.getId()==null)throw error("Item category is required.");
    category.setArchived(false);category.setActive(true);validate(category);return persist(category);}
  @Override public void requireUsable(ItemCategory c){if(c==null||Boolean.TRUE.equals(c.getArchived())||
      !Boolean.TRUE.equals(c.getActive()))throw error("Item category is archived.");}
  protected boolean hasActiveChildren(ItemCategory c){return repository.all().filter(
      "self.parent = :parent AND self.archived = false AND self.active = true").bind("parent",c).count()>0;}
  protected boolean hasActiveItems(ItemCategory c){return c.getId()!=null&&((Number)JPA.em().createQuery(
      "SELECT COUNT(self) FROM Item self WHERE self.category = :category AND self.archived = false AND self.active = true")
      .setParameter("category",c).getSingleResult()).longValue()>0;}
  protected ItemCategory findDuplicate(ItemCategory c){String f="self.company = :company AND self.code = :code AND self.archived = false";
    var q=repository.all().filter(f).bind("company",c.getCompany()).bind("code",c.getCode());if(c.getId()!=null)
      q=repository.all().filter(f+" AND self.id != :id").bind("company",c.getCompany()).bind("code",c.getCode()).bind("id",c.getId());return q.fetchOne();}
  protected Company findPersistedCompany(Long id){return id==null?null:repository.find(id).getCompany();}
  protected ItemCategory persist(ItemCategory c){return repository.save(c);}
  private void requireCompany(Company c){if(c==null)throw error("An active company must be selected.");if(Boolean.TRUE.equals(c.getArchived())||!Boolean.TRUE.equals(c.getActive()))throw error("Company must be active.");}
  private boolean same(Company a,Company b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId()));}
  private String required(String v,String k){if(v==null||v.trim().isEmpty())throw error(k);return v.trim();}
  private IllegalArgumentException error(String k){return new IllegalArgumentException(I18n.get(k));}
}
