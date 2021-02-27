#!/usr/bin/env kscript

@file:MavenRepository("local", "file:///Users/mbonnin/.m2/repository")
@file:DependsOn("net.mbonnin.vespene:vespene-lib:0.5")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:3.1.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.4.2")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.mbonnin.vespene.lib.NexusStagingClient
import java.io.File
import java.lang.IllegalArgumentException
import okio.buffer
import okio.source
import kotlin.system.exitProcess
import net.mbonnin.vespene.lib.md5
import net.mbonnin.vespene.lib.pom.fixIfNeeded
import net.mbonnin.vespene.sign
import kotlinx.coroutines.runBlocking

Thread.currentThread().setUncaughtExceptionHandler { _, e ->
  // OkHttp has threadpools that keep the process alive. kill everything
  e.printStackTrace()
  exitProcess(1)
}

MainCommand().subcommands(Upload(), Prepare()).main(args)

exitProcess(0)

class MainCommand : CliktCommand() {
  override fun run() {
  }
}

class Upload : CliktCommand() {
  private val input by option(help = "The files downloaded from jcenter. Starting after the groupId, like \$module/\$version/\$module-\$version.jar").required()

  private val username by option(help = "your nexus username. For OSSRH, this is your Sonatype jira username. Defaults to reading the 'SONATYPE_NEXUS_USERNAME' environment variable.")
  private val password by option(help = "your nexus password. For OSSRH, this is your Sonatype jira password. Defaults to reading the 'SONATYPE_NEXUS_PASSWORD' environment variable.")

  private val profileId by option(
    help = "your profileId. For OSSRH, this is what you see when you go to https://oss.sonatype.org/#stagingProfiles;\${profileId}." +
        " Defaults to reading the 'SONATYPE_NEXUS_PROFILE_ID' environment variable. Mandatory if the sonatype account has several profileIds."
  )

  override fun run() {

    val client = NexusStagingClient(
      username = username ?: System.getenv("SONATYPE_NEXUS_USERNAME")
      ?: throw IllegalArgumentException("Please specify --username or SONATYPE_NEXUS_USERNAME environment variable"),
      password = password ?: System.getenv("SONATYPE_NEXUS_PASSWORD")
      ?: throw IllegalArgumentException("Please specify --password or SONATYPE_NEXUS_PASSWORD environment variable"),
    )

    uploadFiles(File(input), client)

    println(
      """
      
      🎉 Your files are uploaded 🎉. 
      Go to https://oss.sonatype.org/#stagingRepositories to release them to the world 🚀.
    """.trimIndent()
    )
  }

  private fun uploadFiles(input: File, client: NexusStagingClient) {
    println("uploading...")
    var fileCount = 0
    runBlocking {
      val repositoryId = client.upload(
        directory = input,
        profileId = findProfileId(client),
        comment = "Created by vespene"
      ) { index, total, _ ->
        print("\r  $index/$total")
        System.out.flush()
        fileCount = total
      }
      println("\r  $fileCount files uploaded to '$repositoryId'")
      print("\rclosing...")
      client.closeRepositories(listOf(repositoryId))
    }
  }

  private suspend fun findProfileId(client: NexusStagingClient): String {
    if (profileId != null) {
      return profileId!!
    }

    val envVar = System.getenv("SONATYPE_NEXUS_PROFILE_ID")
    if (envVar != null) {
      return envVar
    }

    println("Looking up profileId...")
    val allIds = client.getProfiles()
    check(allIds.size == 1) {
      val prettyIds = allIds.map {
        "--profile-id=${it.id} (${it.name})"
      }.joinToString("\n")
      "Multiple profileIds found. Use one of:\n${prettyIds}\n"
    }
    return allIds.first().id
  }
}

class Prepare : CliktCommand() {
  private val privateKey by option(
    help = "The file containing the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----." +
        " It can be obtained with gpg --armour --export-secret-keys KEY_ID. Defaults to reading the 'GPG_PRIVATE_KEY' environment variable."
  )
  private val privateKeyPasword by option(
    help = "The  password for the private key. Defaults to reading the 'GPG_PRIVATE_KEY_PASSWORD' environment variable."
  )

  private val input by option(help = "The files downloaded from jcenter. Starting after the groupId, like \$module/\$version/\$module-\$version.jar").required()
  private val output by option(help = "A scratch directory where to put the patched").required()
  private val group by option(help = "The group of the coordinates of your modules. It starts with the groupId configured in Sonatype but can be longer").required()

  private val pomProjectUrl by option()
  private val pomLicenseUrl by option()
  private val pomLicenseName by option()
  private val pomDeveloperName by option()
  private val pomScmUrl by option()
  private val pomProjectName by option()
  private val pomDescription by option()
  private val versions by option(help = "A file containing a list of versions to transfer. Put one version by line. If not specified, the script will upload everything at once")

