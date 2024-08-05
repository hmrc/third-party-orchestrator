import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.1.0"
  private val commonDomainVersion = "0.15.0"
  private val applicationEventVersion  = "0.63.0"
  private val tpdDomainVersion  = "0.2.0"

  def apply(): Seq[ModuleID] = compileDeps ++ testDeps

  val compileDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30"       % bootstrapVersion,
    "uk.gov.hmrc" %% "http-metrics"                    % "2.8.0",
    "uk.gov.hmrc" %% "api-platform-application-events" % applicationEventVersion,
    "uk.gov.hmrc" %% "api-platform-tpd-domain" % tpdDomainVersion,
  )

  val testDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"          % bootstrapVersion,
    "uk.gov.hmrc" %% "api-platform-test-tpd-domain" % tpdDomainVersion
  ).map(_ % "test")
}
