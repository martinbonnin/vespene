package net.mbonnin.vespene.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking
import net.mbonnin.vespene.lib.NexusStagingClient
import net.mbonnin.vespene.lib.md5
import net.mbonnin.vespene.lib.pom.fixIfNeeded
import net.mbonnin.vespene.sign
import okio.buffer
import okio.source
import java.io.File
import java.lang.IllegalArgumentException
import kotlin.system.exitProcess


