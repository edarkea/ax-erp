package com.odc.pricing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odc.catalog.db.Item;
import com.odc.catalog.service.ItemService;
import com.odc.organization.db.Company;
import com.odc.pricing.db.PriceList;
import com.odc.pricing.db.PriceListItem;
import com.odc.reference.db.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceResolverServiceImplTest {
  private Company company;
  private Currency currency;
  private Item item;
  private TestResolver resolver;

  @BeforeEach
  void setUp() {
    company = new Company(); company.setId(1L); company.setActive(true); company.setArchived(false);
    currency = new Currency(); currency.setId(1L); currency.setArchived(false);
    item = new Item(); item.setId(1L); item.setCompany(company);
    item.setActive(true); item.setArchived(false);
    resolver = new TestResolver();
  }

  @Test
  void shouldResolveMostSpecificQuantityRange() {
    resolver.lines.add(line(list(20), null, null, "10"));
    resolver.lines.add(line(list(20), new BigDecimal("5"), new BigDecimal("10"), "8"));
    assertEquals(new BigDecimal("8"),
        resolver.resolve(company, item, currency, LocalDate.now(), new BigDecimal("6"), null).price());
  }

  @Test
  void shouldResolvePriorityAfterSpecificity() {
    resolver.lines.add(line(list(50), null, null, "10"));
    resolver.lines.add(line(list(10), null, null, "9"));
    assertEquals(new BigDecimal("9"),
        resolver.resolve(company, item, currency, LocalDate.now(), BigDecimal.ONE, null).price());
  }

  @Test
  void shouldRejectAmbiguousResult() {
    resolver.lines.add(line(list(10), null, null, "10"));
    resolver.lines.add(line(list(10), null, null, "9"));
    assertThrows(IllegalArgumentException.class,
        () -> resolver.resolve(company, item, currency, LocalDate.now(), BigDecimal.ONE, null));
  }

  @Test
  void shouldRejectExpiredExplicitList() {
    PriceList expired = list(10);
    expired.setValidUntil(LocalDate.now().minusDays(1));
    assertThrows(IllegalArgumentException.class,
        () -> resolver.resolve(company, item, currency, LocalDate.now(), BigDecimal.ONE, expired));
  }

  private PriceList list(int priority) {
    PriceList list = new PriceList();
    list.setId((long) priority); list.setCompany(company); list.setCurrency(currency);
    list.setPriority(priority); list.setActive(true); list.setArchived(false);
    return list;
  }
  private PriceListItem line(
      PriceList list, BigDecimal minimum, BigDecimal maximum, String price) {
    PriceListItem line = new PriceListItem();
    line.setPriceList(list); line.setItem(item); line.setMinimumQuantity(minimum);
    line.setMaximumQuantity(maximum); line.setPrice(new BigDecimal(price));
    line.setActive(true); line.setArchived(false);
    return line;
  }

  private static class TestResolver extends PriceResolverServiceImpl {
    final List<PriceListItem> lines = new ArrayList<>();
    TestResolver() { super(null, new PriceListStub(), new ItemStub()); }
    @Override protected List<PriceListItem> findCandidates(
        Company company, Item item, Currency currency, LocalDate date,
        BigDecimal quantity, PriceList explicitPriceList) {
      return lines;
    }
  }
  private static class PriceListStub implements PriceListService {
    public PriceList save(PriceList value) { return value; }
    public void validate(PriceList value) {}
    public void archive(PriceList value) {}
    public PriceList restore(PriceList value) { return value; }
    public void requireUsable(PriceList value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
  }
  private static class ItemStub implements ItemService {
    public Item save(Item value) { return value; }
    public void validate(Item value) {}
    public void archive(Item value) {}
    public Item restore(Item value) { return value; }
    public void requireUsable(Item value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
  }
}
