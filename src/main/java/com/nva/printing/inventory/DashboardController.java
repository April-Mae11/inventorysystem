package com.nva.printing.inventory;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
// comparator import removed (unused)

// Apache POI imports (used by other controllers in project)
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Simple dashboard controller that shows inventory statistics and recent transactions.
 */
public class DashboardController {

	@FXML private Label totalItemsLabel;
	@FXML private Label lowStockItemsLabel;
	@FXML private Label totalValueLabel;
	@FXML private Label totalQuantityLabel;

	@FXML private TableView<TransactionLogger.TransactionRecord> transactionsTable;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, String> transactionIdColumn;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, String> dateColumn;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, String> itemColumn;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, String> unitPriceColumn;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, String> totalPriceColumn;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, String> typeColumn;
	@FXML private TableColumn<TransactionLogger.TransactionRecord, Integer> qtyColumn;

	@FXML private Button refreshButton;
	@FXML private Label footerText;
	@FXML private Label footerTime;
	// footerTime removed per UI change: no date shown in footer
	@FXML private Label totalRevenueLabel;
	@FXML private MenuButton lowStockMenu;
	@FXML private Label paymentsCashLabel;
	@FXML private Label paymentsGcashLabel;
	@FXML private Label itemsSoldLabel;

	private final DataManager dataManager = DataManager.getInstance();
	private final TransactionLogger txnLogger = TransactionLogger.getInstance();

	// Use 12-hour clock with AM/PM for the Date column (no seconds)
	private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
	private final Timer refresher = new Timer(true);
	// Timeline for updating footer time in the POS Overview
	private javafx.animation.Timeline footerClock;

