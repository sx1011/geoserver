package com.example.geoserver.geoserver;

//import com.zykj.didiao.common.util.common.FileUtil;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTDataStore;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSGeoTIFFDatastoreEncoder;
import it.geosolutions.geoserver.rest.manager.GeoServerRESTStoreManager;
import org.apache.commons.httpclient.NameValuePair;
//import org.apache.http.HttpResponse;
//import org.apache.http.StatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

//import static org.toilelibre.libe.curl.Curl.curl;

@Component
public class GeoServer {

    private static Logger logger = LoggerFactory.getLogger(GeoServer.class);

    /**
     * geoServer配置
     */
    private static String url;
    private static String geoUsername;
    private static String geoPassword;
    //待发布矢量图层的工作区间
    public static String shpWorkspace;
    //待发布影像图层的工作空间
    public static String imageWorkspace;

    public static String stylePath;

    @Value("${geoserver.url}")
    public void setUrl(String url) {
        GeoServer.url = url;
    }

    @Value("${geoserver.username}")
    public void setGeoUsername(String geoUsername) {
        GeoServer.geoUsername = geoUsername;
    }

    @Value("${geoserver.password}")
    public void setGeoPassword(String geoPassword) {
        GeoServer.geoPassword = geoPassword;
    }

    @Value("${geoserver.shpworkspace}")
    public void setShpWorkspace(String shpWorkspace) {
        GeoServer.shpWorkspace = shpWorkspace;
    }

    @Value("${geoserver.imageworkspace}")
    public void setImageWorkspace(String imageWorkspace) {
        GeoServer.imageWorkspace = imageWorkspace;
    }

    @Value("${localdir.style}")
    public void setStylePath(String stylePath) {
        GeoServer.stylePath = stylePath;
    }

    /**
     * 判断工作区（workspace）是否存在，不存在则创建
     */
    public static void judgeWorkSpace(String workspace) throws MalformedURLException {
        URL u = new URL(url);
        GeoServerRESTManager manager = new GeoServerRESTManager(u, geoUsername, geoPassword);
        GeoServerRESTPublisher publisher = manager.getPublisher();
        List<String> workspaces = manager.getReader().getWorkspaceNames();
        if (!workspaces.contains(workspace)) {
            boolean createWorkspace = publisher.createWorkspace(workspace);
            logger.info("create workspace : " + createWorkspace);
        } else {
            logger.info("workspace已经存在了,workspace :" + workspace);
        }
    }

