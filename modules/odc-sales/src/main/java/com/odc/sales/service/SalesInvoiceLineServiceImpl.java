package com.odc.sales.service;
import com.axelor.db.JPA; import com.axelor.i18n.I18n; import com.google.inject.Inject; import com.google.inject.persist.Transactional;
import com.odc.sales.db.*; import com.odc.sales.db.repo.SalesInvoiceLineRepository; import jakarta.persistence.LockModeType;
import java.math.BigDecimal; import java.util.*;
public class SalesInvoiceLineServiceImpl implements SalesInvoiceLineService {
 private final SalesInvoiceLineRepository repository; private final SalesInvoiceService invoiceService;
 @Inject public SalesInvoiceLineServiceImpl(SalesInvoiceLineRepository r,SalesInvoiceService s){repository=r;invoiceService=s;}
 @Transactional public SalesInvoiceLine save(SalesInvoiceLine l){validate(l);return repository.save(l);}
 public void validate(SalesInvoiceLine l){
  if(l==null||l.getSalesInvoice()==null)throw err("Sales invoice is required."); invoiceService.requireEditable(l.getSalesInvoice());
  if(l.getSequence()==null)l.setSequence(next(l.getSalesInvoice())); if(l.getSequence()<=0)throw err("Line sequence must be positive.");
  if(l.getItem()==null)throw err("Item is required.");
  var item=l.getItem(); if(!same(item.getCompany(),l.getSalesInvoice().getCompany())||Boolean.TRUE.equals(item.getArchived())||!Boolean.TRUE.equals(item.getActive()))throw err("Item must be active and belong to the invoice company.");
  if(l.getQuantity()==null||l.getQuantity().signum()<=0)throw err("Quantity must be greater than zero.");
  if(l.getUnitPrice()==null||l.getUnitPrice().signum()<0)throw err("Unit price cannot be negative.");
  if(!List.of("PRICE_LIST","MANUAL").contains(l.getPriceSource()))throw err("Price source is invalid.");
  l.setItemCodeSnapshot(item.getSku());l.setItemNameSnapshot(item.getName());l.setUom(item.getUom());l.setUomCodeSnapshot(item.getUom()==null?null:item.getUom().getCode());
  if(l.getDescription()==null||l.getDescription().isBlank())l.setDescription(item.getName());
  if(duplicate(l)!=null)throw err("Invoice line sequence already exists."); if(l.getArchived()==null)l.setArchived(false);
 }
 @Transactional public void archive(SalesInvoiceLine l){l=lock(l);invoiceService.requireEditable(l.getSalesInvoice());l.setArchived(true);repository.save(l);}
 @Transactional public SalesInvoiceLine restore(SalesInvoiceLine l){l=lock(l);l.setArchived(false);validate(l);return repository.save(l);}
 public List<SalesInvoiceLine> findActiveLines(SalesInvoice i){return repository.all().filter("self.salesInvoice = :invoice AND self.archived = false").bind("invoice",i).order("sequence").fetch();}
 public List<SalesInvoiceLine> lockActiveLines(SalesInvoice i){var v=findActiveLines(i);v.forEach(x->JPA.em().lock(x,LockModeType.PESSIMISTIC_WRITE));return v;}
 public SalesInvoiceLine persistCalculated(SalesInvoiceLine l){return repository.save(l);}
 protected SalesInvoiceLine duplicate(SalesInvoiceLine l){String f="self.salesInvoice=:i AND self.sequence=:s AND self.archived=false";var q=repository.all().filter(f).bind("i",l.getSalesInvoice()).bind("s",l.getSequence());return q.fetchOne();}
 private int next(SalesInvoice i){return findActiveLines(i).stream().map(SalesInvoiceLine::getSequence).max(Integer::compare).orElse(0)+10;}
 private SalesInvoiceLine lock(SalesInvoiceLine l){if(l==null||l.getId()==null)throw err("Persisted invoice line is required.");return JPA.em().find(SalesInvoiceLine.class,l.getId(),LockModeType.PESSIMISTIC_WRITE);}
 private boolean same(com.axelor.db.Model a,com.axelor.db.Model b){return a==b||a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId());}
 private IllegalArgumentException err(String m){return new IllegalArgumentException(I18n.get(m));}
}
