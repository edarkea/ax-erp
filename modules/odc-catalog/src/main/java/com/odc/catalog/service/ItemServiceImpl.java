package com.odc.catalog.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.catalog.db.Item;
import com.odc.catalog.db.repo.ItemRepository;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ItemServiceImpl implements ItemService {
  private static final List<String>TYPES=List.of("PRODUCT","SERVICE");
  private final ItemRepository repository;
  private final ActiveOrganizationService organizationService;
  private final CatalogValidationService validation;
  @Inject public ItemServiceImpl(ItemRepository repository,ActiveOrganizationService organizationService,
      CatalogValidationService validation){this.repository=repository;this.organizationService=organizationService;this.validation=validation;}
  @Override @Transactional public Item save(Item item){
    if(item==null)throw error("Item is required.");Company active=organizationService.requireActiveCompany();
    if(item.getId()==null)item.setCompany(active);validate(item);return persist(item);}
  @Override public void validate(Item item){
    if(item==null)throw error("Item is required.");Company active=organizationService.requireActiveCompany();
    if(item.getId()==null)item.setCompany(active);
    validation.requireActiveCompany(item.getCompany());if(!same(item.getCompany(),active))throw error("You do not have access to the record company.");
    if(item.getItemType()==null||!TYPES.contains(item.getItemType()))throw error("Item type is required.");
    item.setSku(required(item.getSku(),"SKU is required.").toUpperCase(Locale.ROOT));
    item.setName(required(item.getName(),"Item name is required."));item.setBarcode(normalizeBarcode(item.getBarcode()));
    if("PRODUCT".equals(item.getItemType())&&item.getUom()==null)throw error("A product requires a unit of measure.");
    if("SERVICE".equals(item.getItemType())&&item.getBarcode()!=null)throw error("A service cannot have a barcode.");
    validation.requireUsableUnit(item.getUom());validation.requireCategoryForCompany(item.getCategory(),item.getCompany());
    validation.requireCompatibleTax(item.getTaxCategory(),item.getCompany());
    if(item.getArchived()==null)item.setArchived(false);if(item.getActive()==null)item.setActive(true);
    if(!Boolean.TRUE.equals(item.getArchived())&&findSkuDuplicate(item)!=null)throw error("SKU already exists.");
    if(!Boolean.TRUE.equals(item.getArchived())&&item.getBarcode()!=null&&findBarcodeDuplicate(item)!=null)throw error("Barcode already exists.");
    Company persisted=findPersistedCompany(item.getId());if(persisted!=null&&!same(persisted,item.getCompany()))throw error("Item company cannot be changed.");
  }
  @Override @Transactional public void archive(Item item){if(item==null||item.getId()==null)throw error("Item is required.");
    item.setArchived(true);item.setActive(false);persist(item);}
  @Override @Transactional public Item restore(Item item){if(item==null||item.getId()==null)throw error("Item is required.");
    item.setArchived(false);item.setActive(true);validate(item);return persist(item);}
  @Override public void requireUsable(Item item){if(item==null||Boolean.TRUE.equals(item.getArchived())||!Boolean.TRUE.equals(item.getActive()))throw error("Item is archived.");}
  protected Item findSkuDuplicate(Item i){return duplicate(i,"self.company = :company AND self.sku = :value AND self.archived = false",i.getSku());}
  protected Item findBarcodeDuplicate(Item i){return duplicate(i,"self.company = :company AND self.barcode = :value AND self.archived = false",i.getBarcode());}
  private Item duplicate(Item i,String f,String value){var q=repository.all().filter(f).bind("company",i.getCompany()).bind("value",value);
    if(i.getId()!=null)q=repository.all().filter(f+" AND self.id != :id").bind("company",i.getCompany()).bind("value",value).bind("id",i.getId());return q.fetchOne();}
  protected Company findPersistedCompany(Long id){return id==null?null:repository.find(id).getCompany();}
  protected Item persist(Item i){return repository.save(i);}
  private String normalizeBarcode(String v){if(v==null)return null;String x=v.replaceAll("\\s+","");return x.isEmpty()?null:x.toUpperCase(Locale.ROOT);}
  private String required(String v,String k){if(v==null||v.trim().isEmpty())throw error(k);return v.trim();}
  private boolean same(Company a,Company b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId()));}
  private IllegalArgumentException error(String k){return new IllegalArgumentException(I18n.get(k));}
}
