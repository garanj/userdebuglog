package com.garan.userdebuglog.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter
import com.garan.userdebuglog.R
import org.slf4j.LoggerFactory
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

const val TAG = "UserDebugLog"

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Logging will only be enabled on userdebug builds
        if (Build.TYPE.equals("userdebug")) {
            Timber.plant(FileLoggingTree(this))
        }
    }
}


class FileLoggingTree(context: Context) : Timber.Tree() {
    private val logger = LoggerFactory.getLogger(MainApplication::class.java)

    init {
        configureForLocalConfigurationFile(context)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (priority) {
            Log.INFO -> logger.info(message, t)
            Log.WARN -> logger.warn(message, t)
            Log.DEBUG -> logger.debug(message, t)
            Log.VERBOSE -> logger.trace(message, t)
            Log.ERROR -> logger.error(message, t)
        }
    }

    /**
     * By default, the config file is searched for in set locations, and if found, loaded. However,
     * for this use case, the config file is located in /data/data/<app>/<config-file-name>, as it
     * needs to be in a location that can be read and written via adb.
     *
     * Therefore, this function ensures that config file exists and configures the logger with it.
     */
    @SuppressLint("LogNotTimber")
    private fun configureForLocalConfigurationFile(appContext: Context) {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val configFile = File(appContext.filesDir.absolutePath + CONFIG_FILE_NAME)

        if (!configFile.exists()) {
            Log.i(TAG, "Userdebuglog configuration file doesn't exist: Recreating")
            @Suppress("Since15")
            val configBytes =
                appContext.resources.openRawResource(R.raw.userdebuglog_config).readAllBytes()

            FileOutputStream(configFile.absolutePath).use {
                os -> os.write(configBytes)
            }
        }

        try {
            val configurator = JoranConfigurator()
            configurator.context = context
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-step configuration, omit calling context.reset().
            context.reset()
            configurator.doConfigure(configFile)
        } catch (je: JoranException) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context)
    }

    companion object {
        const val CONFIG_FILE_NAME = "/userdebuglog_config.xml"
    }
}