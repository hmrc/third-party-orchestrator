import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.7.0"
  private val commonDomainVersion = "0.18.0"
  private val tpdDomainVersion  = "0.11.0"

  private val appDomainVersion = "0.72.0"
  private val applicationEventVersion  = "0.75.0" // Ensure this version of the application-events library uses the appDomainVersion above

  def apply(): Seq[ModuleID] = compileDeps ++ testDeps

  val compileDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30"       % bootstrapVersion,
    "uk.gov.hmrc" %% "http-metrics"                    % "2.8.0",
    "uk.gov.hmrc" %% "api-platform-application-events" % applicationEventVersion,
    "uk.gov.hmrc" %% "api-platform-tpd-domain"         % tpdDomainVersion,
  )

  val testDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"                      % bootstrapVersion,
    "uk.gov.hmrc" %% "api-platform-test-tpd-domain"                % tpdDomainVersion,
    "uk.gov.hmrc" %% "api-platform-application-domain-fixtures"    % appDomainVersion
  ).map(_ % "test")
}
