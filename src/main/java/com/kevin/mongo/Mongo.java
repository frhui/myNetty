package com.kevin.mongo;

import com.mongodb.DB;
import com.mongodb.DBCollection;

import java.net.UnknownHostException;
import java.util.Date;

/**
 * 对 mongodb 的包装
 *
 */
public class Mongo {

    private DB db;

    /**
     * 构造方法
     *
     * @param host 服务器名称
     * @param port 服务器地址
     * @param db   数据库名称
     *
     * @throws java.net.UnknownHostException 如果无法解析服务器地址
     */
    public Mongo(String host, int port, String db) throws UnknownHostException {
        com.mongodb.Mongo mongo = new com.mongodb.Mongo(host, port);
        this.db = mongo.getDB(db);
    }


    public Mongo(String host, int port, String db, String username, String password) throws UnknownHostException {
        com.mongodb.Mongo mongo = new com.mongodb.Mongo(host, port);
        this.db = mongo.getDB(db);
        this.db.authenticate(username, password.toCharArray());
    }

    // 获取指定的集合
    public DBCollection getCollection(String name) {
        return this.db.getCollection(name);
    }

    // 获取数据库当前时间
    public Date getCurrentTime() {
        Object object = db.eval("new Date()");
        return (Date) object;
    }
}
