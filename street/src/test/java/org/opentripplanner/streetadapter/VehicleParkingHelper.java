package org.opentripplanner.streetadapter;

import java.util.List;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;

/**
 * Street-module version of VehicleParkingHelper containing only the static linking methods
 * that do not require VertexFactory or Graph dependencies.
 */
public class VehicleParkingHelper {

  public static void linkVehicleParkingEntrances(
    List<VehicleParkingEntranceVertex> vehicleParkingVertices
  ) {
    for (int i = 0; i < vehicleParkingVertices.size(); i++) {
      var currentVertex = vehicleParkingVertices.get(i);
      if (isUsableForParking(currentVertex, currentVertex)) {
        VehicleParkingEdge.createVehicleParkingEdge(currentVertex);
      }
      for (int j = i + 1; j < vehicleParkingVertices.size(); j++) {
        var nextVertex = vehicleParkingVertices.get(j);
        if (isUsableForParking(currentVertex, nextVertex)) {
          VehicleParkingEdge.createVehicleParkingEdge(currentVertex, nextVertex);
          VehicleParkingEdge.createVehicleParkingEdge(nextVertex, currentVertex);
        }
      }
    }
  }

  public static void linkToGraph(VehicleParkingEntranceVertex vehicleParkingEntrance) {
    StreetVehicleParkingLink.createStreetVehicleParkingLink(
      vehicleParkingEntrance,
      vehicleParkingEntrance.getParkingEntrance().getVertex()
    );
    StreetVehicleParkingLink.createStreetVehicleParkingLink(
      vehicleParkingEntrance.getParkingEntrance().getVertex(),
      vehicleParkingEntrance
    );
    vehicleParkingEntrance.getParkingEntrance().clearVertex();
  }

  private static boolean isUsableForParking(
    VehicleParkingEntranceVertex from,
    VehicleParkingEntranceVertex to
  ) {
    var usableForBikeParking =
      from.getVehicleParking().hasBicyclePlaces() &&
      from.isWalkAccessible() &&
      to.isWalkAccessible();

    var usableForCarParking =
      from.getVehicleParking().hasAnyCarPlaces() &&
      ((from.isCarAccessible() && to.isWalkAccessible()) ||
        (from.isWalkAccessible() && to.isCarAccessible()));

    return usableForBikeParking || usableForCarParking;
  }
}
