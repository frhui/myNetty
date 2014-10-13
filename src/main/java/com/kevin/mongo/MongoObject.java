package com.kevin.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 对 DBObject 进行包装以方便访问
 *
 */
@SuppressWarnings("unchecked")
public class MongoObject extends HashMap<String, Object> implements Cloneable {

    public MongoObject() {
    }

    public MongoObject(DBObject dbObject) {

        for (Iterator<String> iterator = dbObject.keySet().iterator(); iterator.hasNext(); ) {
            String next = iterator.next();
            Object o = dbObject.get(next);

            Object value = convert(o);
            put(next, value);
        }
    }

    private Object convert(Object o) {

        if (o instanceof List) {
            List l = (List) o;
            List result = new ArrayList();

            for (Object element : l) {
                result.add(convert(element));
            }

            return result;

        } else if (o instanceof DBObject) {
            return new MongoObject((DBObject) o);

        } else {
            return o;
        }
    }

    /////////////////////////////////////////

    public int getInt(String key, int defaultValue) {
        Object value = get(key);

        if (!containsKey(key) || value == null) {
            return defaultValue;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return new BigDecimal(value.toString()).intValue();
        }
    }

    public double getDouble(String key, double defaultValue) {
        Object value = get(key);

        if (!containsKey(key) || value == null) {
            return defaultValue;
        }

        if (value instanceof Double) {
            return (Double) value;
        } else {
            return new BigDecimal(value.toString()).doubleValue();
        }
    }

    public String getString(String key) {
        if (!containsKey(key) || get(key) == null) {
            return null;
        }

        return (String) get(key);
    }

    public Boolean getBoolean(String key) {
        if (!containsKey(key) || get(key) == null) {
            return null;
        }

        return (Boolean) get(key);
    }

    public <T> T getObject(String key) {
        if (!containsKey(key) || get(key) == null) {
            return null;
        }

        return (T) get(key);
    }

    public List<MongoObject> getMongoObjectList(String key) {
        if (!containsKey(key) || get(key) == null) {
            return null;
        }

        return (List<MongoObject>) get(key);
    }

    public <T> List<T> getObjectList(String key) {
        if (!containsKey(key) || get(key) == null) {
            return null;
        }

        return (List<T>) get(key);
    }

    /////////////////////////////////////////

    public MongoObject clone() {
        MongoObject result = (MongoObject) super.clone();
        result.putAll(this);
        return result;
    }

    public DBObject toDBObject() {
        return new BasicDBObject(this);
    }

    public static MongoObject findOrCreateElement(List<MongoObject> mongoObjects, String key, Object value) {

        for (MongoObject mongoObject : mongoObjects) {
            Object v = mongoObject.getObject(key);

            if (v == null && value == null) {
                return mongoObject;
            }

            if (v != null && v.equals(value)) {
                return mongoObject;
            }
        }

        MongoObject mongoObject = new MongoObject();
        mongoObject.put(key, value);
        mongoObjects.add(mongoObject);
        return mongoObject;
    }
}
