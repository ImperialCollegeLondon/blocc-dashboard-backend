package uk.ac.ic.doc.blocc.dashboard.transaction.model;

public record ApprovedTempReading(long timestamp, float temperature, int approvals, String txId) {

}