    /**
     * 判断存储是否存在
     *
     * @param store 存储名
     * @return boolean
     */
    public static boolean shpJudgeDatabase(String store) {
        try {
            URL u = new URL(url);
            GeoServerRESTManager manager = new GeoServerRESTManager(u, geoUsername, geoPassword);
            RESTDataStore restStore = manager.getReader().getDatastore(shpWorkspace, store);
            if (restStore == null) {
                logger.info("数据存储不存在，可以创建！");
                return true;
            } else {
                logger.info("数据存储已经存在了,store:" + store);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 直接发布tiff影像到geoServer
     * 将遥感影像经过http传递过去对应的文件及
     *
     * @param storeName 数据存储名/图层名
     * @param fileUrl   本地文件地址
     */
    public static boolean releaseTiff(String storeName, String fileUrl) throws MalformedURLException, FileNotFoundException {
        URL u = new URL(url);
        //创建一个geoServer rest连接对象
        GeoServerRESTManager manager = new GeoServerRESTManager(u, geoUsername, geoPassword);
        //判断数据存储桶是否存在
        RESTDataStore restStore = manager.getReader().getDatastore(imageWorkspace, storeName);
        //如果不存在就创建一个数据存储，并发布
        if (restStore == null) {
            GSGeoTIFFDatastoreEncoder gsGeoTIFFDatastoreEncoder = new GSGeoTIFFDatastoreEncoder(storeName);
            gsGeoTIFFDatastoreEncoder.setWorkspaceName(imageWorkspace);
            //不确定是否有用
//            gsGeoTIFFDatastoreEncoder.setUrl(new URL("file:" + fileUrl));
//            gsGeoTIFFDatastoreEncoder.setUrl(new URL("file:" + "test.tif"));
            boolean createStore = manager.getStoreManager().create(imageWorkspace, gsGeoTIFFDatastoreEncoder);
            logger.info("create store (TIFF文件创建状态) : " + createStore);
            boolean publish = false;
            publish = manager.getPublisher().publishGeoTIFF(imageWorkspace, storeName, storeName, new File(fileUrl));


            logger.info("publish (TIFF文件发布状态) : " + publish);
            if (publish) {
                return true;
            }
        } else {
            logger.info("数据存储已经存在了,store:" + storeName);
        }
        return false;
    }


    /**
     * 方法一
     * 直接发布shp文件到geoServer
     * 将shp.zip通过http传递过去
     * 不主动设置样式和编码
     * <p>
     *
     * @param fileUrl 本地文件地址 zip格式
     * @param geocode 地理编码
     * @return boolean
     */
/*    public static boolean releaseShpByHttp(String fileUrl, String geocode) {
        try {
            File zipFile = new File(fileUrl);
            //存储名/图层名
            String storeName = FileUtil.getFileNameNoEx(zipFile.getName());
            GeoServerRESTReader reader = new GeoServerRESTReader(url, geoUsername, geoPassword);
            GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(url, geoUsername, geoPassword);
            RESTLayer layer = reader.getLayer(shpWorkspace, storeName);
            if (layer == null) {
                if (publisher.publishShp(shpWorkspace, storeName, storeName, zipFile, geocode)) {
                    logger.info("图层发布成功：" + storeName);
                    return true;
                } else {
                    logger.info("图层发布失败:" + storeName);
                }
            } else {
                logger.info("图层已经发布过了:" + storeName);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return false;

    }*/


    /**
     * 方法二
     * 向geoServer上传shp，并设置存储、图层名、样式和坐标系
     *
     * @param zipFilePath      压缩文件夹位置 完整文件地址
     * @param storeName        存储、图层名 英文
     * @param styleType        图层样式 shp工作空间下的对应样式
     * @param coordinateSystem 坐标系  EPSG:4326
     * @return boolean
     */
    public static boolean publishShp(String zipFilePath, String storeName, String styleType, String coordinateSystem) {
        if (coordinateSystem == null) {
            coordinateSystem = GeoServerRESTPublisher.DEFAULT_CRS;
        }
        try {
            //创建发布类,放入用户名密码和url
            GeoServerRESTPublisher geoServerRESTPublisher = new GeoServerRESTPublisher(url, geoUsername, geoPassword);
            boolean b = geoServerRESTPublisher.publishShp(shpWorkspace, storeName,
                    new NameValuePair[]{new NameValuePair("charset", "GBK")},
                    //图层名称               指定用于发布资源的方法
                    storeName, GeoServerRESTPublisher.UploadMethod.FILE,
                    //zip图集的地址，直接压缩不要文件夹      坐标系         样式
                    new File(zipFilePath).toURI(), coordinateSystem, styleType);
            if (b) {
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }


    /**
     * 上传图层样式文件到geoServer，sld文件
     * 需要放入指定文件夹下
     *
     * @param styleType 样式文件名(不包含sld后缀)
     * @return boolean
     */
    public static boolean publishStyle(String styleType) {
        try {
            URL u = new URL(url);
            GeoServerRESTManager manager = new GeoServerRESTManager(u, geoUsername, geoPassword);
            GeoServerRESTReader reader = manager.getReader();
            GeoServerRESTPublisher publisher = manager.getPublisher();
            //读取style文件
            String styleFile = stylePath + File.separator + styleType + ".sld";
            File file = new File(styleFile);
            //是否已经发布了改style
            if (!reader.existsStyle(shpWorkspace, styleType)) {
                publisher.publishStyleInWorkspace(shpWorkspace, file, styleType);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 删除指定的数据存储
     *
     * @param workspace 工作空间
     * @param storeName 存储名
     * @return boolean
     */
    public static boolean removeStore(String workspace, String storeName) {
        URL u = null;
        try {
            u = new URL(url);
            GeoServerRESTManager manager = new GeoServerRESTManager(u, geoUsername, geoPassword);
            GeoServerRESTStoreManager storeManager = manager.getStoreManager();
            GSAbstractStoreEncoder gsAbstractStoreEncoder;
            if (shpWorkspace.equals(workspace)) {
                gsAbstractStoreEncoder = new GSAbstractStoreEncoder(GeoServerRESTPublisher.StoreType.DATASTORES, storeName) {
                    @Override
                    protected String getValidType() {
                        return null;
                    }
                };
                gsAbstractStoreEncoder.setName(storeName);
                return storeManager.remove(workspace, gsAbstractStoreEncoder, true);
            }

            if (imageWorkspace.equals(workspace)) {
                gsAbstractStoreEncoder = new GSAbstractStoreEncoder(GeoServerRESTPublisher.StoreType.COVERAGESTORES, storeName) {
                    @Override
                    protected String getValidType() {
                        return null;
                    }
                };
                gsAbstractStoreEncoder.setName(storeName);
                return storeManager.remove(workspace, gsAbstractStoreEncoder, true);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

}
