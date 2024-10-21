package functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.RoadsApi;
import com.google.maps.model.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class Function implements HttpFunction {
    private final GeoApiContext geoApiContext;
    private final Gson gson;

    public Function() {
        this.geoApiContext = new GeoApiContext.Builder()
                .apiKey(System.getenv("GOOGLE_MAPS_API_KEY"))
                .build();
        this.gson = new Gson();
    }

    @Override
    public void service(HttpRequest request, HttpResponse response)
            throws IOException {
        response.setContentType("application/json");
        BufferedWriter writer = response.getWriter();

        try {
            String requestBody = request.getReader().lines()
                    .collect(Collectors.joining("\n"));
            LocationRequest locationRequest = gson.fromJson(requestBody, LocationRequest.class);

            if (!locationRequest.isValid()) {
                writeError(writer, "Invalid coordinates provided", 400);
                return;
            }

            LatLng location = new LatLng(
                    locationRequest.getLatitude(),
                    locationRequest.getLongitude()
            );

            SnappedPoint[] snappedPoints = RoadsApi.snapToRoads(geoApiContext, false, location)
                    .await();

            if (snappedPoints == null || snappedPoints.length == 0) {
                writeError(writer, "No nearby roads found", 404);
                return;
            }

            SnappedPoint nearestPoint = snappedPoints[0];

            double distance = calculateDistance(
                    location.lat, location.lng,
                    nearestPoint.location.lat, nearestPoint.location.lng
            );

            GeocodingResult[] geocodingResults = GeocodingApi.reverseGeocode(geoApiContext,
                            new LatLng(nearestPoint.location.lat, nearestPoint.location.lng))
                    .await();

            RoadDetails roadDetails = extractRoadDetails(geocodingResults);

            RoadResponse roadResponse = new RoadResponse(
                    distance,
                    "meters",
                    roadDetails,
                    nearestPoint.location.lat,
                    nearestPoint.location.lng
            );

            writer.write(gson.toJson(roadResponse));

        } catch (Exception e) {
            writeError(writer, "Internal server error: " + e.getMessage(), 500);
        }
    }

    private double calculateDistance(double lat1, double lon1,
                                     double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private RoadDetails extractRoadDetails(GeocodingResult[] results) {
        if (results == null || results.length == 0) {
            return new RoadDetails();
        }

        String roadName = null;
        String roadType = null;

        for (AddressComponent component : results[0].addressComponents) {
            if (containsType(component.types, AddressComponentType.ROUTE)) {
                roadName = component.longName;
            }
            if (containsType(component.types, AddressComponentType.STREET_ADDRESS)) {
                roadType = component.types[0].toString();
            }
        }

        return new RoadDetails(roadName, roadType);
    }

    private boolean containsType(AddressComponentType[] types,
                                 AddressComponentType targetType) {
        for (AddressComponentType type : types) {
            if (type == targetType) return true;
        }
        return false;
    }

    private void writeError(BufferedWriter writer, String message, int statusCode)
            throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        writer.write(error.toString());
    }
}
