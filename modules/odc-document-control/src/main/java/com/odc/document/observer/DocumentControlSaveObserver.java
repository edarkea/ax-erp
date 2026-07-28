package com.odc.document.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.document.db.DocumentSeries;
import com.odc.document.db.EmissionEstablishment;
import com.odc.document.db.PointOfSale;
import com.odc.document.db.UserPointAssignment;
import com.odc.document.service.DocumentSeriesService;
import com.odc.document.service.EmissionConfigurationService;
import com.odc.document.service.UserPointAssignmentService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class DocumentControlSaveObserver {
  private final EmissionConfigurationService configurationService;
  private final UserPointAssignmentService assignmentService;
  private final DocumentSeriesService seriesService;

  @Inject
  public DocumentControlSaveObserver(
      EmissionConfigurationService configurationService,
      UserPointAssignmentService assignmentService,
      DocumentSeriesService seriesService) {
    this.configurationService = configurationService;
    this.assignmentService = assignmentService;
    this.seriesService = seriesService;
  }

  public void onEstablishment(
      @Observes @Named(RequestEvent.SAVE) @EntityType(EmissionEstablishment.class)
          PreRequest event) {
    process(event, EmissionEstablishment.class, configurationService::validate);
  }
  public void onPoint(
      @Observes @Named(RequestEvent.SAVE) @EntityType(PointOfSale.class) PreRequest event) {
    process(event, PointOfSale.class, configurationService::validate);
  }
  public void onAssignment(
      @Observes @Named(RequestEvent.SAVE) @EntityType(UserPointAssignment.class)
          PreRequest event) {
    process(event, UserPointAssignment.class, assignmentService::validate);
  }
  public void onSeries(
      @Observes @Named(RequestEvent.SAVE) @EntityType(DocumentSeries.class) PreRequest event) {
    process(event, DocumentSeries.class, seriesService::validate);
  }
}
