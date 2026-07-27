package com.odc.reference.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.events.PreRequest;
import com.axelor.rpc.Request;
import com.odc.common.rpc.RequestEntityUtils;
import com.odc.reference.db.Country;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestEntityUtilsTest {

  @Test
  void shouldProcessRequestDataAsFallback() {
    Request request = new Request();
    Map<String, Object> values = new HashMap<>();
    values.put("code", "EC");
    request.setData(values);

    RequestEntityUtils.process(
        request, Country.class, country -> country.setCode(country.getCode() + "-OK"));

    assertEquals("EC-OK", values.get("code"));
  }

  @Test
  void shouldProcessOnlyMapRecords() {
    Request request = new Request();
    Map<String, Object> first = new HashMap<>();
    first.put("code", "EC");
    Map<String, Object> second = new HashMap<>();
    second.put("code", "AR");
    request.setRecords(new ArrayList<>(List.of(first, "ignored", second)));
    List<String> processed = new ArrayList<>();

    RequestEntityUtils.process(
        request, Country.class, country -> processed.add(country.getCode()));

    assertEquals(List.of("EC", "AR"), processed);
  }

  @Test
  void shouldProcessPreRequest() {
    Request request = new Request();
    request.setData(new HashMap<>(Map.of("code", "EC")));
    PreRequest event = new PreRequest(this, request);
    List<String> processed = new ArrayList<>();

    RequestEntityUtils.process(
        event, Country.class, country -> processed.add(country.getCode()));

    assertEquals(List.of("EC"), processed);
  }

  @Test
  void shouldRejectNullArguments() {
    assertThrows(
        NullPointerException.class,
        () -> RequestEntityUtils.process((Request) null, Country.class, country -> {}));
    assertThrows(
        NullPointerException.class,
        () -> RequestEntityUtils.process(new Request(), null, country -> {}));
    assertThrows(
        NullPointerException.class,
        () -> RequestEntityUtils.process(new Request(), Country.class, null));
  }
}
