package com.example.geoserver.error;

/*
 * 一般错误信息
 * */
public class ErrorException extends Exception{
    public ErrorException(String message) {
        super(message);
    }
}