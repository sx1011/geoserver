package com.example.geoserver.utils;

import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;

public class ImprovePostGISDatastore {
    private String dataStoreName;
    private String host;
    private int port = 5432;
    private String user = "postgres";
    private String password;
    private String database;

    public ImprovePostGISDatastore(String dataStoreName, String host, String password, String database) {
        this.dataStoreName = dataStoreName;
        this.host = host;
        this.password = password;
        this.database = database;
    }

    public ImprovePostGISDatastore(String dataStoreName, String host, int port, String user, String password, String database) {
        this.dataStoreName = dataStoreName;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    /**
     * 构建 GSPostGISDatastoreEncoder 对象，并将其返回
     * @return GSPostGISDatastoreEncoder 对象
     */
    public GSPostGISDatastoreEncoder builder () {
        GSPostGISDatastoreEncoder build = new GSPostGISDatastoreEncoder(dataStoreName);
        build.setHost(host);
        build.setPort(port);
        build.setUser(user);
        build.setPassword(password);
        build.setDatabase(database);
        build.setExposePrimaryKeys(true);

        return build;
    }
}

