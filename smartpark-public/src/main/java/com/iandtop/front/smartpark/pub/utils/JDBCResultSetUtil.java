package com.iandtop.front.smartpark.pub.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *@author andyzhao
 */
public class JDBCResultSetUtil {
    public static int getRowCount(ResultSet resultSet){
        int rowCount = 0;
        try {
            while(resultSet.next()) {
                rowCount++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rowCount;
    }

    //执行查询sql
    public static List resultSetToList(ResultSet rs) throws SQLException {
        List list = new ArrayList();
        //取得数据表中的字段数目，类型等返回结果
        ResultSetMetaData rsmd = rs.getMetaData();
        //是以ResultSetMetaData对象保存
        int columnCount = rsmd.getColumnCount(); //列的总数
        while (rs.next()) {
            Map m = new HashMap();
            for (int i = 1; i <= columnCount; i++) {
                m.put(rsmd.getColumnName(i), rs.getObject(i));
            }
            list.add(m);
        }

        return list;
    }
}
