import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val commonDomainVersion = "0.18.0"
  private val tpdDomainVersion  = "0.13.0"
  
  private val appDomainVersion = "0.82.0"
  private val appEventVersion  = "0.83.0" // Ensure this version of the application-events library uses the appDomainVersion above

  def apply(): Seq[ModuleID] = compileDeps ++ testDeps

  val compileDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30"       % bootstrapVersion,
    "uk.gov.hmrc" %% "http-metrics"                    % "2.9.0",
    "uk.gov.hmrc" %% "api-platform-application-events" % appEventVersion,
    "uk.gov.hmrc" %% "api-platform-tpd-domain"         % tpdDomainVersion
  )

  val testDeps = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"                      % bootstrapVersion,
    "uk.gov.hmrc" %% "api-platform-test-tpd-domain"                % tpdDomainVersion,
    "uk.gov.hmrc" %% "api-platform-application-domain-fixtures"    % appDomainVersion
  ).map(_ % "test")
}
