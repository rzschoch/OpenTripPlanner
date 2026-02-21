package org.opentripplanner.street.integration;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;

public class CarParkAndRideTest extends ParkAndRideTest {

  private StreetVertex A;
  private StreetVertex B;
  private StreetVertex C;
  private StreetVertex D;

  /**
   * Car park and ride: to parking 1
   */
  @Test
  public void testCarToParking1() {
    assertPath(
      A,
      D,
      StreetMode.CAR_TO_PARK,
      "null - null (0.00, 0)",
      "CAR - CAR_TO_PARK (3.48, 3)",
      "WALK (parked) - VehicleParking (243.48, 183)",
      "WALK (parked) - StreetVehicleParkingLink (243.48, 183)",
      "WALK (parked) - BC street (263.48, 193)",
      "WALK (parked) - CD street (283.48, 203)"
    );
  }

  /**
   * Car park and ride: to parking 2
   */
  @Test
  public void testCarToParking2() {
    assertPath(
      A,
      D,
      StreetMode.CAR_TO_PARK,
      "null - null (0.00, 0)",
      "CAR - CAR_TO_PARK (3.48, 3)",
      "WALK (parked) - VehicleParking (243.48, 183)",
      "WALK (parked) - StreetVehicleParkingLink (243.48, 183)",
      "WALK (parked) - BC street (263.48, 193)",
      "WALK (parked) - CD street (283.48, 203)"
    );
  }

  /**
   * Car park and ride: no path if only pedestrian allowed
   */
  @Test
  public void testNoPathWalkOnly() {
    assertEmptyPath(A, D, StreetMode.CAR_TO_PARK);
  }

  @Test
  public void testNoPathWalkOnlyWithWheelchairAccessible() {
    assertEmptyPath(A, D, StreetMode.CAR_TO_PARK, true);
  }

  @Test
  public void testWheelchairAccessibleCarParking() {
    assertPath(
      A,
      D,
      StreetMode.CAR_TO_PARK,
      true,
      "null - null (0.00, 0)",
      "CAR - CAR_TO_PARK (3.48, 3)",
      "WALK (parked) - VehicleParking (243.48, 183)",
      "WALK (parked) - StreetVehicleParkingLink (243.48, 183)",
      "WALK (parked) - BC street (263.48, 193)",
      "WALK (parked) - CD street (283.48, 203)"
    );
  }

  @Test
  public void testFilteringByRequiredTag() {
    assertPathWithParking(A, D, StreetMode.CAR_TO_PARK, Set.of(), Set.of("indoor"));
    assertNoPathWithParking(A, D, StreetMode.CAR_TO_PARK, Set.of(), Set.of("outdoor"));
  }

  @Test
  public void testFilteringByBannedTag() {
    assertPathWithParking(A, D, StreetMode.CAR_TO_PARK, Set.of("outdoor"), Set.of());
    assertNoPathWithParking(A, D, StreetMode.CAR_TO_PARK, Set.of("indoor"), Set.of());
  }

  @BeforeEach
  protected void setUp() throws Exception {
    // Generate a very simple graph
    //
    //   A <-> B <-> C <-> D
    //         ^
    //         |
    //     Parking 1 (wheelchair accessible, tagged 'indoor')
    //     Parking 2

    graph = modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 47.500, 19.001);
          B = intersection("B", 47.510, 19.001);
          C = intersection("C", 47.520, 19.001);
          D = intersection("D", 47.530, 19.001);

          street(A, B, 87, StreetTraversalPermission.CAR, 10);
          biStreet(B, C, 87);
          biStreet(C, D, 87);

          vehicleParking(
            "parking1",
            19.001,
            47.511,
            false,
            true,
            true,
            List.of(vehicleParkingEntrance(B, "parking1-entrance", true, true)),
            "indoor"
          );

          vehicleParking(
            "parking2",
            19.001,
            47.512,
            false,
            true,
            List.of(vehicleParkingEntrance(B, "parking2-entrance", true, true))
          );
        }
      }
    );
  }
}
