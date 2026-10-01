@main def reconciliationCheck(): Unit =
  val cleanLedger = Vector(
    LedgerPayment("pay_a", 10_000, 0),
    LedgerPayment("pay_b", 8_000, 3_000)
  )

  val cleanSettlements = Vector(
    Settlement("pay_a", 10_000, 300, 9_700, "settled"),
    Settlement("pay_b", 5_000, 150, 4_850, "settled")
  )

  val clean = Reconciler.reconcile(cleanLedger, cleanSettlements)
  assert(clean.clean)
  assert(clean.matched == 2)

  val dirty = Reconciler.reconcile(
    cleanLedger,
    Vector(
      Settlement("pay_a", 9_500, 300, 9_100, "settled"),
      Settlement("pay_a", 9_500, 300, 9_200, "settled"),
      Settlement("pay_orphan", 1_000, 30, 970, "settled")
    )
  )

  assert(dirty.issues.exists {
    case ReconciliationIssue.DuplicateSettlement("pay_a", 2) => true
    case _ => false
  })
  assert(dirty.issues.exists(_.isInstanceOf[ReconciliationIssue.AmountMismatch]))
  assert(dirty.issues.exists(_.isInstanceOf[ReconciliationIssue.ArithmeticMismatch]))
  assert(dirty.issues.exists(_.isInstanceOf[ReconciliationIssue.MissingSettlement]))
  assert(dirty.issues.exists(_.isInstanceOf[ReconciliationIssue.OrphanSettlement]))

  println(s"reconciliation checks passed: ${dirty.issues.size} expected issues detected")
