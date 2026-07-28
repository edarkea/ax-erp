package com.odc.sales.service;
import com.axelor.auth.AuthUtils;import com.axelor.db.JPA;import com.axelor.i18n.I18n;import com.google.inject.Inject;import com.google.inject.persist.Transactional;
import com.odc.sales.db.SalesInvoice;import com.odc.sales.db.repo.SalesInvoiceRepository;
import com.odc.organization.service.*;import com.odc.party.service.PartyRoleService;import jakarta.persistence.LockModeType;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.*;
public class SalesInvoiceServiceImpl implements SalesInvoiceService{
 private final SalesInvoiceRepository repository;private final ActiveOrganizationService active;private final OrganizationAccessService access;private final PartyRoleService roles;
 @Inject public SalesInvoiceServiceImpl(SalesInvoiceRepository r,ActiveOrganizationService a,OrganizationAccessService x,PartyRoleService p){repository=r;active=a;access=x;roles=p;}
 @Transactional public SalesInvoice save(SalesInvoice i){if(i==null)throw err("Sales invoice is required.");if(i.getId()==null){i.setCompany(active.requireActiveCompany());i.setStatus("DRAFT");if(i.getInvoiceDate()==null)i.setInvoiceDate(LocalDate.now());i.setDocumentNo(null);i.setDocumentSequenceReservation(null);i.setArchived(false);zero(i);}validateDraft(i);return repository.save(i);}
 public void validateDraft(SalesInvoice i){requireEditable(i);validateCommon(i);SalesInvoice old=i.getId()==null?null:repository.find(i.getId());if(old!=null&&(!same(old.getCompany(),i.getCompany())||!Objects.equals(old.getStatus(),i.getStatus())||!Objects.equals(old.getDocumentNo(),i.getDocumentNo())||!same(old.getDocumentSequenceReservation(),i.getDocumentSequenceReservation())))throw err("Protected invoice fields cannot be changed manually.");}
 public void validateCommon(SalesInvoice i){
  if(i==null)throw err("Sales invoice is required.");var c=i.getCompany();if(c==null||Boolean.TRUE.equals(c.getArchived())||!Boolean.TRUE.equals(c.getActive()))throw err("Company must be active.");access.requireCompanyAccess(AuthUtils.getUser(),c);if(!same(c,active.requireActiveCompany()))throw err("Invoice must belong to the active company.");
  if(i.getBranch()==null)throw err("Branch is required.");if(!same(i.getBranch().getCompany(),c)||Boolean.TRUE.equals(i.getBranch().getArchived())||!Boolean.TRUE.equals(i.getBranch().getActive()))throw err("Branch must be active and belong to the invoice company.");access.requireBranchAccess(AuthUtils.getUser(),i.getBranch());
  if(i.getCustomer()==null)throw err("Customer is required.");if(!same(i.getCustomer().getCompany(),c)||Boolean.TRUE.equals(i.getCustomer().getArchived())||!Boolean.TRUE.equals(i.getCustomer().getActive()))throw err("Customer must be active and belong to the invoice company.");roles.requireRole(i.getCustomer(),"CUSTOMER");
  if(i.getInvoiceDate()==null)throw err("Invoice date is required.");if(i.getDueDate()!=null&&i.getDueDate().isBefore(i.getInvoiceDate()))throw err("Due date cannot be before invoice date.");
  if(i.getPriceList()==null)throw err("Price list is required.");if(!same(i.getPriceList().getCompany(),c)||Boolean.TRUE.equals(i.getPriceList().getArchived())||!Boolean.TRUE.equals(i.getPriceList().getActive()))throw err("Price list must be active and belong to the invoice company.");
  i.setCurrency(i.getPriceList().getCurrency());
  if(i.getCurrency()==null||Boolean.TRUE.equals(i.getCurrency().getArchived()))throw err("Currency must be active.");
  if(i.getExchangeRate()==null||i.getExchangeRate().signum()<=0)throw err("Exchange rate must be greater than zero.");
 }
 @Transactional public void archive(SalesInvoice v){SalesInvoice i=lock(v);requireEditable(i);i.setArchived(true);repository.save(i);}
 @Transactional public SalesInvoice restore(SalesInvoice v){SalesInvoice i=lock(v);if(!Boolean.TRUE.equals(i.getArchived())||!"DRAFT".equals(i.getStatus())==false)throw err("Only archived draft invoices can be restored.");i.setArchived(false);validateCommon(i);return repository.save(i);}
 public void requireEditable(SalesInvoice i){requireUsable(i);if(!"DRAFT".equals(i.getStatus()))throw err("Only draft invoices can be modified.");}
 public void requireUsable(SalesInvoice i){if(i==null)throw err("Sales invoice is required.");if(Boolean.TRUE.equals(i.getArchived()))throw err("Sales invoice is archived.");}
 protected SalesInvoice lock(SalesInvoice v){if(v==null||v.getId()==null)throw err("Persisted sales invoice is required.");return JPA.em().find(SalesInvoice.class,v.getId(),LockModeType.PESSIMISTIC_WRITE);}
 protected SalesInvoice persist(SalesInvoice i){return repository.save(i);}
 private void zero(SalesInvoice i){i.setSubtotal(BigDecimal.ZERO);i.setTaxTotal(BigDecimal.ZERO);i.setGrandTotal(BigDecimal.ZERO);}
 private boolean same(com.axelor.db.Model a,com.axelor.db.Model b){return a==b||a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId());}
 private IllegalArgumentException err(String m){return new IllegalArgumentException(I18n.get(m));}
}
