scalaVersion := "2.11.7"

libraryDependencies ++= pdfs

lazy val pdfs = Seq("org.apache.pdfbox" % "pdfbox" % "2.0.0-RC1")