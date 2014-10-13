package com.kevin.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * (description)
 *
 */
public class MongoUtils {

    static final Logger log = LoggerFactory.getLogger(MongoUtils.class);

    public static final SimpleDateFormat DATE_FOTMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


    static {
        // Mongodb 所有的本地时间都会转化为 GMT 时间来保存，例如+8区的零点，保存进去之后会变成前一天的16点。
        // 所以在这里先将时间当做 GMT 时间转化为本地时间（即16点当成GMT时间，转化成本地时间就是下一天0点），
        // 保存进去之后就会变回16点。
        DATE_FOTMAT.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }

    // 根据指定的属性名，将 override 的属性值覆盖到 object 上
    public static void merge(DBObject object, DBObject override, String[] propertyNames) {
        if(propertyNames == null || propertyNames.length == 0) {
            return;
        }
        for (String propertyName : propertyNames) {
            if(override.get(propertyName) == null) {
                continue;
            }
            object.put(propertyName, override.get(propertyName));
        }
    }

    /**
     * 将字符串解析为时间对象
     *
     * @param time 时间字符串
     * @return 时间对象，如果解析失败，则返回当前时间
     */
    public static Date parseDate(String time) {
        return parseDate(time, new Date());
    }

    /**
     * 将字符串解析为时间对象
     *
     * @param time         时间字符串
     * @param defaultValue 如果解析失败则要返回的缺省值
     * @return 时间对象，如果解析失败，则返回缺省值
     */
    public static Date parseDate(String time, Date defaultValue) {
        try {
            return DATE_FOTMAT.parse(time);
        } catch (ParseException e) {
            log.error("转换时间失败：\"" + time + "\"", e);
            return defaultValue;
        }
    }

    public static Date parseDate(DBObject object, String property) {
        if (object == null || !object.containsField(property)) {
            return new Date();
        }

        if (object.get(property) instanceof Date) {
            return (Date) object.get(property);
        }

        return parseDate(object.get(property).toString());
    }

    // 将指定属性值转换为 Date 类型
    public static void convertToDate(DBObject dbObject, String[] propertyNames) {
        for (String propertyName : propertyNames) {
            Object value = dbObject.get(propertyName);

            if (value == null) {
                continue;
            }

            dbObject.put(propertyName, parseDate(value.toString()));
        }
    }

    /**
     * 将内容记录的集合属性值中的元素的指定属性值转换为日期类型
     *
     * @param dbObject                内容记录
     * @param collectionPropertyNames 集合属性名
     * @param datePropertyNames       集合元素的属性名，这些属性的值将被转换为日期类型
     */
    public static void convertCollectionPropertyToDate(
            DBObject dbObject, String[] collectionPropertyNames, String[] datePropertyNames) {

        for (String collectionPropertyName : collectionPropertyNames) {
            Object collection = dbObject.get(collectionPropertyName);

            if (collection == null || !(collection instanceof BasicDBList)) {
                continue;
            }

            BasicDBList list = (BasicDBList) collection;
            for (Object o : list) {

                if (!(o instanceof BasicDBObject)) {
                    continue;
                }

                BasicDBObject element = (BasicDBObject) o;
                for (String propertyName : datePropertyNames) {
                    Object value = element.get(propertyName);

                    if (value == null || !(value instanceof String)) {
                        continue;
                    }

                    try {
                        element.put(propertyName, DATE_FOTMAT.parse(value.toString()));
                    } catch (ParseException e) {
                        log.warn("Error parsing property \"" + propertyName + "\" of \"" + collectionPropertyName + "\"");
                    }
                }
            }
        }
    }

    /**
     * 获取或创建一个扩展属性对象
     *
     * @param object      内容记录
     * @param extProperty 扩展属性名
     * @return 扩展属性对象。如果属性不存在，则自动创建一个并放入内容记录中
     */
    public static BasicDBObject getOrCreateExt(DBObject object, String extProperty) {
        BasicDBList ext = (BasicDBList) object.get("ext");

        if (ext == null) {
            ext = new BasicDBList();
            object.put("ext", ext);
        }

        BasicDBObject property = null;
        for (Object o : ext) {
            BasicDBObject dbObject = (BasicDBObject) o;

            if (dbObject.get("name") != null && dbObject.get("name").equals(extProperty)) {
                property = dbObject;
                break;
            }
        }

        if (property == null) {
            property = new BasicDBObject().append("name", extProperty);
            ext.add(property);
        }

        return property;
    }

    /**
     * 将制定的属性和属性值加入到扩展属性中
     *
     * @param object       内容记录
     * @param propertyName 扩展属性名
     * @param propertyVale 扩展属性值
     */
    public static void putPropertyToExt(DBObject object, String propertyName, Object propertyVale) {
        BasicDBList ext = (BasicDBList) object.get("ext");

        if (ext == null) {
            ext = new BasicDBList();
            object.put("ext", ext);
        }


        boolean findData = false;
        for (Object o : ext) {
            BasicDBObject dbObject = (BasicDBObject) o;

            if (dbObject.get("name") != null && dbObject.get("name").equals(propertyName)) {
                findData = true;
                dbObject.put("value", propertyVale);
                break;
            }
        }

        if (!findData) {
            DBObject property = new BasicDBObject();
            property.put("name", propertyName);
            property.put("value", propertyVale);
            ext.add(property);
        }

    }

    /**
     * 合并两条内容记录的 ext 属性。参数 override 中的扩展属性值将覆盖到 object 中的扩展属性上。
     *
     * @param object   被合并的内容记录
     * @param override 包含覆盖属性值的内容记录
     */
    public static void mergeExt(DBObject object, DBObject override) {
        BasicDBList originalList = (BasicDBList) object.get("ext");
        BasicDBList overrideList = (BasicDBList) override.get("ext");

        if (originalList == null) {
            object.put("ext", overrideList);

        } else if (overrideList != null) {
            for (Object o : overrideList) {
                BasicDBObject overrideExtProperty = (BasicDBObject) o;

                boolean found = false;
                for (Object o1 : originalList) {
                    BasicDBObject originalExtProperty = (BasicDBObject) o1;

                    if (originalExtProperty.get("name").equals(overrideExtProperty.get("name"))) {
                        found = true;
                        originalExtProperty.put("value", overrideExtProperty.get("value"));
                        break;
                    }
                }

                if (!found) {
                    originalList.add(overrideExtProperty);
                }
            }
        }
    }

    public static boolean isFieldMissing(DBObject dbObject, String fieldName) {
        return !dbObject.containsField(fieldName)
                || dbObject.get(fieldName) == null
                || StringUtils.isEmpty(dbObject.get(fieldName).toString());
    }
}
