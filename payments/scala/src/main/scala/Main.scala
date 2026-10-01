@main def ledgerBridgeDemo(): Unit =
  val ledger = Vector(
    LedgerPayment("pay_001", capturedCents = 10_000, refundedCents = 0),
    LedgerPayment("pay_002", capturedCents = 7_500, refundedCents = 2_500),
    LedgerPayment("pay_003", capturedCents = 4_200, refundedCents = 0)
  )

  val csv =
    """payment_id,gross_cents,fee_cents,net_cents,status
      |pay_001,10000,320,9680,settled
      |pay_002,5000,175,4825,settled
      |pay_orphan,900,40,860,settled
      |""".stripMargin

  val report = Reconciler.reconcile(ledger, SettlementCsv.parse(csv))

  println(s"matched=${report.matched}")
  report.issues.foreach(issue => println(s"issue=$issue"))

  assert(report.matched == 2)
  assert(report.issues.exists(_.isInstanceOf[ReconciliationIssue.MissingSettlement]))
  assert(report.issues.exists(_.isInstanceOf[ReconciliationIssue.OrphanSettlement]))
