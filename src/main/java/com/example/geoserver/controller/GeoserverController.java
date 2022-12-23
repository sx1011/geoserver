package com.example.geoserver.controller;

import com.example.geoserver.config.GeoServer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("/geoserver")
public class GeoserverController {

    /*@Resource
    private GeoServer geoServer;*/

    @PostMapping
    public boolean geo(String zipFilePath, String storeName, String styleType, String coordinateSystem) {
        boolean b = GeoServer.publishShp(zipFilePath, storeName, styleType, coordinateSystem);
        return b;
    }
}
