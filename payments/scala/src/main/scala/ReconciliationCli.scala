import scala.io.Source

object LedgerCsv:
  def parse(text: String): Vector[LedgerPayment] =
    text.linesIterator
      .drop(1)
      .filter(_.trim.nonEmpty)
      .map { line =>
        val fields = line.split(",", -1).map(_.trim)
        require(fields.length == 3, s"expected 3 columns, got ${fields.length}: $line")
        LedgerPayment(
          paymentId = fields(0),
          capturedCents = fields(1).toLong,
          refundedCents = fields(2).toLong
        )
      }
      .toVector

@main def reconcileFiles(internalPaymentsPath: String, settlementsPath: String): Unit =
  def read(path: String): String =
    val source = Source.fromFile(path)
    try source.mkString
    finally source.close()

  val ledger = LedgerCsv.parse(read(internalPaymentsPath))
  val settlements = SettlementCsv.parse(read(settlementsPath))
  val report = Reconciler.reconcile(ledger, settlements)

  println(s"ledger_payments=${ledger.size}")
  println(s"settlements=${settlements.size}")
  println(s"matched=${report.matched}")
  println(s"issues=${report.issues.size}")
  report.issues.foreach(issue => println(s"issue=$issue"))

  if !report.clean then sys.exit(2)
