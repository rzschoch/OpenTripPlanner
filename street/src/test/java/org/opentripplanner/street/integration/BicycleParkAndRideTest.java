package org.opentripplanner.street.integration;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.StreetVertex;

public class BicycleParkAndRideTest extends ParkAndRideTest {

  private StreetVertex A;
  private StreetVertex B;
  private StreetVertex C;

  /**
   * Bicycle park and ride: to parking 1
   */
  @Test
  public void testBicycleToParking1() {
    assertPath(
      A,
      C,
      StreetMode.BIKE_TO_PARK,
      "null - null (0.00, 0)",
      "BICYCLE - BICYCLE (10.00, 2)",
      "WALK (parked) - VehicleParking (170.00, 62)",
      "WALK (parked) - StreetVehicleParkingLink (170.00, 62)",
      "WALK (parked) - BC street (190.00, 72)"
    );
  }

  /**
   * Bicycle park and ride: to parking 2
   */
  @Test
  public void testBicycleToParking2() {
    assertPath(
      A,
      C,
      StreetMode.BIKE_TO_PARK,
      "null - null (0.00, 0)",
      "BICYCLE - BICYCLE (10.00, 2)",
      "WALK (parked) - VehicleParking (170.00, 62)",
      "WALK (parked) - StreetVehicleParkingLink (170.00, 62)",
      "WALK (parked) - BC street (190.00, 72)"
    );
  }

  @BeforeEach
  protected void setUp() throws Exception {
    // Generate a very simple graph
    //
    //   A <-> B <-> C
    //         ^
    //         |
    //     Parking 1
    //     Parking 2

    graph = modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 47.500, 19.001);
          B = intersection("B", 47.510, 19.001);
          C = intersection("C", 47.520, 19.001);

          biStreet(A, B, 87);
          biStreet(B, C, 87);

          vehicleParking(
            "parking1",
            19.001,
            47.511,
            true,
            false,
            List.of(vehicleParkingEntrance(B, "parking1-entrance", false, true))
          );

          vehicleParking(
            "parking2",
            19.001,
            47.512,
            true,
            false,
            List.of(vehicleParkingEntrance(B, "parking2-entrance", false, true))
          );
        }
      }
    );
  }
}
