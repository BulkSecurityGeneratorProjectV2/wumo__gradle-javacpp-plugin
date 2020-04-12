package org.bytedeco.javacpp.tools

import org.bytedeco.javacpp.Loader
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

internal data class Target(
  val packagePath: String,
  val jniLibName: String,
  val link: List<String>,
  val cppFiles: List<String>
)

internal class Build(val logger: Logger = Logger.create(Builder::class.java)) {
  companion object {
    private val properties: Properties = run {
      System.setProperty("org.bytedeco.javacpp.loadlibraries", "false")
      Loader.loadProperties()
    }
    
    val platform = Loader.getPlatform()
    val libraryPrefix = properties.getProperty("platform.library.prefix", "")
    val librarySuffix = properties.getProperty("platform.library.suffix", "")
  }
  
  /** Logger where to send debug, info, warning, and error messages.  */
  /** The name of the character encoding used for input files as well as output files.  */
  var encoding: String? = null
  
  /** The directory where the generated files and compiled shared libraries get written to.
   * By default they are placed in the same directory as the `.class` file.  */
  var outputDirectory: File? = null
  
  /** The name of the output generated source file or shared library. This enables single-
   * file output mode. By default, the top-level enclosing classes get one file each.  */
  var outputName: String? = null
  
  /** If true, deletes all files from [.outputDirectory] before writing anything in it.  */
  var clean = false
  
  /** If true, attempts to generate C++ JNI files, but if false, only attempts to org.bytedeco.javacpp.parse header files.  */
  var generate = true
  
  /** If true, also generates C++ header files containing declarations of callback functions.  */
  var header = false
  
  /** Accumulates the various properties loaded from resources, files, command line options, etc.  */
  var properties: Properties = run {
    System.setProperty("org.bytedeco.javacpp.loadlibraries", "false")
    Loader.loadProperties()
  }
  
  /** The instance of the [ClassScanner] that fills up a [Collection] of [Class] objects to process.  */
  var classScanner: ClassScanner = ClassScanner(
    logger, ArrayList(),
    UserClassLoader(Thread.currentThread().contextClassLoader)
  )
  
  init {
    System.setProperty("org.bytedeco.javacpp.loadlibraries", "false")
  }
  
  private fun loadClassPath() {
    classScanner.addPackage(null, true)
  }
  
  fun classPaths(vararg classPaths: String) {
    classScanner.classLoader.addPaths(*classPaths)
  }
  
  fun outputDirectory(outputDirectory: String) {
    this.outputDirectory = File(outputDirectory)
  }
  
  fun property(key: String, value: String) {
    if (key.isNotEmpty() && value.isNotEmpty())
      properties[key] = value
  }
  
  private fun cleanOutputDirectory() {
    outputDirectory?.apply {
      if (isDirectory && clean) {
        logger.info("Deleting $outputDirectory")
        Files.walkFileTree(toPath(), object : SimpleFileVisitor<Path>() {
          override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult {
            if (e != null) throw e
            Files.delete(dir)
            return FileVisitResult.CONTINUE
          }
          
          override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.delete(file)
            return FileVisitResult.CONTINUE
          }
        })
      }
    }
  }
  
  private fun parse(classPath: Array<String>, cls: Class<*>): Array<File>? {
    cleanOutputDirectory()
    return Parser(logger, properties, encoding, null).parse(outputDirectory, classPath, cls)
  }
  
  fun generateJava() {
    loadClassPath()
    if (classScanner.getClasses().isEmpty())
      throw Exception("Empty classes!")
    
    for (c in classScanner.getClasses()) {
      if (Loader.getEnclosingClass(c) != c)
        continue
      
      // Do not inherit properties when parsing because it generates annotations itself
      val p = Loader.loadProperties(c, properties, false)
      if (p.isLoaded) {
        val target = p.getProperty("global")
        if (target != null && c.name != target) {
          var found = false
          for (c2 in classScanner.getClasses()) {
            // do not try to regenerate classes that are already scheduled for C++ compilation
            found = found or (c2.name == target)
          }
          if (!generate || !found) {
            parse(classScanner.classLoader.paths, c)
          }
          continue
        }
      }
    }
  }
  
  fun generateJNI(): List<Target> {
    loadClassPath()
    if (classScanner.getClasses().isEmpty()) throw Exception("Empty classes!")
    
    val targets = ArrayList<Target>()
    for (c in classScanner.getClasses()) {
      if (Loader.getEnclosingClass(c) != c)
        continue
      
      // Do not inherit properties when parsing because it generates annotations itself
      var p = Loader.loadProperties(c, properties, false)
      if (p.isLoaded) {
        val t = p.getProperty("global")
        if (t != null && c.name != t) continue
      }
      if (!p.isLoaded) {
        // Now try to inherit to generate C++ source files
        p = Loader.loadProperties(c, properties, true)
      }
      if (!p.isLoaded) error("Could not load platform properties for $c")
      
      val libraryName = outputName ?: p.getProperty("platform.library", "")
      if (libraryName.isEmpty()) continue
      
      val classArray = p.effectiveClasses.toTypedArray()
      val cppFiles = generate(classArray, libraryName)
      
      val link = p.get("platform.link").filterNot { it.trim().run { isEmpty() || endsWith('#') } }
      val library = p.getProperty("platform.library")
      targets.add(Target(c.`package`.name, library, link, cppFiles))
    }
    return targets
  }
  
  private fun generate(classes: Array<Class<*>>, outputName: String): List<String> {
    val outputPath = outputDirectory ?: error("outputDirectory shouldn't be null")
    val p = Loader.loadProperties(classes, properties, true)
    val sourceSuffix = p.getProperty("platform.source.suffix", ".cpp")
    val generator = Generator(logger, properties, encoding)
    val outputFiles = listOf("jnijavacpp$sourceSuffix", "$outputName$sourceSuffix")
    val sourceFilenames = outputFiles.map { outputPath.resolve(it).path }
    val headerFilenames = arrayOf(null, if (header) outputPath.resolve(outputName + ".h").path else null)
    val loadSuffixes = arrayOf("_jnijavacpp", null)
    val baseLoadSuffixes = arrayOf(null, "_jnijavacpp")
    var classPath = System.getProperty("java.class.path")
    for (s in classScanner.classLoader.paths)
      classPath += File.pathSeparator + s
    val classPaths = arrayOf(null, classPath)
    val classesArray = arrayOf(null, classes)
    for (i in sourceFilenames.indices) {
      logger.info("Generating " + sourceFilenames[i])
      val g = generator::generate
      val generated = g(
        sourceFilenames[i], headerFilenames[i],
        loadSuffixes[i], baseLoadSuffixes[i], classPaths[i], classesArray[i]
      )
      if (!generated)
        error("Nothing generated for " + sourceFilenames[i])
    }
    return outputFiles
  }
}