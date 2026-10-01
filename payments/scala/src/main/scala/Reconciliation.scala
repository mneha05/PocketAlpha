import scala.collection.mutable

final case class Settlement(
    paymentId: String,
    grossCents: Long,
    feeCents: Long,
    netCents: Long,
    status: String
)

final case class LedgerPayment(
    paymentId: String,
    capturedCents: Long,
    refundedCents: Long
):
  def expectedNetCents: Long = capturedCents - refundedCents

enum ReconciliationIssue:
  case MissingSettlement(paymentId: String)
  case OrphanSettlement(paymentId: String)
  case DuplicateSettlement(paymentId: String, count: Int)
  case ArithmeticMismatch(paymentId: String, grossCents: Long, feeCents: Long, netCents: Long)
  case AmountMismatch(paymentId: String, expectedCents: Long, settledGrossCents: Long)

final case class ReconciliationReport(
    matched: Int,
    issues: Vector[ReconciliationIssue]
):
  def clean: Boolean = issues.isEmpty

object Reconciler:
  def reconcile(
      ledger: Seq[LedgerPayment],
      settlements: Seq[Settlement]
  ): ReconciliationReport =
    val issues = Vector.newBuilder[ReconciliationIssue]
    val settlementGroups = settlements.groupBy(_.paymentId)

    settlementGroups.foreach { case (paymentId, rows) =>
      if rows.size > 1 then
        issues += ReconciliationIssue.DuplicateSettlement(paymentId, rows.size)
    }

    val ledgerById = ledger.map(payment => payment.paymentId -> payment).toMap
    var matched = 0

    ledger.foreach { payment =>
      settlementGroups.get(payment.paymentId) match
        case None =>
          issues += ReconciliationIssue.MissingSettlement(payment.paymentId)

        case Some(rows) =>
          val settlement = rows.head

          if settlement.grossCents - settlement.feeCents != settlement.netCents then
            issues += ReconciliationIssue.ArithmeticMismatch(
              payment.paymentId,
              settlement.grossCents,
              settlement.feeCents,
              settlement.netCents
            )

          if payment.expectedNetCents != settlement.grossCents then
            issues += ReconciliationIssue.AmountMismatch(
              payment.paymentId,
              payment.expectedNetCents,
              settlement.grossCents
            )
          else if rows.size == 1 && settlement.grossCents - settlement.feeCents == settlement.netCents then
            matched += 1
    }

    settlements.foreach { settlement =>
      if !ledgerById.contains(settlement.paymentId) then
        issues += ReconciliationIssue.OrphanSettlement(settlement.paymentId)
    }

    ReconciliationReport(matched, issues.result())

object SettlementCsv:
  def parse(text: String): Vector[Settlement] =
    text.linesIterator
      .drop(1)
      .filter(_.trim.nonEmpty)
      .map { line =>
        val fields = line.split(",", -1).map(_.trim)
        require(fields.length == 5, s"expected 5 columns, got ${fields.length}: $line")

        Settlement(
          paymentId = fields(0),
          grossCents = fields(1).toLong,
          feeCents = fields(2).toLong,
          netCents = fields(3).toLong,
          status = fields(4)
        )
      }
      .toVector
