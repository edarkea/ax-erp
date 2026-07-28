package com.odc.document.module;

import com.axelor.app.AxelorModule;
import com.odc.document.observer.DocumentControlSaveObserver;
import com.odc.document.service.DocumentSequenceService;
import com.odc.document.service.DocumentSequenceServiceImpl;
import com.odc.document.service.DocumentSeriesService;
import com.odc.document.service.DocumentSeriesServiceImpl;
import com.odc.document.service.EmissionConfigurationService;
import com.odc.document.service.EmissionConfigurationServiceImpl;
import com.odc.document.service.UserPointAssignmentService;
import com.odc.document.service.UserPointAssignmentServiceImpl;

public class OdcDocumentControlModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(EmissionConfigurationService.class).to(EmissionConfigurationServiceImpl.class);
    bind(UserPointAssignmentService.class).to(UserPointAssignmentServiceImpl.class);
    bind(DocumentSeriesService.class).to(DocumentSeriesServiceImpl.class);
    bind(DocumentSequenceService.class).to(DocumentSequenceServiceImpl.class);
    bind(DocumentControlSaveObserver.class);
  }
}
