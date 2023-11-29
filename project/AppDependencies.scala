
import play.core.PlayVersion
import play.sbt.PlayImport.*
import sbt.Keys.libraryDependencies
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "7.22.0"
  private val enumeratumVersion = "1.7.0"
  lazy val scalaCheckVersion = "1.15.4"

  def apply(): Seq[ModuleID] = compileDeps ++ testDeps

  val compileDeps = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"       % bootstrapVersion,
    "uk.gov.hmrc"             %% "http-metrics"                    % "2.7.0",
    "uk.gov.hmrc"             %% "api-platform-application-domain" % "0.25.0"
  )

   lazy val testScopes = Seq(Test.name, IntegrationTest.name).mkString(",")

  val testDeps = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"          % bootstrapVersion,
    "org.mockito"             %% "mockito-scala-scalatest"         % "1.17.22",
    "org.scalatest"           %% "scalatest"                       % "3.2.17",
    "com.vladsch.flexmark"    % "flexmark-all"                     % "0.62.2" 

  ).map(_ % testScopes)
}
