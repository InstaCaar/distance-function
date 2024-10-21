package functions;

class LocationRequest {
    private double latitude;
    private double longitude;

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public boolean isValid() {
        return latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}