package com.meetangee

import ExecuteShellCommand

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.apache.commons.csv.CSVFormat
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileReader
import java.nio.Buffer
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DateFormat
import java.text.ParseException
import java.util.*
import java.util.Locale
import java.text.SimpleDateFormat


data class LogEvent(val created: Date, val message: String)

val data: MutableMap<String, MutableList<LogEvent>> = HashMap()

fun main(arg: Array<String>) {
    val server = embeddedServer(Netty, 8080) {
        routing {
//            get("/") {
//                call.respondText("Hey there..", ContentType.Text.Html)
//            }
        }
    }

    System.out.println(exec("pwd"))
    loadData()
    System.out.println("Successfully parsed")

    server.start(wait = true)
}

fun loadData() {
    val logPath = "src/main/resources/log"
    val logDir = File(logPath)

    if (logDir.isDirectory) {
        logDir.listDirectories()
                .forEach {
                    parseDateDir(it)
                }
    } else throw IllegalStateException("$logPath is not a directory")
}

fun parseDateDir(dateDir: File) {
    if (!dateDir.isDirectory) throw IllegalStateException("${dateDir.path} is not a directory")

    val date = dateDir.name.split("T").first()

    dateDir.listDirectories()
            .takeIf { it.isNotEmpty() }
            ?.first()
            ?.listDirectories()
            ?.forEach { userDir ->
                data[date].let {
                    if (it == null) {
                        data[date] = parseUserDir(userDir)
                    } else {
                        data[date]?.addAll(parseUserDir(userDir))
                    }
                }
            }


}

fun parseUserDir(userDir: File): MutableList<LogEvent> {
    if (!userDir.isDirectory) throw IllegalStateException("${userDir.path} is not a directory")

    val userId = userDir.name.toLong()
    val logFileName = "000000"
    val logFile = File(userDir, logFileName)
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    val userLogs = ArrayList<LogEvent>()


    if (logFile.doesNotExist()) {
        exec("gunzip -k ${userDir.absolutePath}/$logFileName.gz").printToConsole()
    }

    if (logFile.doesNotExist()) throw IllegalStateException("Error during unzipping log file in ${userDir.path}")


    Files.lines(Paths.get(logFile.absolutePath)).use {
        it
                .filter { it.isNotBlank() }
                .forEach {
                    val lineSplits = it?.split(delimiters = *arrayOf(" "), ignoreCase = true, limit = 2)
                    val date = format.parseOrNull(lineSplits?.first())
                    if (date != null) userLogs.add(LogEvent(date, lineSplits!![1]))

                }
    }

    return userLogs
}


fun File.listDirectories(): Array<out File> {
    return listFiles(FileFilter { it.isDirectory })
}

fun File.doesNotExist(): Boolean {
    return !exists()
}

fun String.printToConsole() {
    System.out.println(this)
}

fun DateFormat.parseOrNull(source: String?) : Date? {
    return try {
        parse(source)
    } catch (e: ParseException) {
        null
    }
}

fun exec(command: String): String {
    return ExecuteShellCommand().executeCommand(command)
}

