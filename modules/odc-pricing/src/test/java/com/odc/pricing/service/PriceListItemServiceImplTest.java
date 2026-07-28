package com.odc.pricing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.catalog.db.Item;
import com.odc.catalog.service.ItemService;
import com.odc.organization.db.Company;
import com.odc.pricing.db.PriceList;
import com.odc.pricing.db.PriceListItem;
import com.odc.reference.db.Currency;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceListItemServiceImplTest {

  private Company company;
  private PriceList priceList;
  private TestPriceListItemService service;

  @BeforeEach
  void setUp() {
    company = company(1L);
    priceList = priceList(1L, company, false);
    service = new TestPriceListItemService();
  }

  @Test
  void shouldCreateValidLineAndAcceptZeroPrice() {
    PriceListItem standard = line(priceList, item(1L, company, false), new BigDecimal("10.2500"));
    PriceListItem zero = line(priceList, item(2L, company, false), BigDecimal.ZERO);

    assertEquals(new BigDecimal("10.2500"), service.save(standard).getPrice());
    assertEquals(BigDecimal.ZERO, service.save(zero).getPrice());
    assertTrue(standard.getActive());
    assertFalse(standard.getArchived());
  }

  @Test
  void shouldRejectNegativePrice() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.validate(
                line(priceList, item(1L, company, false), new BigDecimal("-0.0001"))));
  }

  @Test
  void shouldRejectMissingListOrItem() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(line(null, item(1L, company, false), BigDecimal.ONE)));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(line(priceList, null, BigDecimal.ONE)));
  }

  @Test
  void shouldRejectItemFromAnotherCompany() {
    PriceListItem line = line(priceList, item(1L, company(2L), false), BigDecimal.ONE);

    assertThrows(IllegalArgumentException.class, () -> service.validate(line));
  }

  @Test
  void shouldRejectArchivedListOrItem() {
    priceList.setArchived(true);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(line(priceList, item(1L, company, false), BigDecimal.ONE)));

    priceList.setArchived(false);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(line(priceList, item(1L, company, true), BigDecimal.ONE)));
  }

  @Test
  void shouldRejectDuplicateInsideSameList() {
    PriceListItem line = line(priceList, item(1L, company, false), BigDecimal.ONE);
    service.duplicate = line(priceList, line.getItem(), BigDecimal.TEN);

    assertThrows(IllegalArgumentException.class, () -> service.validate(line));
  }

  @Test
  void shouldAllowSameItemInAnotherList() {
    PriceListItem line =
        line(priceList(2L, company, false), item(1L, company, false), BigDecimal.ONE);
    service.duplicate =
        line(priceList(1L, company, false), line.getItem(), BigDecimal.TEN);

    service.validate(line);
  }

  @Test
  void shouldArchiveWithoutPhysicalRemovalAndRestoreWithValidation() {
    PriceListItem line = line(priceList, item(1L, company, false), BigDecimal.ONE);
    line.setId(10L);
    service.archive(line);
    assertTrue(line.getArchived());
    assertFalse(line.getActive());

    service.duplicate = line(priceList, line.getItem(), BigDecimal.TEN);
    assertThrows(IllegalArgumentException.class, () -> service.restore(line));
  }

  @Test
  void shouldRejectChangingListOnExistingLine() {
    PriceListItem line =
        line(priceList(2L, company, false), item(1L, company, false), BigDecimal.ONE);
    line.setId(10L);
    service.persistedPriceList = priceList;

    assertThrows(IllegalArgumentException.class, () -> service.validate(line));
  }

  @Test
  void shouldNotContainCompanyField() {
    assertNull(
        java.util.Arrays.stream(PriceListItem.class.getMethods())
            .filter(method -> method.getName().equals("getCompany"))
            .findFirst()
            .orElse(null));
  }

  private static PriceListItem line(PriceList list, Item item, BigDecimal price) {
    PriceListItem line = new PriceListItem();
    line.setPriceList(list);
    line.setItem(item);
    line.setPrice(price);
    return line;
  }

  private static PriceList priceList(Long id, Company company, boolean archived) {
    PriceList list = new PriceList();
    list.setId(id);
    list.setCompany(company);
    list.setName("Retail");
    list.setCurrency(new Currency());
    list.setActive(true);
    list.setArchived(archived);
    return list;
  }

  private static Item item(Long id, Company company, boolean archived) {
    Item item = new Item();
    item.setId(id);
    item.setCompany(company);
    item.setSku("ITEM-" + id);
    item.setName("Item " + id);
    item.setActive(true);
    item.setArchived(archived);
    return item;
  }

  private static Company company(Long id) {
    Company company = new Company();
    company.setId(id);
    company.setActive(true);
    company.setArchived(false);
    return company;
  }

  private static class TestPriceListItemService extends PriceListItemServiceImpl {

    private PriceListItem duplicate;
    private PriceList persistedPriceList;

    TestPriceListItemService() {
      super(null, new PriceListServiceStub(), new ItemServiceStub());
    }

    @Override
    protected PriceListItem findDuplicate(PriceListItem line) {
      if (duplicate == null
          || duplicate.getPriceList() == null
          || line.getPriceList() == null
          || !duplicate.getPriceList().getId().equals(line.getPriceList().getId())) {
        return null;
      }
      return duplicate;
    }

    @Override
    protected PriceList findPersistedPriceList(Long id) {
      return persistedPriceList;
    }

    @Override
    protected PriceListItem persist(PriceListItem line) {
      return line;
    }
  }

  private static class PriceListServiceStub implements PriceListService {
    @Override public PriceList save(PriceList list) { return list; }
    @Override public void validate(PriceList list) {}
    @Override public void archive(PriceList list) {}
    @Override public PriceList restore(PriceList list) { return list; }
    @Override
    public void requireUsable(PriceList list) {
      if (list == null || Boolean.TRUE.equals(list.getArchived())
          || !Boolean.TRUE.equals(list.getActive())) {
        throw new IllegalArgumentException("Unavailable price list");
      }
    }
  }

  private static class ItemServiceStub implements ItemService {
    @Override public Item save(Item item) { return item; }
    @Override public void validate(Item item) {}
    @Override public void archive(Item item) {}
    @Override public Item restore(Item item) { return item; }
    @Override
    public void requireUsable(Item item) {
      if (item == null || Boolean.TRUE.equals(item.getArchived())
          || !Boolean.TRUE.equals(item.getActive())) {
        throw new IllegalArgumentException("Unavailable item");
      }
    }
  }
}