	@FXML
	public void initialize() {
		// Configure table columns
		// Transaction ID column: show e-wallet transaction reference when present (GCash),
		// otherwise show "CASH" for sales paid by cash. For non-POS records leave blank.
		transactionIdColumn.setCellValueFactory(cell -> {
			TransactionLogger.TransactionRecord r = cell.getValue();
			String id = "";
			try {
				if (r != null) {
					String userDesc = r.getUser();
					// userDesc often contains markers like [Tx:...][ERef:...]
					if (userDesc != null) {
						int epos = userDesc.indexOf("[ERef:");
						if (epos >= 0) {
							int end = userDesc.indexOf(']', epos);
							if (end > epos) {
								id = userDesc.substring(epos + 6, end);
							}
						} else if ("SALE".equalsIgnoreCase(r.getType())) {
							// SALE without ERef -> assume cash
							id = "CASH";
						}
					}
				}
			} catch (Exception ignored) {}
			return new javafx.beans.property.SimpleStringProperty(id == null ? "" : id);
		});

		// Date column: format the LocalDateTime from the record
		dateColumn.setCellValueFactory(cell -> {
			TransactionLogger.TransactionRecord r = cell.getValue();
			String s = "";
			try {
				if (r != null && r.getDate() != null) s = dtf.format(r.getDate());
			} catch (Exception ignored) {} 
			return new javafx.beans.property.SimpleStringProperty(s);
		});
		itemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
		// Unit Price column: lookup inventory item by name and show formatted unit price
		unitPriceColumn.setCellValueFactory(cell -> {
			TransactionLogger.TransactionRecord r = cell.getValue();
			String s = "";
			try {
				if (r != null && r.getItemName() != null) {
					// Prefer stored unit price in the transaction record; fall back to current inventory lookup
					double up = r.getUnitPrice();
					if (up > 0.0) {
						s = String.format("₱%,.2f", up);
					} else {
						InventoryItem itm = dataManager.getAllItems().stream()
							.filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(r.getItemName()))
							.findFirst().orElse(null);
						if (itm != null) s = String.format("₱%,.2f", itm.getUnitPrice());
					}
				}
			} catch (Exception ignored) {}
			return new javafx.beans.property.SimpleStringProperty(s);
		});
		// Total Price column: unit price * quantity for SALE/pos records; formatted
		totalPriceColumn.setCellValueFactory(cell -> {
			TransactionLogger.TransactionRecord r = cell.getValue();
			String s = "";
			try {
				if (r != null && r.getItemName() != null) {
					// Prefer stored totalPrice in record, else compute from stored unitPrice or lookup
					double tp = r.getTotalPrice();
					if (tp > 0.0) {
						s = String.format("₱%,.2f", tp);
					} else if (r.getUnitPrice() > 0.0) {
						double total = r.getUnitPrice() * Math.max(0, r.getQuantity());
						s = String.format("₱%,.2f", total);
					} else {
						InventoryItem itm = dataManager.getAllItems().stream()
							.filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(r.getItemName()))
							.findFirst().orElse(null);
						if (itm != null) {
							double total = itm.getUnitPrice() * Math.max(0, r.getQuantity());
							s = String.format("₱%,.2f", total);
						}
					}
				}
			} catch (Exception ignored) {}
			return new javafx.beans.property.SimpleStringProperty(s);
		});
		// Type column: if the record represents a POS sale, display payment method (GCash/Cash)
		typeColumn.setCellValueFactory(cell -> {
			TransactionLogger.TransactionRecord r = cell.getValue();
			String txt = "";
			try {
				if (r != null) {
					// if user description contains ERef -> GCash
					String userDesc = r.getUser();
					if (userDesc != null && userDesc.contains("[ERef:")) {
						txt = "GCash";
					} else if ("SALE".equalsIgnoreCase(r.getType())) {
						txt = "Cash";
					} else {
						// fallback to the original type for non-sales
						txt = r.getType() == null ? "" : r.getType();
					}
				}
			} catch (Exception ignored) {}
			return new javafx.beans.property.SimpleStringProperty(txt);
		});
		qtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

	// Use refreshTransactions() ordering (we sort data in code). Disable column sorting
	// so the UI header does not show the sort arrow next to Item.
	itemColumn.setSortable(false);

		// Show initial data
		refreshMetrics();
		refreshTransactions();

		// Listen for new transactions so the overview updates in real-time
		try {
			txnLogger.getRecords().addListener((javafx.collections.ListChangeListener<TransactionLogger.TransactionRecord>) change -> {
				Platform.runLater(() -> {
					refreshTransactions();
					// attempt to select the top (most recent) visible row after refresh
					if (!transactionsTable.getItems().isEmpty()) {
						transactionsTable.getSelectionModel().selectFirst();
						transactionsTable.scrollTo(0);
					}
				});
			});
		} catch (Exception ignored) {}

		// Periodic refresh every 30 seconds (background)
			refresher.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					Platform.runLater(() -> {
						refreshMetrics();
						refreshTransactions();
					});
				}
			}, 30000, 30000);

		// Start footer clock (updates every second)
		try {
			footerClock = new javafx.animation.Timeline(
				new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> {
					try {
						if (footerTime != null) footerTime.setText(java.time.LocalDateTime.now().format(dtf));
					} catch (Exception ignored) {}
				})
			);
			footerClock.setCycleCount(javafx.animation.Animation.INDEFINITE);
			footerClock.play();
		} catch (Exception ignored) {}

		refreshButton.setOnAction(e -> {
			refreshMetrics();
			refreshTransactions();
		});

		// Clear recent transactions handler (button added in FXML)
		try {
			if (this.getClass().getDeclaredField("clearRecentButton") != null) {
				// nothing; the button will be wired via FXML to handleClearRecentTransactions
			}
		} catch (Exception ignored) {}
	}

	/**
	 * Remove transaction markers like [Tx:...] and [ERef:...] from a user description,
	 * returning the plain username (trimmed). If no username present, returns empty string.
	 */
	private String stripMarkers(String userDesc) {
		if (userDesc == null) return "";
		String s = userDesc;
		try {
			// remove any [Tx:....] and [ERef:....] occurrences
			s = s.replaceAll("\\[Tx:[^\\]]*\\]", "");
			s = s.replaceAll("\\[ERef:[^\\]]*\\]", "");
			// also remove any leftover square brackets
			s = s.replaceAll("\\[|\\]", "");
			// trim whitespace and punctuation
			s = s.trim();
			if (s.endsWith("-") || s.endsWith(":") || s.endsWith(",")) s = s.substring(0, s.length() - 1).trim();
		} catch (Exception ignored) {}
		return s == null ? "" : s;
	}

	private void refreshMetrics() {
		DataManager.InventoryStats stats = dataManager.getInventoryStats();
		totalItemsLabel.setText(String.valueOf(stats.getTotalItems()));
		// low stock menu: show count and populate dropdown
		int lowCount = stats.getLowStockItems();
		try {
			if (lowStockMenu != null) {
				lowStockMenu.getItems().clear();
				java.util.List<InventoryItem> low = dataManager.getLowStockItems();
				for (InventoryItem it : low) {
					String label = String.format("%s (%d left)", it.getName(), it.getQuantity());
					MenuItem mi = new MenuItem(label);
					lowStockMenu.getItems().add(mi);
				}
				lowStockMenu.setText(lowCount + (lowCount == 1 ? " item" : " items"));
			}
		} catch (Exception ignored) {}
		totalValueLabel.setText(String.format("₱%,.2f", stats.getTotalValue()));
		totalQuantityLabel.setText(String.valueOf(stats.getTotalQuantity()));
		// default items sold label (will be recomputed in refreshTransactions)
		if (itemsSoldLabel != null) itemsSoldLabel.setText("0");
	}

	private void refreshTransactions() {
		ObservableList<TransactionLogger.TransactionRecord> records = FXCollections.observableArrayList(txnLogger.getRecords());
		// Show latest 10 payment-related transactions (SALE or ERef), then sort alphabetically A->Z by item
		List<TransactionLogger.TransactionRecord> latest = records.stream()
				.filter(r -> {
					if (r == null) return false;
					if ("SALE".equalsIgnoreCase(r.getType())) return true;
					String ud = r.getUser();
					return ud != null && ud.contains("[ERef:");
				})
				.sorted((a, b) -> {
					String an = a == null || a.getItemName() == null ? "" : a.getItemName().toLowerCase();
					String bn = b == null || b.getItemName() == null ? "" : b.getItemName().toLowerCase();
					return an.compareTo(bn);
				})
				.limit(10)
				.toList();
		transactionsTable.getItems().setAll(latest);
		// compute total revenue from the displayed transactions by looking up unit price
		double totalRevenue = 0.0;
		java.util.List<InventoryItem> allItems = dataManager.getAllItems();
		for (TransactionLogger.TransactionRecord rec : latest) {
			try {
				int qty = rec.getQuantity();
				String itemName = rec.getItemName();
				if (itemName != null) {
					// Prefer stored totalPrice; else use stored unitPrice; else lookup inventory
					double tp = rec.getTotalPrice();
					if (tp > 0.0) {
						totalRevenue += tp;
					} else if (rec.getUnitPrice() > 0.0) {
						totalRevenue += rec.getUnitPrice() * qty;
					} else {
						InventoryItem itm = allItems.stream()
								.filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(itemName))
								.findFirst().orElse(null);
						if (itm != null) {
							totalRevenue += itm.getUnitPrice() * qty;
						}
					}
				}
			} catch (Exception e) {
				// ignore lookup errors per-item
			}
		}
		final double displayRevenue = totalRevenue;
		Platform.runLater(() -> {
			try { totalRevenueLabel.setText(String.format("₱%,.2f", displayRevenue)); } catch (Exception ignored) {}
		});
		// compute payments totals and items sold for today
		try {
			double cashTotal = 0.0;
			double gcashTotal = 0.0;
			int itemsSold = 0;
			java.time.LocalDate today = java.time.LocalDate.now();
			for (TransactionLogger.TransactionRecord rec : records) {
				if (rec == null || rec.getDate() == null) continue;
				if (!rec.getDate().toLocalDate().isEqual(today)) continue; // only today's transactions
				// count items sold for SALE records
				if ("SALE".equalsIgnoreCase(rec.getType())) {
					itemsSold += Math.max(0, rec.getQuantity());
					// determine if GCash via user description ERef
					String ud = rec.getUser();
					InventoryItem itm = dataManager.getAllItems().stream()
						.filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(rec.getItemName()))
						.findFirst().orElse(null);
					double unit = itm == null ? 0.0 : itm.getUnitPrice();
					if (ud != null && ud.contains("[ERef:")) {
						gcashTotal += unit * rec.getQuantity();
					} else {
						cashTotal += unit * rec.getQuantity();
					}
				}
			}
			final double cashF = cashTotal;
			final double gcashF = gcashTotal;
			final int itemsF = itemsSold;
			Platform.runLater(() -> {
				try { if (paymentsCashLabel != null) paymentsCashLabel.setText(String.format("Cash: ₱%,.2f", cashF)); } catch (Exception ignored) {}
				try { if (paymentsGcashLabel != null) paymentsGcashLabel.setText(String.format("GCash: ₱%,.2f", gcashF)); } catch (Exception ignored) {}
				try { if (itemsSoldLabel != null) itemsSoldLabel.setText(String.valueOf(itemsF)); } catch (Exception ignored) {}
			});
		} catch (Exception ignored) {}
	}

	public void shutdown() {
		try { refresher.cancel(); } catch (Exception ignored) {}
		try { if (footerClock != null) footerClock.stop(); } catch (Exception ignored) {}
	}

	@FXML
	private void handleBack() {
		try {
			// Close the window containing this controller
			javafx.stage.Window w = transactionsTable.getScene().getWindow();
			if (w instanceof javafx.stage.Stage) ((javafx.stage.Stage) w).close();
		} catch (Exception ignored) {}
	}

	@FXML
	private void handleClearRecentTransactions() {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "This will clear recent transactions. Continue?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
		confirm.setHeaderText(null);
		java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
		if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.YES) return;

		try {
			txnLogger.clearAllRecords();
			if (transactionsTable != null) transactionsTable.getItems().clear();
			refreshMetrics();
			Alert ok = new Alert(Alert.AlertType.INFORMATION, "Recent transactions cleared.", javafx.scene.control.ButtonType.OK);
			ok.setHeaderText(null);
			ok.showAndWait();
		} catch (Exception e) {
			Alert err = new Alert(Alert.AlertType.ERROR, "Failed to clear recent transactions: " + e.getMessage(), javafx.scene.control.ButtonType.OK);
			err.setHeaderText(null);
			err.showAndWait();
		}
	}

	private void showAlert(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	@FXML
	private void handleExportTransactions() {
		try {
			javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
			fc.setTitle("Export Transactions");
			fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
			String userHome = System.getProperty("user.home");
			java.io.File downloads = new java.io.File(userHome, "Downloads");
			if (downloads.exists() && downloads.isDirectory()) fc.setInitialDirectory(downloads);
			fc.setInitialFileName("transactions_" + java.time.LocalDate.now().toString() + ".xlsx");
			java.io.File out = fc.showSaveDialog(transactionsTable.getScene().getWindow());
			if (out == null) {
				showAlert("Export Cancelled", "No file selected.");
				return;
			}
			// Collect only the recent transactions currently shown in the POS Overview (transactionsTable)
			List<TransactionLogger.TransactionRecord> list = new ArrayList<>(transactionsTable.getItems());
			exportTransactionsToExcel(out, list);
			showAlert("Export Successful", "Transactions exported to: " + out.getAbsolutePath());
		} catch (Exception e) {
			showAlert("Export Error", "Failed to export transactions: " + e.getMessage());
		}
	}

	private void exportTransactionsToExcel(java.io.File file, List<TransactionLogger.TransactionRecord> records) throws IOException {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Transactions");
			// Styles
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			CreationHelper ch = workbook.getCreationHelper();
			CellStyle dateStyle = workbook.createCellStyle();
			short df = ch.createDataFormat().getFormat("yyyy-mm-dd hh:mm AM/PM");
			dateStyle.setDataFormat(df);
			CellStyle currencyStyle = workbook.createCellStyle();
			currencyStyle.setDataFormat(ch.createDataFormat().getFormat("₱0.00"));

			// SUMMARY block (top rows)
			int rowIdx = 0;
			Row titleRow = sheet.createRow(rowIdx++);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellValue("POS Overview Summary");
			titleCell.setCellStyle(headerStyle);

			// Compute today's payments and items sold and totals
			double cashTotal = 0.0;
			double gcashTotal = 0.0;
			int itemsSold = 0;
			double totalRevenue = 0.0;
			java.time.LocalDate today = java.time.LocalDate.now();
			for (TransactionLogger.TransactionRecord rec : TransactionLogger.getInstance().getRecords()) {
				if (rec == null || rec.getDate() == null) continue;
				if (!rec.getDate().toLocalDate().isEqual(today)) continue;
				if ("SALE".equalsIgnoreCase(rec.getType())) {
					itemsSold += Math.max(0, rec.getQuantity());
					double tp = rec.getTotalPrice();
					double up = rec.getUnitPrice();
					double computed = tp > 0.0 ? tp : (up > 0.0 ? up * rec.getQuantity() : 0.0);
					totalRevenue += computed;
					String ud = rec.getUser();
					if (ud != null && ud.contains("[ERef:")) gcashTotal += computed; else cashTotal += computed;
				}
			}
			// Total inventory value from DataManager
			DataManager.InventoryStats stats = dataManager.getInventoryStats();
			double totalInventoryValue = stats.getTotalValue();

			Row r1 = sheet.createRow(rowIdx++);
			r1.createCell(0).setCellValue("Payments Today - Cash");
			Cell c1 = r1.createCell(1);
			c1.setCellValue(cashTotal);
			c1.setCellStyle(currencyStyle);

			Row r2 = sheet.createRow(rowIdx++);
			r2.createCell(0).setCellValue("Payments Today - GCash");
			Cell c2 = r2.createCell(1);
			c2.setCellValue(gcashTotal);
			c2.setCellStyle(currencyStyle);

			Row r3 = sheet.createRow(rowIdx++);
			r3.createCell(0).setCellValue("Items Sold Today");
			r3.createCell(1).setCellValue(itemsSold);

			Row r4 = sheet.createRow(rowIdx++);
			r4.createCell(0).setCellValue("Total Inventory Value");
			Cell c4 = r4.createCell(1);
			c4.setCellValue(totalInventoryValue);
			c4.setCellStyle(currencyStyle);

			Row r5 = sheet.createRow(rowIdx++);
			r5.createCell(0).setCellValue("Total Revenue Today");
			Cell c5 = r5.createCell(1);
			c5.setCellValue(totalRevenue);
			c5.setCellStyle(currencyStyle);

			// Empty row between summary and transactions
			rowIdx++;

			// Transactions header
			Row header = sheet.createRow(rowIdx++);
			String[] headers = new String[] {"Reference", "Item", "Unit Price", "Qty", "Total Price", "Payment Method", "Date/Time", "User"};
			for (int i = 0; i < headers.length; i++) {
				Cell c = header.createCell(i);
				c.setCellValue(headers[i]);
				c.setCellStyle(headerStyle);
			}

			// Write transactions starting at current rowIdx
			for (TransactionLogger.TransactionRecord rec : records) {
				Row row = sheet.createRow(rowIdx++);
				// Reference: prefer Tx id (with username) or ERef or fallback to CASH for sales
				String userDesc = rec.getUser() == null ? "" : rec.getUser();
				String tx = "";
				String eref = "";
				try {
					int tpos = userDesc.indexOf("[Tx:");
					if (tpos >= 0) {
						int end = userDesc.indexOf(']', tpos);
						if (end > tpos) tx = userDesc.substring(tpos + 4, end);
					}
					int epos = userDesc.indexOf("[ERef:");
					if (epos >= 0) {
						int end = userDesc.indexOf(']', epos);
						if (end > epos) eref = userDesc.substring(epos + 6, end);
					}
				} catch (Exception ignored) {}
				String usernameOnly = stripMarkers(userDesc);
				String reference = "";
				// Priority: GCash reference (ERef) -> Cash label for cash sales -> Tx id (if present) -> username
				if (!eref.isEmpty()) {
					reference = eref;
				} else if ("SALE".equalsIgnoreCase(rec.getType())) {
					// For cash sales, just mark as Cash (do not include cashier name or Tx id)
					reference = "Cash";
				} else if (!tx.isEmpty()) {
					// For non-sale records with a Tx id, show the Tx id only
					reference = tx;
				} else {
					reference = usernameOnly;
				}
				row.createCell(0).setCellValue(reference);
				row.createCell(1).setCellValue(rec.getItemName() == null ? "" : rec.getItemName());
				Cell upCell = row.createCell(2);
				upCell.setCellValue(rec.getUnitPrice());
				upCell.setCellStyle(currencyStyle);
				row.createCell(3).setCellValue(rec.getQuantity());
				Cell tpCell = row.createCell(4);
				tpCell.setCellValue(rec.getTotalPrice());
				tpCell.setCellStyle(currencyStyle);
				String pm = "";
				if (userDesc.contains("[ERef:")) pm = "GCash";
				else if ("SALE".equalsIgnoreCase(rec.getType())) pm = "Cash";
				row.createCell(5).setCellValue(pm);
				if (rec.getDate() != null) {
					java.util.Date d = java.util.Date.from(rec.getDate().atZone(ZoneId.systemDefault()).toInstant());
					Cell dc = row.createCell(6);
					dc.setCellValue(d);
					dc.setCellStyle(dateStyle);
				} else {
					row.createCell(6).setCellValue("");
				}
				// User column: username only (strip any [Tx:] or [ERef:] markers)
				row.createCell(7).setCellValue(usernameOnly);
			}

			// Auto-size columns
			for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

			try (FileOutputStream fos = new FileOutputStream(file)) { workbook.write(fos); }
		}
	}
}
