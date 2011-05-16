#!/bin/sh
exec bin/scala -classpath bin -deprecation -nocompdaemon "$0" "$@"
!# 
// Local Variables:
// mode: scala
// End:

/// count lines of code in NetLogo source tree

import Scripting.shell

val autogenerated = Set("src/main/org/nlogo/lex/TokenLexer.java",
                        "src/main/org/nlogo/agent/ImportLexer.java",
                        "src/main/org/nlogo/window/Events.java",
                        "src/main/org/nlogo/app/Events.java")
val format = "%25s: %6d unique, %6d total  (%3d%% Scala)\n"
var tj, tuj, ts, tus = 0 // totals. u = unique, j = Java, s = Scala

def percent(n: Int, d: Int) =
  math.ceil(100 * n / d).toInt
// for extensions we don't want to break it out into packages, we just
// want to "flatten" all the packages into one entry
def names(dir: String, extension: String, flatten: Boolean) =
  shell("find '" + dir + "' -name *." + extension + (if (flatten) "" else " -depth 1"))
    .filterNot(autogenerated)
def lines(dir: String, extension: String, flatten: Boolean) =
  shell("cat /dev/null " + names(dir, extension, flatten).mkString(" ") + " || cat /dev/null")
    .map(_.trim)
    .toList
def dirs(root: String) =
  shell("find " + root + """ -type d | grep -v \\.svn || grep -v \\.git""")
    .filterNot(_.containsSlice("/build/"))
    .filterNot(_.matches("extensions/.*/src/.*/.*"))

def outputLines(root: String) =
  for{dir <- dirs(root)
      flatten = dir.matches("extensions/.*/src/.*")
      j = lines(dir, "java", flatten)
      uj = j.distinct
      s = lines(dir, "scala", flatten)
      us = s.distinct
      if j.nonEmpty || s.nonEmpty}
  yield {
    tj += j.size; tuj += uj.size; ts += s.size; tus += us.size
    format.format(dir.replaceAll(root + "/org/nlogo/", "")
                     .replaceAll(".src.org", "")
                     .replaceAll(root + "/", "")
                     .replaceFirst(".src$", "")
                     .replaceAll("/", "."),
                  uj.size + us.size, j.size + s.size,
                  percent(us.size, uj.size + us.size))
  }
def firstNumber(s: String) =
  s.dropWhile(!_.isDigit).takeWhile(_.isDigit).mkString.toInt
def sortAndPrint(root: String) = {
  println(root + ":")
  outputLines(root).toList.sortBy(firstNumber).reverse.foreach(print)
  println()
}
sortAndPrint("src/main")
sortAndPrint("src/test")
sortAndPrint("src/tools")
sortAndPrint("extensions")
sortAndPrint("project/build")
sortAndPrint("bin")
println(format.format("TOTAL",
                      tuj + tus, tj + ts,
                      percent(tus, tuj + tus)))
