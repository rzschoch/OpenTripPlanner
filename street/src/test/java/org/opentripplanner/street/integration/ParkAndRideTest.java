package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.filter.ParkingFilter;
import org.opentripplanner.street.search.request.filter.ParkingSelect;
import org.opentripplanner.streetadapter.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.streetadapter.StreetSearchBuilder;

public abstract class ParkAndRideTest extends GraphRoutingTest {

  protected Graph graph;

  protected void assertPathWithParking(
    StreetVertex fromVertex,
    StreetVertex toVertex,
    StreetMode streetMode,
    Set<String> bannedTags,
    Set<String> requiredTags
  ) {
    assertNotEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        false
      )
    );
    assertNotEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        true
      )
    );
  }

  protected void assertNoPathWithParking(
    StreetVertex fromVertex,
    StreetVertex toVertex,
    StreetMode streetMode,
    Set<String> bannedTags,
    Set<String> requiredTags
  ) {
    assertEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        false
      )
    );
    assertEquals(
      List.of(),
      runStreetSearchAndCreateDescriptor(
        fromVertex,
        toVertex,
        streetMode,
        false,
        bannedTags,
        requiredTags,
        true
      )
    );
  }

  protected void assertEmptyPath(Vertex fromVertex, Vertex toVertex, StreetMode streetMode) {
    assertPath(fromVertex, toVertex, streetMode, false, List.of(), List.of());
  }

  protected void assertEmptyPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible
  ) {
    assertPath(fromVertex, toVertex, streetMode, requireWheelChairAccessible, List.of(), List.of());
  }

  protected void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    String... descriptor
  ) {
    assertPath(fromVertex, toVertex, streetMode, false, List.of(descriptor), List.of(descriptor));
  }

  protected void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible,
    String... descriptor
  ) {
    assertPath(
      fromVertex,
      toVertex,
      streetMode,
      requireWheelChairAccessible,
      List.of(descriptor),
      List.of(descriptor)
    );
  }

  protected List<String> runStreetSearchAndCreateDescriptor(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible,
    Set<String> bannedTags,
    Set<String> requiredTags,
    boolean arriveBy
  ) {
    var parkingFilter = new ParkingFilter(
      bannedTags.isEmpty() ? List.of() : List.of(new ParkingSelect.TagsSelect(bannedTags)),
      requiredTags.isEmpty() ? List.of() : List.of(new ParkingSelect.TagsSelect(requiredTags))
    );

    var streetSearchRequest = StreetSearchRequest.of()
      .withFrom(new Coordinate(fromVertex.getLon(), fromVertex.getLat()))
      .withTo(new Coordinate(toVertex.getLon(), toVertex.getLat()))
      .withBike(b ->
        b.withParking(parking -> {
          parking.withFilter(parkingFilter);
          parking.withCost(Cost.costOfSeconds(120));
          parking.withTime(Duration.ofSeconds(60));
        })
      )
      .withCar(c ->
        c.withParking(parking -> {
          parking.withFilter(parkingFilter);
          parking.withCost(Cost.costOfSeconds(240));
          parking.withTime(Duration.ofSeconds(180));
        })
      )
      .withWheelchairEnabled(requireWheelChairAccessible)
      .withArriveBy(arriveBy)
      .withMode(streetMode)
      .build();

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(streetSearchRequest)
      .withFrom(fromVertex)
      .withTo(toVertex)
      .getShortestPathTree();

    var path = tree.getPath(arriveBy ? fromVertex : toVertex);

    if (path == null) {
      return List.of();
    }

    return path.states
      .stream()
      .map(s ->
        String.format(
          Locale.ROOT,
          "%s%s - %s (%,.2f, %d)",
          s.getBackMode(),
          s.isVehicleParked() ? " (parked)" : "",
          s.getBackEdge() != null ? s.getBackEdge().getDefaultName() : null,
          s.getWeight(),
          s.getElapsedTimeSeconds()
        )
      )
      .collect(Collectors.toList());
  }

  private void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean requireWheelChairAccessible,
    List<String> departAtDescriptor,
    List<String> arriveByDescriptor
  ) {
    var departAt = runStreetSearchAndCreateDescriptor(
      fromVertex,
      toVertex,
      streetMode,
      requireWheelChairAccessible,
      Set.of(),
      Set.of(),
      false
    );
    var arriveBy = runStreetSearchAndCreateDescriptor(
      fromVertex,
      toVertex,
      streetMode,
      requireWheelChairAccessible,
      Set.of(),
      Set.of(),
      true
    );

    assertEquals(departAtDescriptor, departAt, "departAt path");
    assertEquals(arriveByDescriptor, arriveBy, "arriveBy path");
  }
}
