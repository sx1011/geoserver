package com.example.geoserver.utils;

import java.io.*;

public class ComTools {
    /**
     * 读取文件内容
     *
     * @param file File对象
     * @return 返回文件内容
     * @throws IOException IO 错误
     */
    public static String readFile(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        StringBuilder stringBuffer = new StringBuilder();

        while ((str = in.readLine()) != null) {
            stringBuffer.append(str);
        }

        return stringBuffer.toString();
    }

    /**
     * 提供curl post 请求方法
     * @param cmds curl 命令行 String 数组
     * @return 执行curl命令返回值 JSON
     * @throws IOException 获取输入输出流不存在
     */
    public static String curlPost(String[] cmds) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmds);

        Process start = processBuilder.start();
        InputStream inputStream = start.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder stringBuilder = new StringBuilder();

        bufferedReader.lines().forEach(stringBuilder::append);

        return stringBuilder.toString();
    }

    /**
     * 提供curl put 请求方法
     * @param cmds curl 命令行 String 数组
     * @return 执行curl命令返回值 JSON
     * @throws IOException 获取输入输出流不存在
     */
    public static String curlPUT(String[] cmds) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmds);

        Process start = processBuilder.start();
        InputStream inputStream = start.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder stringBuilder = new StringBuilder();

        bufferedReader.lines().forEach(stringBuilder::append);

        return stringBuilder.toString();
    }
}