  override fun run() {
    if (File(output).exists()) {
      print("$output already exists, overwrite? [y/n]")
      while (true) {
        when(readLine()) {
          "y" -> {
            File(output).deleteRecursively()
            break
          }
          "n" -> exitProcess(0)
        }
      }
    }
    val versionsToUpload = getVersions()
    if (versionsToUpload != null) {
      versionsToUpload.forEach {
        prepare(it)
      }
    } else {
      prepare(null)
    }
  }

  private fun prepare(version: String?) {
    prepareFiles(
      File(input),
      File(output),
      group = group,
      version = version,
      privateKey = privateKey?.let { File(it).readText() } ?: System.getenv("GPG_PRIVATE_KEY")
      ?: throw IllegalArgumentException("Please specify --private-key or GPG_PRIVATE_KEY environment variable"),
      privateKeyPassword = privateKeyPasword ?: System.getenv("GPG_PRIVATE_KEY_PASSWORD")
      ?: throw IllegalArgumentException("Please specify --private-key-password or GPG_PRIVATE_KEY_PASSWORD environment variable"),
    )
  }

  private fun getVersions(): List<String>? {
    if (versions == null) {
      return null
    }

    val includedVersions = versions?.let {
      File(it).readLines().map {
        it.replace("/", "")
      }
    }

    val inputFile = File(input)
    val allVersions = inputFile.listFiles().flatMap {
      if (it.isDirectory) {
        it.listFiles().filter { it.isDirectory }
      } else {
        emptyList()
      }
    }.map {
      it.name
    }.distinct()
      .sorted()

    return allVersions.filter {
      includedVersions == null || includedVersions.contains(it)
    }
  }

  private fun prepareFiles(
    input: File,
    output: File,
    group: String,
    version: String?,
    privateKey: String,
    privateKeyPassword: String,
  ) {
    val dest = File(output, group.replace(".", "/"))

    val moduleDirs = input.listFiles().filter {
      it.isDirectory
    }.sortedBy { it.name }

    moduleDirs.forEachIndexed { moduleIndex, moduleDir ->
      val versionDirs = moduleDir.listFiles()
        .filter {
          it.isDirectory && (version == null || version == it.name)
        }.sortedBy { it.name }

      versionDirs.forEachIndexed { index, versionDirectory ->
        val moduleString = "${moduleDir.name}"
        val versionString = "version ${versionDirectory.name}"
        if (version != null) {
          print("\rpreparing files for $versionString $moduleString...\u001b[0K")
        } else {
          print("\rpreparing files for $moduleString $versionString...\u001b[0K")
        }

        versionDirectory.listFiles()
          ?.filter { it.isFile }
          ?.filter { it.extension != "md5" && it.extension != "asc" }
          ?.forEach {

            /**
             * Each actual data file should have 4 uploaded files
             * - data
             * - data.md5
             * - data.asc
             * - data.md5.asc
             */
            val destProjectDir = File(dest, "${moduleDir.name}/${versionDirectory.name}")
            destProjectDir.mkdirs()

            val dataFile = File(destProjectDir, it.name)
            var dataHasChanged = false
            if (it.extension == "pom") {
              val newPom = it.fixIfNeeded(
                projectUrl = pomProjectUrl,
                licenseUrl = pomLicenseUrl,
                licenseName = pomLicenseName,
                developerName = pomDeveloperName,
                scmUrl = pomScmUrl,
                projectName = pomProjectName,
                description = pomDescription
              )
              if (newPom != null) {
                dataHasChanged = true
                dataFile.writeText(newPom)
              } else {
                it.copyTo(dataFile)
              }
            } else {
              it.copyTo(dataFile)
            }

            val md5File = File(destProjectDir, it.name + ".md5")
            val originalMd5 = File(it.absolutePath + ".md5")
            if (dataHasChanged || !originalMd5.exists()) {
              //println("adding ${md5File.name}")
              val md5 = dataFile.source().buffer().md5()
              md5File.writeText(md5)
            } else {
              originalMd5.copyTo(md5File)
            }

            val ascFile = File(destProjectDir, it.name + ".asc")
            val originalAsc = File(it.absolutePath + ".asc")
            if (dataHasChanged || !originalAsc.exists()) {
              //println("adding ${ascFile.name}")
              val asc = dataFile.source().buffer().sign(privateKey, privateKeyPassword)
              ascFile.writeText(asc)
            } else {
              originalAsc.copyTo(ascFile)
            }

            /**
             * This doesn't seem to be required and I'm not sure how that works
             * Jcenter has some .md5.asc but maven-publish has .asc.md5
             * Leave it out for now
             */
            /*val md5AscFile = File(destProjectDir, it.name + ".md5.asc")
            val originalMd5Asc = File(it.absolutePath + ".md5.asc")
            if (dataHasChanged || !originalMd5Asc.exists()) {
              //println("adding ${ascMd5File.name}")
              val asc = ascFile.source().buffer().sign(privateKey, privateKeyPassword)
              md5AscFile.writeText(asc)
            } else {
              originalMd5Asc.copyTo(md5AscFile)
            }*/
          }
      }
    }
    println("")
  }
}