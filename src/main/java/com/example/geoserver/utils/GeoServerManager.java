package com.example.geoserver.utils;

import com.example.geoserver.constant.AcceptType;
import com.example.geoserver.constant.ContentType;
import com.example.geoserver.constant.HttpConstant;
import com.example.geoserver.error.ErrorException;
import com.example.geoserver.error.ExistedException;
import com.example.geoserver.error.ogc.CoverageStoreNotFoundException;
import com.example.geoserver.error.ogc.DataSourceNotFoundException;
import com.example.geoserver.error.ogc.LayerGroupNotFoundException;
import com.example.geoserver.error.ogc.LayerNotFoundException;
import com.example.geoserver.error.ogc.StyleServiceNotFoundException;
import com.example.geoserver.error.ogc.WorkSpaceNotFoundException;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSLayerGroupEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class GeoServerManager {
    //  加强geoserver publisher
    private final ImproveGeoServerPublisher geoServerRESTPublisher;
    //  geoserver REST 管理者
    private final GeoServerRESTManager geoServerRESTManager;
    //  geoserver REST 阅读者
    private final GeoServerReader reader;


    /**
     * 直接提供 geoserver 地址，用户名，密码为默认的： admin/geoserver
     *
     * @param restUrl geoserver 服务地址
     * @throws MalformedURLException 服务地址错误
     */
    public GeoServerManager(String restUrl) throws MalformedURLException {
        this(restUrl, "admin", "geoserver");
    }

    /**
     * 提供 geoserver 服务地址和用户名、密码
     *
     * @param restUrl  geoserver 服务地址
     * @param userName geoserver登录用户名
     * @param password geoserver 密码
     * @throws MalformedURLException 服务地址或登录失败错误
     */
    public GeoServerManager(String restUrl, String userName, String password) throws MalformedURLException {
        geoServerRESTPublisher = new ImproveGeoServerPublisher(restUrl, userName, password);
        geoServerRESTManager = new GeoServerRESTManager(new URL(restUrl), userName, password);
        reader = new GeoServerReader(restUrl, userName, password);
    }

    /**
     * 创建工作空间
     *
     * @param workspaceName 工作空间名称
     * @return 是否创建成功
     * @throws ExistedException 工作空间已存在
     */
    public Boolean createWorkspace(String workspaceName) throws ExistedException {
        if (reader.existsWorkspace(workspaceName)) {
            throw new ExistedException("工作空间；" + workspaceName);
        }

        return geoServerRESTPublisher.createWorkspace(workspaceName);
    }

    /**
     * 通过名称 和 URI 创建工作空间
     *
     * @param workspaceName 工作空间名称
     * @param uri           URI名称
     * @return 是否创建成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws URISyntaxException URI 格式或链接错误
     */
    public Boolean createWorkspace(String workspaceName, String uri) throws WorkSpaceNotFoundException, URISyntaxException {
        if (!reader.existsWorkspace(workspaceName)) {
            throw new WorkSpaceNotFoundException(workspaceName);
        }

        return geoServerRESTPublisher.createWorkspace(workspaceName, new URI(uri));
    }

    /**
     * 删除工作空间
     *
     * @param workspaceName 要删除的工作空间名称
     * @return 删除工作空间是否成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     */
    public Boolean removeWorkspace(String workspaceName) throws WorkSpaceNotFoundException {
        if (!reader.existsWorkspace(workspaceName)) {
            throw new WorkSpaceNotFoundException(workspaceName);
        }

        return geoServerRESTPublisher.removeWorkspace(workspaceName, true);
    }


    /**
     * 创建 Style 服务
     * 不能将同一 SLD 文件创建多个style 服务，这将会导致删除异常
     *
     * @param sldFile sld文件对象
     * @return 返回是否创建成功
     * @throws StyleServiceNotFoundException style 样式服务不存在
     * @throws IOException 读取 SLD 文件错误
     */
    public Boolean createStyle(File sldFile) throws StyleServiceNotFoundException, IOException {
        String sldFileName = sldFile.getName();

        String[] split = sldFileName.split(".sld");
        String styleName = split[0];

        reader.existsStyle(styleName);

        return this.createStyle(sldFile, styleName);
    }

    /**
     * 创建 Style 服务，并提供style 服务名称
     * 不能将同一 SLD 文件创建多个style 服务，这将会导致删除异常
     *
     * @param sldFile   sld 文件对象
     * @param styleName style 服务名称
     * @return 返回是否创建成功
     * @throws StyleServiceNotFoundException style 样式服务不存在
     * @throws IOException 读取 SLD 文件错误
     */
    public Boolean createStyle(File sldFile, String styleName) throws StyleServiceNotFoundException, IOException {
        if (!reader.existsStyle(styleName)) {
            throw new StyleServiceNotFoundException(styleName);
        }

        //     请求路径
        String url = String.format("%s/rest/styles?name=%s&raw=true", geoServerRESTPublisher.getRestURL(), styleName);
        //     登录名、密码字符串
        String logInStr = geoServerRESTPublisher.getLogInStr();
        //     获取sld文件内容
        String file = ComTools.readFile(sldFile);

        //     格式化 sld xml 文件
        String sldInfo = file.replaceAll(" {4}", "").replaceAll(" {2}", "").replaceAll("\"", "\\\\\"");

        String[] cmds = {
                "curl", "-u", logInStr,
                "-X", HttpConstant.POST, url,
                "-H", AcceptType.JSON,
                "-H", ContentType.SLD,
                "-d", "\"" + sldInfo + "\""
        };

        String curlPostResult = ComTools.curlPost(cmds);

        /*
         * ================================================ 创建完需要put一下
         * */

        //     请求路径
        String urlPUT = String.format("%s/rest/styles/%s?raw=true", geoServerRESTPublisher.getRestURL(), styleName);

        String[] cmdsPUT = {
                "curl", "-u", logInStr,
                "-X", HttpConstant.PUT, urlPUT,
                "-H", AcceptType.JSON,
                "-H", ContentType.SLD,
                "-d", "\"" + sldInfo + "\""
        };

        ComTools.curlPUT(cmdsPUT);

        return Objects.equals(curlPostResult, styleName);
    }

    /**
     * 删除style服务
     *
     * @param styleName 要删除的style 名称
     * @return 是否删除成功
     * @throws StyleServiceNotFoundException 样式服务不存在
     */
    public Boolean removeStyle(String styleName) throws StyleServiceNotFoundException {
        if (!reader.existsStyle(styleName)) {
            throw new StyleServiceNotFoundException(styleName);
        }

        return geoServerRESTPublisher.removeStyle(styleName, true);
    }

    /**
     * 创建 Style 服务到指定的工作空间下
     * 不能将同一 SLD 文件创建多个style 服务，这将会导致删除异常
     *
     * @param workspaceName 工作空间名称
     * @param sldFile       sld文件对象
     * @return 返回是否创建成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws ExistedException           style服务已存在
     * @throws IOException                文件错误
     */
    public Boolean createStyleToWorkspace(String workspaceName, File sldFile) throws WorkSpaceNotFoundException, ExistedException, IOException {
        reader.existsWorkspace(workspaceName);

        String sldFileName = sldFile.getName();
        String styleName = sldFileName.split(".sld")[0];

        return this.createStyleToWorkspace(workspaceName, sldFile, styleName);
    }

    /**
     * 创建 Style 服务到指定的工作空间下，并提供style 服务名称
     * 不能将同一 SLD 文件创建多个style 服务，这将会导致删除异常
     *
     * @param workspaceName 工作空间名称
     * @param sldFile       sld 文件对象
     * @param styleName     style 服务名称
     * @return 返回是否创建成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws ExistedException           style样式服务已存在
     * @throws IOException                文件错误
     */
    public Boolean createStyleToWorkspace(String workspaceName, File sldFile, String styleName) throws WorkSpaceNotFoundException, ExistedException, IOException {
        if (reader.existsStyleFromWorkspace(workspaceName, styleName)) {
            throw new ExistedException("style 样式服务：" + styleName);
        }

        //     请求路径
        String url = String.format("%s/rest/workspaces/%s/styles?name=%s&raw=true", geoServerRESTPublisher.getRestURL(), workspaceName, styleName);
        //     登录名、密码字符串
        String logInStr = geoServerRESTPublisher.getLogInStr();
        //     获取sld文件内容
        String file = ComTools.readFile(sldFile);

        //     格式化 sld xml 文件
        String sldInfo = file.replaceAll(" {4}", "").replaceAll(" {2}", "").replaceAll("\"", "\\\\\"");

        String[] cmds = {
                "curl", "-u", logInStr,
                "-X", HttpConstant.POST, url,
                "-H", AcceptType.JSON,
                "-H", ContentType.SLD,
                "-d", "\"" + sldInfo + "\""
        };

        String curlPostResult = ComTools.curlPost(cmds);

        /*
         * ================================================ 创建完需要put一下
         * */

        //     请求路径
        String urlPUT = String.format("%s/rest/workspaces/%s/styles/%s?raw=true", geoServerRESTPublisher.getRestURL(), workspaceName, styleName);

        String[] cmdsPUT = {
                "curl", "-u", logInStr,
                "-X", HttpConstant.PUT, urlPUT,
                "-H", AcceptType.JSON,
                "-H", ContentType.SLD,
                "-d", "\"" + sldInfo + "\""
        };

        ComTools.curlPUT(cmdsPUT);

        return Objects.equals(curlPostResult, styleName);
    }

    /**
     * 删除工作空间下的style服务
     *
     * @param workspaceName 工作空间名称
     * @param styleName     要删除的style 名称
     * @return 是否删除成功
     * @throws WorkSpaceNotFoundException    工作空间不存在
     * @throws StyleServiceNotFoundException 样式服务不存在
     */
    public Boolean removeStyleFromWorkspace(String workspaceName, String styleName) throws WorkSpaceNotFoundException, StyleServiceNotFoundException {
        if (!reader.existsStyleFromWorkspace(workspaceName, styleName)) {
            throw new StyleServiceNotFoundException(styleName);
        }

        return geoServerRESTPublisher.removeStyleInWorkspace(workspaceName, styleName, true);
    }

    /**
     * 创建shp 图层，shp文件名则为数据源、图层名
     *
     * @param workspaceName 工作空间
     * @param shpFile       shp Zip 文件对象
     * @param crsCode       坐标系代码
     * @return shp图层是否创建成功
     * @throws FileNotFoundException 文件不存在错误
     * @throws FileNotFoundException         文件不存在错误
     * @throws ErrorException                shp源必须为 zip
     * @throws WorkSpaceNotFoundException    工作空间不存在
     * @throws ExistedException              图层名称已存在
     */
    public Boolean createShpLayer(String workspaceName, File shpFile, int crsCode) throws FileNotFoundException, WorkSpaceNotFoundException, ErrorException, ExistedException {
        //    获取shp 名称
        String shpFileName = shpFile.getName();

        //    获取shp 文件名切割数组
        String[] splitList = shpFileName.split("\\.");

        //    如果后缀名不是 zip 则直接报错
        if (!Objects.equals(splitList[1], "zip")) {
            throw new ErrorException("shp 源文件必须为 zip 压缩包文件");
        }

        String crsName = "EPSG:" + crsCode;

        //    定义数据源名和图层名
        String storeName, layerName;
        storeName = layerName = splitList[0];

        if (reader.existsLayer(workspaceName, layerName)) {
            throw new ExistedException("图层名称：" + layerName);
        }

        return geoServerRESTPublisher.publishShp(workspaceName, storeName, layerName, shpFile, crsName);
    }

    /**
     * 创建shp 图层，shp文件名则为数据源、图层名，并指定样式
     *
     * @param workspaceName 工作空间
     * @param shpFile       shp Zip 文件对象
     * @param crsCode       坐标系代码
     * @param styleName     样式名称
     * @return 是否shp 图层是否成功
     * @throws FileNotFoundException         文件不存在错误
     * @throws ErrorException                shp源必须为 zip
     * @throws WorkSpaceNotFoundException    工作空间不存在
     * @throws ExistedException              图层名称已存在
     * @throws StyleServiceNotFoundException style 样式服务不存在
     */
    public Boolean createShpLayer(
            String workspaceName,
            File shpFile,
            int crsCode,
            String styleName
    ) throws FileNotFoundException, ErrorException, WorkSpaceNotFoundException, ExistedException, StyleServiceNotFoundException {
        //    获取shp 名称
        String shpFileName = shpFile.getName();

        //    获取shp 文件名切割数组
        String[] splitList = shpFileName.split("\\.");

        //    如果后缀名不是 zip 则直接报错
        if (!Objects.equals(splitList[1], "zip")) {
            throw new ErrorException("shp 源文件必须为 zip 压缩包文件");
        }

        String crsName = "EPSG:" + crsCode;

        //    定义数据源名和图层名
        String storeName, layerName;
        storeName = layerName = splitList[0];

        if (reader.existsLayer(workspaceName, layerName)) {
            throw new ExistedException("图层名称：%s" + layerName);
        }

        if (!reader.existsStyle(styleName)) {
            throw new StyleServiceNotFoundException(styleName);
        }

        return geoServerRESTPublisher.publishShp(workspaceName, storeName, layerName, shpFile, crsName, styleName);
    }

    /**
     * 创建shp 图层，shp文件名则为数据源、图层名，并指定在某工作空间下的样式服务
     *
     * @param workspaceName      工作空间
     * @param shpFile            shp Zip 文件对象
     * @param crsCode            坐标系代码
     * @param styleWorkspaceName 样式服务所在工作空间名称
     * @param styleName          样式名称
     * @return 创建shp 图层是否成功
     * @throws FileNotFoundException         文件不存在错误
     * @throws ErrorException                shp源文件必须为zip
     * @throws WorkSpaceNotFoundException    工作空间不存在
     * @throws ExistedException              图层名称已存在
     * @throws StyleServiceNotFoundException style样式服务不存在
     */
    public Boolean createShpLayer(
            String workspaceName,
            File shpFile,
            int crsCode,
            String styleWorkspaceName,
            String styleName
    ) throws FileNotFoundException, ErrorException, WorkSpaceNotFoundException, ExistedException, StyleServiceNotFoundException {
        //    获取shp 名称
        String shpFileName = shpFile.getName();

        //    获取shp 文件名切割数组
        String[] splitList = shpFileName.split("\\.");

        //    如果后缀名不是 zip 则直接报错
        if (!Objects.equals(splitList[1], "zip")) {
            throw new ErrorException("shp 源文件必须为 zip 压缩包文件");
        }

        String crsName = "EPSG:" + crsCode;

        //    定义数据源名和图层名
        String storeName, layerName;
        storeName = layerName = splitList[0];

        if (reader.existsLayer(workspaceName, layerName)) {
            throw new ExistedException("图层名称：%s" + layerName);
        }

        if (!reader.existsStyle(styleName)) {
            throw new StyleServiceNotFoundException(styleName);
        }

        String shpStyle = styleWorkspaceName + ":" + styleName;

        return geoServerRESTPublisher.publishShp(workspaceName, storeName, layerName, shpFile, crsName, shpStyle);
    }


    /**
     * 发布PostGIS 中存在的表图层
     *
     * @param gsPostGISDatastoreEncoder PostGIS DataStore 配置对象
     * @param workspaceName             工作空间名称
     * @param tableName                 要发布的表名
     * @param crsCode                   坐标系代码
     * @return 是否发布成功
     * @throws WorkSpaceNotFoundException 工作空间不存
     * @throws ExistedException           数据源已存在、图层已存在
     * @throws ErrorException             数据源发布失败
     */
    public Boolean createPostGISLayer(
            GSPostGISDatastoreEncoder gsPostGISDatastoreEncoder,
            String workspaceName,
            String tableName,
            int crsCode
    ) throws ExistedException, WorkSpaceNotFoundException, ErrorException {
        GSLayerEncoder gsLayerEncoder = new GSLayerEncoder();

        return createPostGISLayer(gsPostGISDatastoreEncoder, workspaceName, tableName, crsCode, gsLayerEncoder);
    }

    /**
     * 发布PostGIS 中存在的表图层
     *
     * @param gsPostGISDatastoreEncoder PostGIS DataStore 配置对象
     * @param workspaceName             工作空间名称
     * @param tableName                 要发布的表名
     * @param crsCode                   坐标系代码
     * @param styleName                 style 样式服务名称
     * @return 是否发布成功
     * @throws WorkSpaceNotFoundException 工作空间不存
     * @throws ExistedException           数据源已存在、图层已存在
     * @throws ErrorException             数据源发布失败
     */
    public Boolean createPostGISLayer(
            GSPostGISDatastoreEncoder gsPostGISDatastoreEncoder,
            String workspaceName,
            String tableName,
            int crsCode,
            String styleName
    ) throws ExistedException, WorkSpaceNotFoundException, ErrorException {
        GSLayerEncoder gsLayerEncoder = new GSLayerEncoder();

        gsLayerEncoder.setDefaultStyle(styleName);
        return createPostGISLayer(gsPostGISDatastoreEncoder, workspaceName, tableName, crsCode, gsLayerEncoder);
    }

    /**
     * 发布PostGIS 中存在的表图层，指定在工作空间中的样式服务
     *
     * @param gsPostGISDatastoreEncoder PostGIS DataStore 配置对象
     * @param workspaceName             工作空间名称
     * @param tableName                 要发布的表名
     * @param crsCode                   坐标系代码
     * @param styleWorkspace            style 样式服务工作空间名称
     * @param styleName                 style 样式服务名称
     * @return 是否发布成功
     * @throws WorkSpaceNotFoundException    工作空间不存
     * @throws StyleServiceNotFoundException 样式服务不存在
     * @throws ExistedException              数据源已存在、图层已存在
     * @throws ErrorException                数据源发布失败
     */
    public Boolean createPostGISLayer(
            GSPostGISDatastoreEncoder gsPostGISDatastoreEncoder,
            String workspaceName,
            String tableName,
            int crsCode,
            String styleWorkspace,
            String styleName
    ) throws WorkSpaceNotFoundException, StyleServiceNotFoundException, ExistedException, ErrorException {
        GSLayerEncoder gsLayerEncoder = new GSLayerEncoder();

        if (!reader.existsStyleFromWorkspace(styleWorkspace, styleName)) {
            throw new StyleServiceNotFoundException(styleName);
        }

        gsLayerEncoder.setDefaultStyle(styleWorkspace + ":" + styleName);

        return createPostGISLayer(gsPostGISDatastoreEncoder, workspaceName, tableName, crsCode, gsLayerEncoder);
    }

    /**
     * 发布PostGIS 中存在的表，并指定style样式
     *
     * @param gsPostGISDatastoreEncoder PostGIS DataStore 配置对象
     * @param workspaceName             工作空间名称
     * @param tableName                 要发布的表名
     * @param crsCode                   坐标系代码
     * @param gsLayerEncoder            图层配置对象
     * @return 是否发布成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws ExistedException           数据源已存在、图层已存在
     * @throws ErrorException             数据源创建失败
     */
    private Boolean createPostGISLayer(
            GSPostGISDatastoreEncoder gsPostGISDatastoreEncoder,
            String workspaceName,
            String tableName,
            int crsCode,
            GSLayerEncoder gsLayerEncoder
    ) throws WorkSpaceNotFoundException, ExistedException, ErrorException {
        if (reader.existsDataStore(workspaceName, tableName)) {
            throw new ExistedException("数据源：" + tableName);
        }

        if (reader.existsLayer(workspaceName, tableName)) {
            throw new ExistedException("图层：" + tableName);
        }

        //    创建一个datastore
        boolean postGISDataStoreResult = geoServerRESTManager.getStoreManager().create(workspaceName, gsPostGISDatastoreEncoder);

        //    获取 datastore 名称
        String storeName = gsPostGISDatastoreEncoder.getName();

        boolean publishDBLayerResult = false;

        if (postGISDataStoreResult) {
            GSFeatureTypeEncoder gsFeatureTypeEncoder = new GSFeatureTypeEncoder();

            gsFeatureTypeEncoder.setTitle(tableName);
            gsFeatureTypeEncoder.setNativeName(tableName);
            gsFeatureTypeEncoder.setName(tableName);
            gsFeatureTypeEncoder.setSRS("EPSG:" + crsCode);

            publishDBLayerResult = geoServerRESTPublisher.publishDBLayer(workspaceName, storeName, gsFeatureTypeEncoder, gsLayerEncoder);
        } else {
            throw new ErrorException(String.format("创建 datastore：%s 失败", storeName));
        }

        return publishDBLayerResult;
    }

    /**
     * 发布Tiff 服务（wms）
     *
     * @param workspaceName 工作空间名称
     * @param layerName     图层名称
     * @param tifFile       tif 文件对象
     * @return 是否发布成功
     * @throws FileNotFoundException      没有找到文件
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws ExistedException           图层已存在
     */
    public Boolean createGeoTIFFLayer(String workspaceName, String layerName, File tifFile) throws FileNotFoundException, WorkSpaceNotFoundException, ExistedException {
        if (reader.existsLayer(workspaceName, layerName)) {
            throw new ExistedException("图层：" + layerName);
        }

        return geoServerRESTPublisher.publishGeoTIFF(workspaceName, layerName, tifFile);
    }


    /**
     * 在指定工作空间下创建图层组
     *
     * @param workspaceName  工作空间民称
     * @param layerGroupName 图层组名称
     * @param layersList     图层名称列表
     * @return 图层组是否创建成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws LayerNotFoundException     图层不存在
     * @throws ExistedException           图层组已存在
     */
    public Boolean createLayerGroup(String workspaceName, String layerGroupName, ArrayList<String> layersList) throws WorkSpaceNotFoundException, LayerNotFoundException, ExistedException {
        if (reader.existsLayerGroup(workspaceName, layerGroupName)) {
            throw new ExistedException("图层组：" + layerGroupName);
        }

        GSLayerGroupEncoder gsLayerGroupEncoder = new GSLayerGroupEncoder();
        gsLayerGroupEncoder.setWorkspace(workspaceName);
        gsLayerGroupEncoder.setName(layerGroupName);

        for (String layer : layersList) {
            String[] split = layer.split(":");

            String layerWorkspaceName = split[0];
            String layerName = split[1];

            if (!reader.existsLayer(layerWorkspaceName, layerName)) {
                throw new LayerNotFoundException(layerName);
            }
            gsLayerGroupEncoder.addLayer(layer);
        }

        return geoServerRESTPublisher.createLayerGroup(workspaceName, layerGroupName, gsLayerGroupEncoder);
    }

    /**
     * 删除矢量数据源
     *
     * @param workspaceName 要删除的矢量数据源所在的工作空间名称
     * @param dataStoreName 要删除的矢量数据源名称
     * @return 是否删除成功
     * @throws DataSourceNotFoundException 数据源不存在
     * @throws WorkSpaceNotFoundException  工作空间不存在
     */
    public Boolean removeDataStore(String workspaceName, String dataStoreName) throws DataSourceNotFoundException, WorkSpaceNotFoundException {
        if (!reader.existsDataStore(workspaceName, dataStoreName)) {
            throw new DataSourceNotFoundException(dataStoreName);
        }

        return geoServerRESTPublisher.removeDatastore(workspaceName, dataStoreName, true);
    }

    /**
     * 删除栅格数据源
     *
     * @param workspaceName      要删除的栅格数据源所在的工作空间名称
     * @param coverageStoresName 要删除的栅格数据源名称
     * @return 栅格数据源是否删除成功
     * @throws WorkSpaceNotFoundException     工作空间不存在
     * @throws CoverageStoreNotFoundException 栅格数据源不存在
     */
    public Boolean removeCoverageStores(String workspaceName, String coverageStoresName) throws WorkSpaceNotFoundException, CoverageStoreNotFoundException {
        if (!reader.existsCoverageStore(workspaceName, coverageStoresName)) {
            throw new CoverageStoreNotFoundException(coverageStoresName);
        }

        return geoServerRESTPublisher.removeCoverageStore(workspaceName, coverageStoresName, true);
    }

    /**
     * 删除图层
     *
     * @param workspaceName 删除图层所在的工作空间名称
     * @param layerName     删除的图层的名称
     * @return 是否删除成功
     * @throws WorkSpaceNotFoundException 工作空间不存在
     * @throws LayerNotFoundException     图层不存在
     */
    public Boolean removeLayer(String workspaceName, String layerName) throws WorkSpaceNotFoundException, LayerNotFoundException {
        if (!reader.existsLayer(workspaceName, layerName)) {
            throw new LayerNotFoundException(layerName);
        }

        return geoServerRESTPublisher.removeLayer(workspaceName, layerName);
    }

    /**
     * 删除图层组
     *
     * @param workspaceName  删除图层组所在的工作空间
     * @param layerGroupName 图层组名称
     * @return 是否删除成功
     * @throws WorkSpaceNotFoundException  工作空间不存在
     * @throws LayerGroupNotFoundException 图层组不存在
     */
    public Boolean removeLayerGroup(String workspaceName, String layerGroupName) throws WorkSpaceNotFoundException, LayerGroupNotFoundException {
        if (!reader.existsLayerGroup(workspaceName, layerGroupName)) {
            throw new LayerGroupNotFoundException(layerGroupName);
        }

        return geoServerRESTPublisher.removeLayerGroup(workspaceName, layerGroupName);
    }


}
