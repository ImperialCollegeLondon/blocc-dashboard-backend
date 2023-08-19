package uk.ac.ic.doc.blocc.dashboard.approvedtransaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.ac.ic.doc.blocc.dashboard.approvedtransaction.model.ApprovedTempReading;
import uk.ac.ic.doc.blocc.dashboard.approvedtransaction.model.ApprovedTransaction;
import uk.ac.ic.doc.blocc.dashboard.approvedtransaction.model.CompositeKey;
import uk.ac.ic.doc.blocc.dashboard.fabric.model.TemperatureHumidityReading;

public class ApprovedTransactionServiceTest {

  @InjectMocks
  private ApprovedTransactionService approvedTransactionService;

  @Mock
  private ApprovedTransactionRepository repository;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void getApprovedTempReadingsTest() {
    int containerNum = 1;
    List<ApprovedTransaction> mockData = Arrays.asList(
        new ApprovedTransaction(
            "tx123",
            1, List.of("Container5MSP", "Container6MSP"),
            new TemperatureHumidityReading(25, 0.3F, 100L)),

        new ApprovedTransaction(
            "tx1299",
            1, List.of("Container5MSP"),
            new TemperatureHumidityReading(22, 0.3F, 100L))
    );

    when(repository.findAllByContainerNum(containerNum)).thenReturn(mockData);

    List<ApprovedTempReading> result =
        approvedTransactionService.getApprovedTempReadings(containerNum);

    assertEquals(2, result.size());

    // Expected ApprovedTempReadings based on the mock data
    ApprovedTempReading expectedFirstReading = new ApprovedTempReading(100L, 25, 2, "tx123");
    ApprovedTempReading expectedSecondReading = new ApprovedTempReading(100L, 22, 1, "tx1299");

    // Assertions
    assertEquals(expectedFirstReading, result.get(0));
    assertEquals(expectedSecondReading, result.get(1));
  }

  @Test
  public void addsSensorChaincodeTransaction() {
    approvedTransactionService.addTempReading("tx6789", 1,
        30, 0.9F, 300L);

    // Create an ArgumentCaptor for ApprovedTransaction
    ArgumentCaptor<ApprovedTransaction> captor = ArgumentCaptor.forClass(ApprovedTransaction.class);

    verify(repository, times(1)).save(captor.capture());

    // Retrieve the captured argument
    ApprovedTransaction capturedTransaction = captor.getValue();

    // Now you can assert on the properties of the captured ApprovedTransaction
    assertEquals("tx6789", capturedTransaction.getTxId());
    assertEquals(1, capturedTransaction.getContainerNum());
    assertEquals(30, capturedTransaction.getReading().getTemperature());
    assertEquals(0.9F, capturedTransaction.getReading().getRelativeHumidity());
    assertEquals(300L, capturedTransaction.getReading().getTimestamp());
  }

  @Test
  public void throwsExceptionWhenSavingReadingToExistedTransaction() {
    // Given
    String txId = "tx6789";
    int containerNum = 1;
    float temperature = 30;
    float relativeHumidity = 0.9F;
    long timestamp = 300L;

    // Mock the repository to return an Optional containing a transaction when findById is called
    when(repository.findById(new CompositeKey(txId, containerNum))).thenReturn(
        Optional.of(new ApprovedTransaction()));

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> approvedTransactionService.addTempReading(txId, containerNum, temperature,
            relativeHumidity, timestamp)
    );

    // Assert the exception message
    assertEquals(String.format("Transaction %s exists", txId), exception.getMessage());
  }

  @Test
  public void addsSensorChaincodeTransactionWithReading() {
    TemperatureHumidityReading reading = new TemperatureHumidityReading(30, 0.9F, 300L);
    approvedTransactionService.addTempReading("tx6789", 1, reading);

    // Create an ArgumentCaptor for ApprovedTransaction
    ArgumentCaptor<ApprovedTransaction> captor = ArgumentCaptor.forClass(ApprovedTransaction.class);

    verify(repository, times(1)).save(captor.capture());

    // Retrieve the captured argument
    ApprovedTransaction capturedTransaction = captor.getValue();

    // Now you can assert on the properties of the captured ApprovedTransaction
    assertEquals("tx6789", capturedTransaction.getTxId());
    assertEquals(1, capturedTransaction.getContainerNum());
    assertEquals(30, capturedTransaction.getReading().getTemperature());
    assertEquals(0.9F, capturedTransaction.getReading().getRelativeHumidity());
    assertEquals(300L, capturedTransaction.getReading().getTimestamp());
  }

  @Test
  public void throwsExceptionWhenSavingReadingObjectToExistedTransaction() {
    // Given
    String txId = "tx6789";
    TemperatureHumidityReading reading = new TemperatureHumidityReading(
        30, 0.9F, 300L);

    int containerNum = 1;

    // Mock the repository to return an Optional containing a transaction when findById is called
    when(repository.findById(new CompositeKey(txId, containerNum))).thenReturn(
        Optional.of(new ApprovedTransaction()));

    // When & Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> approvedTransactionService.addTempReading(txId, containerNum, reading)
    );

    // Assert the exception message
    assertEquals(String.format("Transaction %s for container %d exists", txId, containerNum),
        exception.getMessage());
  }

  @Test
  public void approvesExistingTransaction() {
    // Given
    String txId = "12345";
    int containerNum = 1;
    String approvingMspId = "Container5MSP";
    ApprovedTransaction approvedTransaction =
        new ApprovedTransaction(txId, 1,
            new TemperatureHumidityReading()); // Assuming there's a default constructor or use another appropriate constructor

    when(repository.findById(new CompositeKey(txId, containerNum))).thenReturn(
        Optional.of(approvedTransaction));

    // When
    approvedTransactionService.approveTransaction(txId, containerNum, approvingMspId);

    // Then
    assertTrue(
        approvedTransaction.getApprovingMspIds().contains(approvingMspId));
    verify(repository).save(approvedTransaction);
  }

  @Test
  public void throwsExceptionWhenApprovingNonExistedTransaction() {
    // Given
    String txId = "12345";
    int containerNum = 1;
    String approvingMspId = "Container5MSP";

    when(repository.findById(new CompositeKey(txId, containerNum))).thenReturn(Optional.empty());

    // When & Then
    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> approvedTransactionService.approveTransaction(txId, containerNum, approvingMspId));

    assertEquals(String.format("Transaction %s for container %d is not found", txId, containerNum),
        exception.getMessage());
  }

  @Test
  public void transactionRollbacksWhenApprovingAlreadyApprovedTransaction() {
    // Given
    String txId = "12345";
    int containerNum = 1;
    String approvingMspId = "Container5MSP";
    ApprovedTransaction approvedTransaction =
        new ApprovedTransaction(txId, containerNum,
            new TemperatureHumidityReading());

    // Mock the repository to return the transaction
    when(repository.findById(new CompositeKey(txId, containerNum))).thenReturn(
        Optional.of(approvedTransaction));

    // Mock the transaction to already have the approvingMspId
    approvedTransaction.approve(approvingMspId);

    // When & Then
    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> approvedTransactionService.approveTransaction(txId, containerNum, approvingMspId));

    assertEquals(String.format("%s has already approved this transaction", approvingMspId),
        exception.getMessage());

    // Verify that the repository's save method was never called, indicating a rollback
    verify(repository, times(0)).save(any(ApprovedTransaction.class));
  }

}