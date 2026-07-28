package com.odc.document.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.document.db.DocumentSequenceReservation;
import com.odc.document.db.DocumentSeries;
import com.odc.document.db.EmissionEstablishment;
import com.odc.document.db.PointOfSale;
import com.odc.document.db.repo.DocumentSequenceReservationRepository;
import com.odc.document.service.DocumentSequenceService;

public class DocumentSequenceController {
  public void configurePointDomain(ActionRequest request, ActionResponse response) {
    DocumentSeries series = request.getContext().asType(DocumentSeries.class);
    EmissionEstablishment establishment = series.getEmissionEstablishment();
    response.setAttr(
        "pointOfSale",
        "domain",
        establishment == null || establishment.getId() == null
            ? "self.id = 0"
            : "self.archived = false AND self.active = true "
                + "AND self.emissionEstablishment.id = "
                + establishment.getId());

    PointOfSale selected = series.getPointOfSale();
    if (selected != null
        && (establishment == null
            || establishment.getId() == null
            || selected.getEmissionEstablishment() == null
            || !establishment.getId().equals(selected.getEmissionEstablishment().getId()))) {
      response.setValue("pointOfSale", null);
    }
  }

  public void voidReservation(ActionRequest request, ActionResponse response) {
    DocumentSequenceReservation context =
        request.getContext().asType(DocumentSequenceReservation.class);
    if (context.getId() == null) {
      throw new IllegalArgumentException("A persisted reservation is required.");
    }
    DocumentSequenceReservation reservation =
        Beans.get(DocumentSequenceReservationRepository.class).find(context.getId());
    Beans.get(DocumentSequenceService.class)
        .voidReservation(reservation, context.getVoidReason());
    response.setReload(true);
  }
}
