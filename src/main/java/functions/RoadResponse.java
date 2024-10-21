package functions;

class RoadResponse {
    private final double distance;
    private final String unit;
    private final RoadDetails roadDetails;
    private final Location nearestLocation;

    public RoadResponse(double distance, String unit, RoadDetails roadDetails,
                        double latitude, double longitude) {
        this.distance = distance;
        this.unit = unit;
        this.roadDetails = roadDetails;
        this.nearestLocation = new Location(latitude, longitude);
    }

    private static class Location {
        private final double latitude;
        private final double longitude;

        public Location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
