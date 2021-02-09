package net.mbonnin.vespene.lib.pom


import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun File.fixIfNeeded(
  licenseName: String?,
  licenseUrl: String?,
  projectUrl: String?,
  scmUrl: String?,
  developerName: String?

): String? {
  val factory = DocumentBuilderFactory.newInstance()
  val builder = factory.newDocumentBuilder()
  var hasChanged = false

  val document = builder.parse(this)
  val projectNode = document.childNodes.item(0)
  val url = projectNode.childNodes.toList().firstOrNull {
    (it as? Element)?.tagName == "url"
  }
  if (url == null && projectUrl != null) {
    hasChanged = true
    //println("adding url to $name")
    projectNode.appendChild(document.createElement("url").apply {
      textContent = projectUrl
    })
  }
  val licenses = projectNode.childNodes.toList().firstOrNull {
    (it as? Element)?.tagName == "licenses"
  }

  check (licenseUrl == null && licenseName == null || licenseName != null && licenseUrl != null) {
    "if you provide licenseUrl, you must provide licenseName, and vice-versa"
  }
  if (licenses == null && licenseUrl != null && licenseName != null) {
    hasChanged = true
    //println("adding license to $name")
    projectNode.appendChild(document.createElement("licenses").also { licenses ->
      licenses.appendChild(document.createElement("license").also { license ->
        license.appendChild(document.createElement("name").also { name ->
          name.textContent = licenseName
        })
        license.appendChild(document.createElement("url").also { url ->
          url.textContent = licenseUrl
        })
      })
    })
  }
  val scm = projectNode.childNodes.toList().firstOrNull {
    (it as? Element)?.tagName == "scm"
  }
  if (scm == null && scmUrl != null) {
    hasChanged = true
    //println("adding scm to $name")
    projectNode.appendChild(document.createElement("scm").also { scm ->
      scm.appendChild(document.createElement("url").also { url ->
        url.textContent = scmUrl
      })
    })
  }
  val developers = projectNode.childNodes.toList().firstOrNull {
    (it as? Element)?.tagName == "developers"
  }
  if (developers == null && developerName != null) {
    hasChanged = true
    //println("adding developers to $name")
    projectNode.appendChild(document.createElement("developers").also { developers ->
      developers.appendChild(document.createElement("developer").also { developer ->
        developer.appendChild(document.createElement("name").also { name ->
          name.textContent = developerName
        })
      })
    })
  }

  return if (hasChanged) {
    val transformer = TransformerFactory.newInstance().newTransformer()
    val outputStream = ByteArrayOutputStream()
    val result = StreamResult(outputStream.writer())
    transformer.transform(DOMSource(document), result)

    outputStream.flush()
    String(outputStream.toByteArray())
  } else {
    return null
  }
}

private fun NodeList.toList() = 0.until(length).map { item(it) }