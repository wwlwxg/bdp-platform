/*
 * Copyright [2022] [DMetaSoul Team]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dmetasoul.lakesoul.meta;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dmetasoul.lakesoul.meta.entity.DataFileOp;
import com.zaxxer.hikari.HikariConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static com.dmetasoul.lakesoul.meta.DBConfig.*;

public class DBUtil {

    private static final String driverNameDefault = "org.postgresql.Driver";
    private static final String urlDefault = "jdbc:postgresql://127.0.0.1:5432/lakesoul_test?stringtype=unspecified";
    private static final String usernameDefault = "lakesoul_test";
    private static final String passwordDefault = "lakesoul_test";

    private static final String driverNameKey = "lakesoul.pg.driver";
    private static final String urlKey = "lakesoul.pg.url";
    private static final String usernameKey = "lakesoul.pg.username";
    private static final String passwordKey = "lakesoul.pg.password";

    private static final String driverNameEnv = "LAKESOUL_PG_DRIVER";
    private static final String urlEnv = "LAKESOUL_PG_URL";
    private static final String usernameEnv = "LAKESOUL_PG_USERNAME";
    private static final String passwordEnv = "LAKESOUL_PG_PASSWORD";

    private static final String lakeSoulHomeEnv = "LAKESOUL_HOME";

    // Retrieve config value in order: ENV, System Prop, Default Value
    private static String getConfigValue(String envKey, String propKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value != null) {
            return value;
        }
        value = System.getProperty(propKey);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    /**
     * PG connection config retrieved in the following order:
     * 1. An env var "LAKESOUL_HOME" (case-insensitive) point to a property file or
     * 2. A system property "lakesoul_home" (in lower case) point to a property file;
     * Following config keys are used to read the property file:
     * lakesoul.pg.driver, lakesoul.pg.url, lakesoul.pg.username, lakesoul.pg.password
     * 3. Any of the following env var exist:
     * LAKESOUL_PG_DRIVER, LAKESOUL_PG_URL, LAKESOUL_PG_USERNAME, LAKESOUL_PG_PASSWORD
     * 4. Otherwise, resolved to each's config's default
     */
    public static DataBaseProperty getDBInfo() {

        String configFile = System.getenv(lakeSoulHomeEnv);
        if (null == configFile) {
            configFile = System.getenv(lakeSoulHomeEnv.toLowerCase());
            if (null == configFile) {
                configFile = System.getProperty(lakeSoulHomeEnv.toLowerCase());
            }
        }
        Properties properties = new Properties();
        if (configFile != null) {
            try {
                properties.load(Files.newInputStream(Paths.get(configFile)));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            properties.setProperty(driverNameKey, getConfigValue(driverNameEnv, driverNameKey, driverNameDefault));
            properties.setProperty(urlKey, getConfigValue(urlEnv, urlKey, urlDefault));
            properties.setProperty(usernameKey, getConfigValue(usernameEnv, usernameKey, usernameDefault));
            properties.setProperty(passwordKey, getConfigValue(passwordEnv, passwordKey, passwordDefault));
        }
        DataBaseProperty dataBaseProperty = new DataBaseProperty();
        dataBaseProperty.setDriver(properties.getProperty(driverNameKey, driverNameDefault));
        dataBaseProperty.setUrl(properties.getProperty(urlKey, urlDefault));
        dataBaseProperty.setUsername(properties.getProperty(usernameKey, usernameDefault));
        dataBaseProperty.setPassword(properties.getProperty(passwordKey, passwordDefault));
        return dataBaseProperty;
    }

    public static void cleanAllTable() {
        String tableInfo = "truncate table table_info";
        String tableNameId = "truncate table table_name_id";
        String tablePathId = "truncate table table_path_id";
        String dataCommitInfo = "truncate table data_commit_info";
        String partitionInfo = "truncate table partition_info";
        String namespace = "truncate table namespace";
        Connection conn;
        Statement stmt;
        try {
            conn = DBConnector.getConn();
            stmt = conn.createStatement();
            stmt.addBatch(tableInfo);
            stmt.addBatch(tableNameId);
            stmt.addBatch(tablePathId);
            stmt.addBatch(dataCommitInfo);
            stmt.addBatch(partitionInfo);
            stmt.addBatch(namespace);
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBConnector.closeAllConnections();
        }
    }

    public static JSONObject stringToJSON(String s) {
        return JSONObject.parseObject(s);
    }

    public static JSONObject stringMapToJson(Map<String, String> map) {
        JSONObject object = new JSONObject();
        object.putAll(map);
        return object;
    }

    public static Map<String, String> jsonToStringMap(JSONObject o) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : o.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }

    public static String jsonToString(JSONObject o) {
        return JSON.toJSONString(o);
    }

    public static JSONArray stringToJSONArray(String s) {
        return JSONArray.parseArray(s);
    }

    public static String changeDataFileOpListToString(List<DataFileOp> dataFileOpList) {
        if (dataFileOpList.size() < 1) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (DataFileOp dataFileOp : dataFileOpList) {
            String path = dataFileOp.getPath();
            String fileOp = dataFileOp.getFileOp();
            long size = dataFileOp.getSize();
            String fileExistCols = dataFileOp.getFileExistCols();
            sb.append(String.format("\"(%s,%s,%s,\\\"%s\\\")\",", path, fileOp, size, fileExistCols));
        }
        sb = new StringBuilder(sb.substring(0, sb.length() - 1));
        sb.append("}");
        return sb.toString();
    }

    public static List<DataFileOp> changeStringToDataFileOpList(String s) {
        List<DataFileOp> rsList = new ArrayList<>();
        if (!s.startsWith("{") || !s.endsWith("}")) {
            // todo 这里应该报错
            return rsList;
        }
        String[] fileOpTmp = s.substring(1, s.length() - 1).split("\",\"");
        for (String value : fileOpTmp) {
            String tmpElem = value.replace("\"", "").replace("\\", "");
            if (!tmpElem.startsWith("(") || !tmpElem.endsWith(")")) {
                // todo 报错
                continue;
            }
            tmpElem = tmpElem.substring(1, tmpElem.length() - 1);
            DataFileOp dataFileOp = new DataFileOp();
            dataFileOp.setPath(tmpElem.substring(0, tmpElem.indexOf(",")));
            tmpElem = tmpElem.substring(tmpElem.indexOf(",") + 1);
            String fileOp = tmpElem.substring(0, tmpElem.indexOf(","));
            dataFileOp.setFileOp(fileOp);
            tmpElem = tmpElem.substring(tmpElem.indexOf(",") + 1);
            dataFileOp.setSize(Long.parseLong(tmpElem.substring(0, tmpElem.indexOf(","))));
            tmpElem = tmpElem.substring(tmpElem.indexOf(",") + 1);
            dataFileOp.setFileExistCols(tmpElem);
            rsList.add(dataFileOp);
        }
        return rsList;
    }

    public static String formatTableInfoPartitionsField(List<String> primaryKeys, List<String> rangePartitions) {
        return formatTableInfoPartitionsField(
                String.join(LAKESOUL_HASH_PARTITION_SPLITTER, primaryKeys),
                String.join(LAKESOUL_RANGE_PARTITION_SPLITTER, rangePartitions));
    }

    public static String formatTableInfoPartitionsField(String primaryKeys, List<String> rangePartitions) {
        return formatTableInfoPartitionsField(primaryKeys,
                String.join(LAKESOUL_RANGE_PARTITION_SPLITTER,
                        rangePartitions));
    }

    public static String formatTableInfoPartitionsField(String primaryKeys, String rangePartitions) {
        return String.join(LAKESOUL_PARTITION_SPLITTER_OF_RANGE_AND_HASH, rangePartitions, primaryKeys);
    }

    public static TablePartitionKeys parseTableInfoPartitions(String partitions) {
        if (StringUtils.isBlank(partitions) || partitions.equals(LAKESOUL_PARTITION_SPLITTER_OF_RANGE_AND_HASH)) {
            return new TablePartitionKeys();
        }
        // has hash keys, no range keys
        String[] rangeAndPks = StringUtils.split(partitions, LAKESOUL_PARTITION_SPLITTER_OF_RANGE_AND_HASH);
        if (StringUtils.startsWith(partitions, LAKESOUL_PARTITION_SPLITTER_OF_RANGE_AND_HASH)) {
            return new TablePartitionKeys(
                    Arrays.asList(StringUtils.split(rangeAndPks[0], LAKESOUL_HASH_PARTITION_SPLITTER)),
                    Collections.emptyList());
        }
        // has range keys, no pks
        if (StringUtils.endsWith(partitions, LAKESOUL_PARTITION_SPLITTER_OF_RANGE_AND_HASH)) {
            return new TablePartitionKeys(Collections.emptyList(),
                    Arrays.asList(StringUtils.split(rangeAndPks[0], LAKESOUL_RANGE_PARTITION_SPLITTER)));
        }
        return new TablePartitionKeys(
                Arrays.asList(StringUtils.split(rangeAndPks[1], LAKESOUL_HASH_PARTITION_SPLITTER)),
                Arrays.asList(StringUtils.split(rangeAndPks[0], LAKESOUL_RANGE_PARTITION_SPLITTER))
        );
    }

    // assume that map iteration order is partition level order
    // usually implementations uses LinkedHashMap for this parameter
    public static String formatPartitionDesc(Map<String, String> partitionDesc) {
        if (partitionDesc.isEmpty()) {
            return LAKESOUL_NON_PARTITION_TABLE_PART_DESC;
        }
        return partitionDesc.entrySet().stream().map(entry -> String.join(LAKESOUL_PARTITION_DESC_KV_DELIM,
                entry.getKey(),
                entry.getValue())).collect(Collectors.joining(LAKESOUL_RANGE_PARTITION_SPLITTER));
    }

    public static LinkedHashMap<String, String> parsePartitionDesc(String partitionDesc) {
        LinkedHashMap<String, String> descMap = new LinkedHashMap<>();
        if (partitionDesc.equals(LAKESOUL_NON_PARTITION_TABLE_PART_DESC)) {
            descMap.put("", LAKESOUL_NON_PARTITION_TABLE_PART_DESC);
            return descMap;
        }
        String[] splits = StringUtils.split(partitionDesc, LAKESOUL_RANGE_PARTITION_SPLITTER);
        for (String part : splits) {
            String[] kv = part.split(LAKESOUL_PARTITION_DESC_KV_DELIM, -1);
            if (kv.length != 2 || kv[0].isEmpty()) {
                throw new RuntimeException("Partition Desc " + part + " is not valid");
            }
            descMap.put(kv[0], kv[1]);
        }
        return descMap;
    }

    public static void fillDataSourceConfig(HikariConfig config) {
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(60000);
        config.setMaximumPoolSize(8);
        config.setKeepaliveTime(10000);
        config.setMinimumIdle(1);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }

    public static class TablePartitionKeys {
        public List<String> primaryKeys;
        public List<String> rangeKeys;

        public TablePartitionKeys() {
            primaryKeys = Collections.emptyList();
            rangeKeys = Collections.emptyList();
        }

        public TablePartitionKeys(List<String> pks, List<String> rks) {
            primaryKeys = pks;
            rangeKeys = rks;
        }

        public String getPartitionsString() {
            return formatTableInfoPartitionsField(primaryKeys, rangeKeys);
        }

        public String getPKString() {
            if (primaryKeys.isEmpty()) return "";
            return String.join(LAKESOUL_HASH_PARTITION_SPLITTER, primaryKeys);
        }

        public String getRangeKeyString() {
            if (rangeKeys.isEmpty()) return "";
            return String.join(LAKESOUL_RANGE_PARTITION_SPLITTER, rangeKeys);
        }
    }
}
