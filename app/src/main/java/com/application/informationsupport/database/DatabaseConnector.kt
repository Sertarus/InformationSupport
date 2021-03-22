package com.application.informationsupport.database

import android.database.SQLException
import java.sql.Connection
import java.sql.DriverManager

class DatabaseConnector(val url: String?, val username: String?, val password: String?) {

    private val defaultDriver = "oracle.jdbc.driver.OracleDriver"

    @Throws(ClassNotFoundException::class, SQLException::class)
    fun createConnection(
        driver: String,
        url: String?,
        username: String?,
        password: String?
    ): Connection {
        Class.forName(driver)
        return DriverManager.getConnection(url, username, password)
    }

    @Throws(ClassNotFoundException::class, SQLException::class)
    fun createConnection(): Connection {
        return createConnection(defaultDriver, url, username, password)
    }

}