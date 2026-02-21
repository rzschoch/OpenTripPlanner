package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.intersection_model.ConstantIntersectionTraversalCalculator;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.streetadapter.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.streetadapter.StreetSearchBuilder;

/**
 * Verifies the triangle inequality property of shortest paths on a simple in-memory
 * graph. For any vertices A, B, C the shortest path must satisfy:
 * <pre>
 *   cost(A → C) ≤ cost(A → B) + cost(B → C)
 * </pre>
 * A violation would indicate a bug in the routing algorithm or the cost function.
 * All reluctance values are set to 1.0 so that weight equals duration, making the
 * cost function monotonically increasing and the inequality straightforward to verify.
 * <p>
 * Graph layout (all edges are bidirectional):
 * <pre>
 *       A ---100--- B
 *       |         / |
 *      50      80  100
 *       |    /      |
 *       C ---120--- D ---90--- E
 * </pre>
 */
public class TriangleInequalityTest {

  private final IntersectionTraversalCalculator calculator =
    new ConstantIntersectionTraversalCalculator(0.0);

  private StreetVertex A, B, C, D, E;
  private List<Vertex> allVertices;

  @BeforeEach
  public void setUp() {
    A = StreetModelForTest.intersectionVertex("A", 0.00002, 0.00000);
    B = StreetModelForTest.intersectionVertex("B", 0.00002, 0.00002);
    C = StreetModelForTest.intersectionVertex("C", 0.00000, 0.00000);
    D = StreetModelForTest.intersectionVertex("D", 0.00000, 0.00002);
    E = StreetModelForTest.intersectionVertex("E", 0.00000, 0.00004);

    biEdge(A, B, 100);
    biEdge(A, C, 50);
    biEdge(B, C, 80);
    biEdge(B, D, 100);
    biEdge(C, D, 120);
    biEdge(D, E, 90);

    allVertices = List.of(A, B, C, D, E);
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
    var request = buildRequest(mode);

    for (Vertex start : allVertices) {
      for (Vertex end : allVertices) {
        if (start == end) {
          continue;
        }

        var directPath = findPath(request, start, end);
        assertNotNull(
          directPath,
          "No path found from %s to %s".formatted(start.getDefaultName(), end.getDefaultName())
        );

        double directWeight = directPath.getWeight();
        assertTrue(directWeight > 0, "Path weight should be positive");

        List<String> violations = new ArrayList<>();
        for (Vertex intermediate : allVertices) {
          if (intermediate == start || intermediate == end) {
            continue;
          }

          var startToIntermediate = findPath(request, start, intermediate);
          if (startToIntermediate == null) {
            continue;
          }

          var intermediateToEnd = findPath(request, intermediate, end);
          if (intermediateToEnd == null) {
            continue;
          }

          double viaWeight =
            startToIntermediate.getWeight() + intermediateToEnd.getWeight();
          double diff = viaWeight - directWeight;
          if (diff < -0.01) {
            violations.add(
              "via %s: direct=%.2f, via=%.2f, diff=%.4f".formatted(
                intermediate.getDefaultName(),
                directWeight,
                viaWeight,
                diff
              )
            );
          }
        }

        assertTrue(
          violations.isEmpty(),
          "%s→%s: %d triangle inequality violations:\n%s".formatted(
            start.getDefaultName(),
            end.getDefaultName(),
            violations.size(),
            String.join("\n", violations)
          )
        );
      }
    }
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
    Vertex from,
    Vertex to
  ) {
    return StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(from)
      .withTo(to)
      .withIntersectionTraversalCalculator(calculator)
      .getShortestPathTree()
      .getPath(to);
  }

  private static void biEdge(StreetVertex a, StreetVertex b, double meters) {
    edge(a, b, meters);
    edge(b, a, meters);
  }

  private static void edge(StreetVertex from, StreetVertex to, double meters) {
    new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withName("%s→%s".formatted(from.getDefaultName(), to.getDefaultName()))
      .withMeterLength(meters)
      .withPermission(StreetTraversalPermission.ALL)
      .withCarSpeed(1.0f)
      .buildAndConnect();
  }
}
