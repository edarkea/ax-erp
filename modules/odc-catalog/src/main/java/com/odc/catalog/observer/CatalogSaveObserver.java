package com.odc.catalog.observer;
import static com.odc.common.rpc.RequestEntityUtils.process;
import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.catalog.db.*;
import com.odc.catalog.service.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
public class CatalogSaveObserver {
  private final UnitOfMeasureService units;private final ItemCategoryService categories;private final ItemService items;
  @Inject public CatalogSaveObserver(UnitOfMeasureService u,ItemCategoryService c,ItemService i){units=u;categories=c;items=i;}
  public void unit(@Observes @Named(RequestEvent.SAVE) @EntityType(UnitOfMeasure.class) PreRequest e){process(e,UnitOfMeasure.class,units::validate);}
  public void category(@Observes @Named(RequestEvent.SAVE) @EntityType(ItemCategory.class) PreRequest e){process(e,ItemCategory.class,categories::validate);}
  public void item(@Observes @Named(RequestEvent.SAVE) @EntityType(Item.class) PreRequest e){process(e,Item.class,items::validate);}
}
