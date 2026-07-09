import sbt.*

object AppDependencies {

  private val bootstrapVersion    = "10.7.0"
  private val commonDomainVersion = "1.1.0"
  private val tpdDomainVersion    = "1.0.0"

  private val appEventVersion     = "1.1.0" // Ensure this version of the application-events library uses the appDomainVersion above
  private val appDomainVersion    = "1.3.0"
  private val mockitoScalaVersion = "2.2.1"

  def apply(): Seq[ModuleID] = compileDeps ++ testDeps

  val compileDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30"       % bootstrapVersion,
    // "uk.gov.hmrc" %% "api-platform-application-events" % appEventVersion,
    // Use these during poc stage of development
    "uk.gov.hmrc" %% "api-platform-application-events" % appEventVersion exclude ("uk.gov.hmrc", "api-platform-application-domain"),
    "uk.gov.hmrc" %% "api-platform-application-domain" % appDomainVersion,
    "uk.gov.hmrc" %% "api-platform-tpd-domain"         % tpdDomainVersion
  )

  val testDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"                   % bootstrapVersion,
    "uk.gov.hmrc" %% "api-platform-test-tpd-domain"             % tpdDomainVersion,
    "uk.gov.hmrc" %% "api-platform-application-domain-fixtures" % appDomainVersion,
    "org.mockito" %% "mockito-scala-scalatest"                  % mockitoScalaVersion
  ).map(_ % "test")
}
