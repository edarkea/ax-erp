package com.odc.common.rpc;

import com.axelor.db.Model;
import com.axelor.events.PreRequest;
import com.axelor.rpc.Context;
import com.axelor.rpc.Request;
import com.axelor.rpc.RequestUtils;
import java.util.Objects;
import java.util.function.Consumer;

public final class RequestEntityUtils {

  private RequestEntityUtils() {}

  public static <T extends Model> void process(
      PreRequest event, Class<T> entityType, Consumer<T> action) {
    Objects.requireNonNull(event, "PreRequest is required.");
    process(event.getRequest(), entityType, action);
  }

  public static <T extends Model> void process(
      Request request, Class<T> entityType, Consumer<T> action) {
    Objects.requireNonNull(request, "Request is required.");
    Objects.requireNonNull(entityType, "Entity type is required.");
    Objects.requireNonNull(action, "Entity action is required.");

    RequestUtils.processRequest(
        request,
        values -> action.accept(new Context(values, entityType).asType(entityType)));
  }
}
