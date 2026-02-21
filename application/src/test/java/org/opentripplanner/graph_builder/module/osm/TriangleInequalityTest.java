package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.search.intersection_model.ConstantIntersectionTraversalCalculator;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.streetadapter.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.streetadapter.StreetSearchBuilder;
import org.opentripplanner.test.support.ResourceLoader;

/**
 * Verifies the triangle inequality property of shortest paths on a real OSM graph
 * (NYC_small.osm.pbf). For any vertices A, B, C the shortest path must satisfy:
 * <pre>
 *   cost(A → C) ≤ cost(A → B) + cost(B → C)
 * </pre>
 * A violation would indicate a bug in the routing algorithm or the cost function.
 * All reluctance values are set to 1.0 so that weight equals duration, making the
 * cost function monotonically increasing and the inequality straightforward to verify.
 */
public class TriangleInequalityTest {

  private static Graph graph;

  private final IntersectionTraversalCalculator calculator =
    new ConstantIntersectionTraversalCalculator(10.0);

  private Vertex start;
  private Vertex end;

  @BeforeAll
  public static void onlyOnce() {
    graph = new Graph();

    var file = ResourceLoader.of(TriangleInequalityTest.class).file("NYC_small.osm.pbf");
    var provider = new DefaultOsmProvider(file, true);
    var osmModule = OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .build();

    osmModule.buildGraph();
  }

  @BeforeEach
  public void before() {
    start = graph.getVertex(VertexLabel.osm(1919595913));
    end = graph.getVertex(VertexLabel.osm(42448554));
  }

  static Stream<Arguments> streetModes() {
    return Stream.of(
      Arguments.of(StreetMode.WALK),
      Arguments.of(StreetMode.BIKE),
      Arguments.of(StreetMode.CAR)
    );
  }

  @ParameterizedTest(name = "triangle inequality holds for {0}")
  @MethodSource("streetModes")
  void triangleInequalityHolds(StreetMode mode) {
    assertNotNull(start, "Start vertex not found in graph");
    assertNotNull(end, "End vertex not found in graph");

    var request = buildRequest(mode);

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withDominanceFunction(new DominanceFunctions.EarliestArrival())
      .withRequest(request)
      .withFrom(start)
      .withTo(end)
      .withIntersectionTraversalCalculator(calculator)
      .getShortestPathTree();

    var directPath = tree.getPath(end);
    assertNotNull(directPath, "No path found from start to end");

    double directWeight = directPath.getWeight();
    assertTrue(directWeight > 0, "Path weight should be positive");

    List<String> violations = new ArrayList<>();

    for (Vertex intermediate : graph.getVertices()) {
      if (intermediate == start || intermediate == end) {
        continue;
      }

      var startToIntermediate = findPath(request, null, start, intermediate);
      if (startToIntermediate == null) {
        continue;
      }

      var backEdge = startToIntermediate.states.getLast().getBackEdge();
      var intermediateToEnd = findPath(request, backEdge, intermediate, end);
      if (intermediateToEnd == null) {
        continue;
      }

      double viaWeight = startToIntermediate.getWeight() + intermediateToEnd.getWeight();
      double diff = viaWeight - directWeight;
      if (diff < -0.01) {
        violations.add(
          "via %s: direct=%.2f, via=%.2f, diff=%.4f".formatted(
            intermediate,
            directWeight,
            viaWeight,
            diff
          )
        );
      }
    }

    assertTrue(
      violations.isEmpty(),
      "%d triangle inequality violations found:\n%s".formatted(
        violations.size(),
        String.join("\n", violations)
      )
    );
  }

  private StreetSearchRequest buildRequest(StreetMode mode) {
    return StreetSearchRequest.of()
      .withMode(mode)
      .withWalk(walk -> walk.withStairsReluctance(1.0).withSpeed(1.0).withReluctance(1.0))
      .withTurnReluctance(1.0)
      .withCar(car -> car.withReluctance(1.0))
      .withBike(bike -> bike.withSpeed(1.0).withReluctance(1.0))
      .withScooter(scooter -> scooter.withSpeed(1.0).withReluctance(1.0))
      .build();
  }

  private GraphPath<State, Edge, Vertex> findPath(
    StreetSearchRequest request,
    Edge startBackEdge,
    Vertex from,
    Vertex to
  ) {
    return StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withOriginBackEdge(startBackEdge)
      .withRequest(request)
      .withFrom(from)
      .withTo(to)
      .withIntersectionTraversalCalculator(calculator)
      .getShortestPathTree()
      .getPath(to);
  }
}
