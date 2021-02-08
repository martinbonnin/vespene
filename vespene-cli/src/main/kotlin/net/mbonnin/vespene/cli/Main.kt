package net.mbonnin.vespene.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  MainCommand()
    .subcommands(UploadCommand())
    .subcommands(Close())
    .subcommands(Release())
    .subcommands(CloseAndRelease())
    .subcommands(Drop())
    .main(args)
}

class MainCommand : CliktCommand() {
  override fun run() {
  }
}

abstract class BaseCommand : CliktCommand() {
  private val username by option(help = "your nexus username. For OSSRH, this is your Sonatype jira username. Defaults to reading the 'SONATYPE_NEXUS_USERNAME' environment variable.")
  private val password by option(help = "your nexus password. For OSSRH, this is your Sonatype jira password. Defaults to reading the 'SONATYPE_NEXUS_PASSWORD' environment variable.")

  override fun run() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
      // OkHttp has threadpools that keep the process alive. kill everything
      e.printStackTrace()
      exitProcess(1)
    }

    val client = NexusStagingClient(
      username = username ?: System.getenv("SONATYPE_NEXUS_USERNAME")
      ?: throw IllegalArgumentException("Please specify username"),
      password = password ?: System.getenv("SONATYPE_NEXUS_PASSWORD")
      ?: throw IllegalArgumentException("Please specify password"),
    )
    runBlocking {
      run(client)
    }
    // OkHttp has threadpools that keep the process alive. kill everything
    exitProcess(0)
  }

  abstract suspend fun run(client: NexusStagingClient)
}

class UploadCommand : BaseCommand() {
  private val dir by argument(help = "the directory with the files to upload. It typically contains maven hierarchy of files like 'com/example/module/version/module-version.jar'")

  //private val closeAndRelease by option(help = "automatically close and release after the upload").flag()
  private val profileId by option(
    help = "your profileId. For OSSRH, this is what you see when you go to https://oss.sonatype.org/#stagingProfiles;\${profileId}." +
        " Defaults to reading the 'SONATYPE_NEXUS_PROFILE_ID' environment variable. Mandatory if the sonatype account has several profileIds."
  )

  override suspend fun run(client: NexusStagingClient) {
    val inputProfileId = profileId ?: System.getenv("SONATYPE_NEXUS_PROFILE_ID")
    val profileId = if (inputProfileId == null) {
      println("Looking up profileId...")
      val allIds = client.getProfiles()
      check(allIds.size == 1) {
        val prettyIds = allIds.map {
          "- ${it.name}: --staging-profile-id=${it.id}"
        }.joinToString("\n")
        "Multiple profileIds found. Use one of:\n${prettyIds}\n"
      }
      allIds.first().id
    } else {
      inputProfileId
    }

    println("Creating staging directory...")
    val repositoryId = client.upload(File(dir), profileId) { index, total, path ->
      println(String.format("%4d/%4d $path", index, total))
    }
    println("Files uploaded to staging repository '$repositoryId'")
  }
}

class Close : BaseCommand() {
  private val repositoryId by argument(help = "the repositoryId to close")

  override suspend fun run(client: NexusStagingClient) {
    client.closeRepositories(listOf(repositoryId))
    println("Repository closed.")
  }
}

class Release : BaseCommand() {
  private val dropAfterRelease by option().flag(default = true)
  private val repositoryId by argument(help = "the repositoryId to release")

  override suspend fun run(client: NexusStagingClient) {
    client.releaseRepositories(listOf(repositoryId), dropAfterRelease)
    println("Repository released.")
  }
}

class Drop : BaseCommand() {
  private val repositoryId by argument(help = "the repositoryId to drop")

  override suspend fun run(client: NexusStagingClient) {
    client.dropRepositories(listOf(repositoryId))
    println("Repository dropped.")
  }
}

class CloseAndRelease : BaseCommand() {
  private val dropAfterRelease by option().flag(default = true)
  private val repositoryId by argument(help = "the repositoryId to close")

  override suspend fun run(client: NexusStagingClient) {
    client.closeRepositories(listOf(repositoryId))

    println("waiting for repository to be closed")
    client.waitForClose(repositoryId = repositoryId, pollingIntervalMillis = 10_000) {
      print(".")
      System.out.flush()
    }
    println("")
    println("Repository closed, releasing...")
    client.releaseRepositories(listOf(repositoryId), dropAfterRelease)
    println("Repository closed and released.")
  }
}


