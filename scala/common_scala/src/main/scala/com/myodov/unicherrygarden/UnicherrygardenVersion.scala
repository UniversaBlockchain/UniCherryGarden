package com.myodov.unicherrygarden

import java.io.IOException
import java.util.Properties

/**
 * The convenient accessor to the versions of each CherryGarden component,
 * which are stored in the sbt-generated `.properties` files.
 */
object UnicherrygardenVersion {
  def loadPropsFromNamedResource(resourceName: String): Properties = {
    val resourcePath = "unicherrygarden/" + resourceName
    val props = new Properties
    val resourceAsStream = ClassLoader.getSystemResourceAsStream(resourcePath)
    if (resourceAsStream == null) {
      System.err.printf("Cannot load resource from %s\n", resourcePath)
    } else {
      try props.load(resourceAsStream)
      catch {
        case e: IOException =>
          System.err.printf("Cannot load properties from %s: %s\n", resourcePath, e)
          throw new RuntimeException(String.format("Cannot load properties file from %s", resourcePath))
      }
    }
    props
  }
}
