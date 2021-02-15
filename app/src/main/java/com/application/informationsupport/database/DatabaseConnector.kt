package com.application.informationsupport.database

import android.database.SQLException
import java.sql.Connection
import java.sql.DriverManager

class DatabaseConnector {

    private val defaultDriver = "oracle.jdbc.driver.OracleDriver"
    private val defaultURL = "jdbc:oracle:thin:@26.132.63.148:1521:XE"
    private val defaultUsername = "system"
    private val defaultPassword = "12345"

    @Throws(ClassNotFoundException::class, SQLException::class)
    fun createConnection(
        driver: String,
        url: String,
        username: String,
        password: String
    ): Connection {
        Class.forName(driver)
        return DriverManager.getConnection(url, username, password)
    }

    @Throws(ClassNotFoundException::class, SQLException::class)
    fun createConnection(): Connection {
        return createConnection(defaultDriver, defaultURL, defaultUsername, defaultPassword)
    }

}