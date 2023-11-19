package org.wdt.intellijmanager.command

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.wdt.intellijmanager.objects.ConfigObject
import org.wdt.intellijmanager.utils.OptionUtils
import org.wdt.utils.gson.getJsonObject
import org.wdt.utils.gson.getString
import org.wdt.utils.gson.readFileToJsonObject
import org.wdt.utils.io.*
import java.io.File
import java.io.IOException
import java.util.*

class CopyVmoptionsCommand {

    @Throws(IOException::class)
    fun copyVmoptionsFile(commandLine: CommandLine) {
        if (commandLine.hasOption(toolsCachePathCommandOption)) {
            val copyVmoptionsTask = CopyVmoptionsTask(
                commandLine.getOptionValue(toolsPathCommandOption),
                commandLine.getOptionValue(toolsCachePathCommandOption),
                if (commandLine.hasOption(SaveConfigFileCommand.saveFileOption))
                    SaveConfigFileCommand.getSavdFilePath(commandLine.getOptionValue(SaveConfigFileCommand.saveFileOption))
                else null
            )
            copyVmoptionsTask.notConfig = commandLine.hasOption(notCofnigOption)
            copyVmoptionsTask.openFile = commandLine.hasOption(ConfigCommand.openConfigFileOption)
            copyVmoptionsTask.copyVmoptionsFile()
        } else {
            throw IOException("Must have -cp parameter")
        }
    }

    companion object {

        @JvmStatic
        val toolsPathCommandOption: Option = OptionUtils.getOption("ip", "idepath", true)

        @JvmStatic
        val toolsCachePathCommandOption: Option = OptionUtils.getOption("cp", "cachepath", true)

        @JvmStatic
        val notCofnigOption: Option = OptionUtils.getOption("nc", "notconfig", false)
    }
}

class CopyVmoptionsTask(private var idePath: String, private var cachePath: String, private var saveFile: File?) {
    var notConfig: Boolean = false
    var openFile: Boolean = false

    @Throws(IOException::class)
    fun copyVmoptionsFile() {
        val config = if (notConfig) null else ConfigObject.getCofnig()
        if (config != null) println(config)
        val configAddress = if (config == null) cachePath else config.ideConfigSameDirectory!!.canonicalPath
        val pluginsAddress = if (config == null) cachePath else config.idePluginsSameDirectory!!.canonicalPath
        val ideBinPath = File(idePath)
        val cacheAddress = File(cachePath)
        cacheAddress.createDirectories()
        if (ideBinPath.isDirectory() && ideBinPath.isFileExists()) {
            val launchFirstJson = File(ideBinPath, "product-info.json").readFileToJsonObject()
                .getAsJsonArray("launch").getJsonObject(0)
            val child = File(ideBinPath.getCanonicalFile(), launchFirstJson.getString("vmOptionsFilePath"))
            val vmoptions = IOUtils.toString(
                Objects.requireNonNull(
                    CopyVmoptionsCommand::class.java.getResourceAsStream("/idea.vmoptions")
                )
            ).replace(":CacheAddress", FilenameUtils.separatorsToWindows(cachePath))
                .replace(":ConfigAddress", configAddress).replace(":PluginsAddress", pluginsAddress)
            child.writeStringToFile(vmoptions)
            println("Copy File To: $child")
            saveFile?.writeStringToFile("$idePath\n$cachePath")
            if (openFile) OptionUtils.openFile(child)
        } else {
            throw IOException("IDE path must is a directory")
        }
    }
}