package com.odc.catalog.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.catalog.db.*;
import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.reference.db.Country;
import com.odc.tax.db.TaxCategory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogServicesTest {
  @Test void unitIsGlobalNormalizedAndPrecisionBounded(){
    TestUnitService service=new TestUnitService();UnitOfMeasure unit=new UnitOfMeasure();
    unit.setCode(" kg ");unit.setName(" Kilogram ");unit.setDecimalPrecision(8);service.save(unit);
    assertEquals("KG",unit.getCode());assertEquals("Kilogram",unit.getName());
    unit.setDecimalPrecision(9);assertThrows(IllegalArgumentException.class,()->service.validate(unit));
    assertFalse(hasMethod(UnitOfMeasure.class,"getCompany"));
  }
  @Test void unitRestoreRevalidatesDuplicate(){
    TestUnitService service=new TestUnitService();UnitOfMeasure unit=new UnitOfMeasure();
    unit.setId(1L);unit.setCode("KG");unit.setName("Kilogram");unit.setDecimalPrecision(4);unit.setArchived(true);
    service.duplicate=new UnitOfMeasure();assertThrows(IllegalArgumentException.class,()->service.restore(unit));
  }
  @Test void categoryDetectsSelfTwoAndThreeLevelCycles(){
    Company company=company(1L,country(1L));TestCategoryService service=new TestCategoryService(company);
    ItemCategory a=category(company,"A"),b=category(company,"B"),c=category(company,"C");
    a.setParent(a);assertThrows(IllegalArgumentException.class,()->service.validate(a));
    a.setParent(b);b.setParent(a);assertThrows(IllegalArgumentException.class,()->service.validate(a));
    b.setParent(c);c.setParent(a);assertThrows(IllegalArgumentException.class,()->service.validate(a));
  }
  @Test void categoryRejectsOtherCompanyAndArchiveWithChildrenOrItems(){
    Company a=company(1L,country(1L)),b=company(2L,country(1L));
    TestCategoryService service=new TestCategoryService(a);ItemCategory child=category(a,"CH");
    child.setParent(category(b,"P"));assertThrows(IllegalArgumentException.class,()->service.validate(child));
    child.setId(3L);child.setParent(null);service.children=true;
    assertThrows(IllegalArgumentException.class,()->service.archive(child));
    service.children=false;service.items=true;
    assertThrows(IllegalArgumentException.class,()->service.archive(child));
  }
  @Test void createsProductAndServiceWithCompanyPolicy(){
    Company company=company(1L,country(1L));TestItemService service=new TestItemService(company);
    Item product=item("PRODUCT"," p-1 ");product.setUom(unit());service.save(product);
    assertSame(company,product.getCompany());assertEquals("P-1",product.getSku());
    Item serviceItem=item("SERVICE"," s-1 ");service.save(serviceItem);
    assertNull(serviceItem.getUom());
  }
  @Test void rejectsProductWithoutUnitServiceBarcodeAndDuplicates(){
    TestItemService service=new TestItemService(company(1L,country(1L)));
    assertThrows(IllegalArgumentException.class,()->service.validate(item("PRODUCT","P")));
    Item serviceItem=item("SERVICE","S");serviceItem.setBarcode("123");
    assertThrows(IllegalArgumentException.class,()->service.validate(serviceItem));
    Item product=item("PRODUCT","P");product.setUom(unit());service.skuDuplicate=product;
    assertThrows(IllegalArgumentException.class,()->service.validate(product));
  }
  @Test void validatesCompanyCategoryUnitAndTaxCompatibility(){
    Country ec=country(1L),us=country(2L);Company company=company(1L,ec);
    CatalogValidationService validation=new ValidationStub();
    TestItemService service=new TestItemService(company,validation);
    Item item=item("PRODUCT","P");item.setUom(unit());item.setCategory(category(company(2L,ec),"C"));
    assertThrows(IllegalArgumentException.class,()->service.validate(item));
    item.setCategory(null);TaxCategory tax=new TaxCategory();tax.setCountry(us);tax.setArchived(false);item.setTaxCategory(tax);
    assertThrows(IllegalArgumentException.class,()->service.validate(item));
  }
  @Test void itemHasNoUpperModuleCollections(){
    assertFalse(hasMethod(Item.class,"getPriceListItems"));assertFalse(hasMethod(Item.class,"getSalesInvoiceLines"));
  }
  private static Item item(String type,String sku){Item i=new Item();i.setItemType(type);i.setSku(sku);i.setName("Name");return i;}
  private static UnitOfMeasure unit(){UnitOfMeasure u=new UnitOfMeasure();u.setCode("EA");u.setName("Each");u.setActive(true);u.setArchived(false);return u;}
  private static ItemCategory category(Company c,String code){ItemCategory x=new ItemCategory();x.setCompany(c);x.setCode(code);x.setName(code);x.setActive(true);x.setArchived(false);return x;}
  private static Country country(Long id){Country c=new Country();c.setId(id);c.setArchived(false);return c;}
  private static Company company(Long id,Country country){Company c=new Company();c.setId(id);c.setCountry(country);c.setActive(true);c.setArchived(false);return c;}
  private static boolean hasMethod(Class<?> c,String n){return java.util.Arrays.stream(c.getMethods()).anyMatch(m->m.getName().equals(n));}
  private static class TestUnitService extends UnitOfMeasureServiceImpl{
    UnitOfMeasure duplicate;TestUnitService(){super(null);}
    protected UnitOfMeasure persist(UnitOfMeasure u){return u;}protected UnitOfMeasure findDuplicate(UnitOfMeasure u){return duplicate;}
    protected UnitOfMeasure findArchived(String c){return null;}
    protected boolean hasActiveItems(UnitOfMeasure u){return false;}
  }
  private static class TestCategoryService extends ItemCategoryServiceImpl{
    boolean children,items;TestCategoryService(Company c){super(null,new ActiveStub(c));}
    protected ItemCategory persist(ItemCategory c){return c;}protected ItemCategory findDuplicate(ItemCategory c){return null;}
    protected Company findPersistedCompany(Long id){return null;}protected boolean hasActiveChildren(ItemCategory c){return children;}
    protected boolean hasActiveItems(ItemCategory c){return items;}
  }
  private static class TestItemService extends ItemServiceImpl{
    Item skuDuplicate,barcodeDuplicate;TestItemService(Company c){this(c,new PermissiveValidation());}
    TestItemService(Company c,CatalogValidationService v){super(null,new ActiveStub(c),v);}
    protected Item persist(Item i){return i;}protected Item findSkuDuplicate(Item i){return skuDuplicate;}
    protected Item findBarcodeDuplicate(Item i){return barcodeDuplicate;}protected Company findPersistedCompany(Long id){return null;}
  }
  private static class PermissiveValidation implements CatalogValidationService{
    public void requireActiveCompany(Company c){}public void requireCategoryForCompany(ItemCategory c,Company x){}
    public void requireUsableUnit(UnitOfMeasure u){}public void requireCompatibleTax(TaxCategory t,Company c){}
  }
  private static class ValidationStub extends PermissiveValidation{
    public void requireCategoryForCompany(ItemCategory c,Company x){if(c!=null&&!c.getCompany().getId().equals(x.getId()))throw new IllegalArgumentException();}
    public void requireCompatibleTax(TaxCategory t,Company c){if(t!=null&&(c.getCountry()==null||!c.getCountry().getId().equals(t.getCountry().getId())))throw new IllegalArgumentException();}
  }
  private static class ActiveStub implements ActiveOrganizationService{
    final Company c;ActiveStub(Company c){this.c=c;}public Optional<Company>getActiveCompany(){return Optional.ofNullable(c);}
    public Company requireActiveCompany(){if(c==null)throw new IllegalArgumentException();return c;}
    public Optional<Branch>getActiveBranch(){return Optional.empty();}public Branch requireActiveBranch(){throw new UnsupportedOperationException();}
    public ActiveOrganizationContext getContext(){return null;}public Company setActiveCompany(Company c){return c;}public Company setActiveCompany(Long id){return c;}
    public Branch setActiveBranch(Branch b){return b;}public Branch setActiveBranch(Long id){return null;}public void clearActiveCompany(){}public void clearActiveBranch(){}public void clearContext(){}
    public List<Company>getAvailableCompanies(){return List.of(c);}public List<Branch>getAvailableBranches(){return List.of();}public List<Branch>getAvailableBranches(Company c){return List.of();}
  }
}
