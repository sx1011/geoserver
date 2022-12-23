package com.example.geoserver.utils;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;

public class ImproveGeoServerPublisher extends GeoServerRESTPublisher {

    private String restURL;
    private String username;
    private String password;

    public ImproveGeoServerPublisher(String restURL, String username, String password) {
        super(restURL, username, password);

        this.restURL = restURL;
        this.username = username;
        this.password = password;
    }

    public String getRestURL() {
        return restURL;
    }

    public void setRestURL(String restURL) {
        this.restURL = restURL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
