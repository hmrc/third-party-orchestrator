import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val commonDomainVersion = "0.13.0"
  private val applicationEventVersion  = "0.49.0"

  def apply(): Seq[ModuleID] = compileDeps ++ testDeps

  val compileDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30"       % bootstrapVersion,
    "uk.gov.hmrc" %% "http-metrics"                    % "2.8.0",
    "uk.gov.hmrc" %% "api-platform-application-events" % applicationEventVersion
  )

  val testDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"          % bootstrapVersion,
    "uk.gov.hmrc" %% "api-platform-test-common-domain" % commonDomainVersion
  ).map(_ % "test")
}
