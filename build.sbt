import scala.language.postfixOps

lazy val commonSettings = Seq(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.9",
  organization := "org.ergoplatform",
  resolvers += Resolver.sonatypeRepo("public"),
  licenses := Seq("CC0" -> url("https://creativecommons.org/publicdomain/zero/1.0/legalcode")),
  description := "Playgrounds for modeling Ergo contracts",
  pomExtra :=
    <developers>
      <developer>
        <id>greenhat</id>
        <name>Denys Zadorozhnyi</name>
        <url>https://github.com/greenhat/</url>
      </developer>
    </developers>,
  publishMavenStyle := true,
  publishTo := sonatypePublishToBundle.value
)

// prefix version with "-SNAPSHOT" for builds without a git tag
dynverSonatypeSnapshots in ThisBuild := true
// use "-" instead of default "+"
dynverSeparator in ThisBuild := "-"

lazy val allConfigDependency = "compile->compile;test->test"

lazy val dependencies = Seq(
  "org.ergoplatform" %% "ergo-scala-compiler" % "0.0.0-24-c5373c88-SNAPSHOT",
)

lazy val testingDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % Test
)

lazy val root = project
  .in(file("."))
  .withId("ergo-playgrounds")
  .settings(commonSettings)
  .settings(publish / skip := true)
  .aggregate(playgrounds, playgroundsExamples)

lazy val playgrounds = project
  .in(file("playgrounds"))
  .withId("playgrounds")
  .settings(moduleName := "ergo-playgrounds")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= dependencies ++ testingDependencies
  )

lazy val playgroundsExamples = project
  .in(file("playgrounds-examples"))
  .withId("playgrounds-examples")
  .settings(moduleName := "ergo-playgrounds-examples")
  .settings(commonSettings)
  .dependsOn(playgrounds)
  .settings(
    libraryDependencies ++= dependencies ++ testingDependencies
  )

lazy val commonScalacOptions = List(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Yno-adapted-args",
)

// PGP key for signing a release build published to sonatype
// signing is done by sbt-pgp plugin
// how to generate a key - https://central.sonatype.org/pages/working-with-pgp-signatures.html
// how to export a key and use it with Travis - https://docs.scala-lang.org/overviews/contributors/index.html#export-your-pgp-key-pair
// pgpPublicRing := file("ci/pubring.asc")
// pgpSecretRing := file("ci/secring.asc")
// pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray)
// usePgpKeyHex("")

lazy val credentialFile = Path.userHome / ".sbt" / ".sigma-sonatype-credentials"
credentials in ThisBuild ++= (for {
  file <- if (credentialFile.exists) Some(credentialFile) else None
} yield Credentials(file)).toSeq

credentials in ThisBuild ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq

