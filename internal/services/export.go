package services

import (
	"bytes"
	"encoding/csv"
	"fmt"
	"strings"
	"time"

	"github.com/jung-kurt/gofpdf"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/xuri/excelize/v2"
)

func (s *Services) ExportSalePDF(sale *models.Sale) ([]byte, error) {
	settings, err := s.GetSettings()
	if err != nil {
		settings = &models.ShopSettings{ShopName: "Medical Shop"}
	}

	pdf := gofpdf.New("P", "mm", "A4", "")
	pdf.AddPage()
	pdf.SetFont("Arial", "B", 16)
	pdf.Cell(0, 10, settings.ShopName)
	pdf.Ln(8)
	pdf.SetFont("Arial", "", 10)
	if settings.Address != "" {
		pdf.Cell(0, 6, settings.Address)
		pdf.Ln(6)
	}
	if settings.GSTNumber != "" {
		pdf.Cell(0, 6, "GST: "+settings.GSTNumber)
		pdf.Ln(10)
	}

	pdf.SetFont("Arial", "B", 14)
	pdf.Cell(0, 8, "INVOICE")
	pdf.Ln(8)
	pdf.SetFont("Arial", "", 10)
	pdf.Cell(0, 6, "Bill No: "+sale.BillNumber)
	pdf.Ln(6)
	pdf.Cell(0, 6, "Date: "+sale.SaleDate.Format("02-01-2006 15:04"))
	pdf.Ln(10)

	pdf.SetFont("Arial", "B", 9)
	pdf.CellFormat(70, 7, "Medicine", "1", 0, "L", false, 0, "")
	pdf.CellFormat(20, 7, "Qty", "1", 0, "C", false, 0, "")
	pdf.CellFormat(30, 7, "Price", "1", 0, "R", false, 0, "")
	pdf.CellFormat(30, 7, "GST", "1", 0, "R", false, 0, "")
	pdf.CellFormat(30, 7, "Total", "1", 1, "R", false, 0, "")
	pdf.SetFont("Arial", "", 9)

	for _, item := range sale.Items {
		name := "Item"
		if item.Medicine != nil {
			name = item.Medicine.MedicineName
		}
		pdf.CellFormat(70, 7, name, "1", 0, "L", false, 0, "")
		pdf.CellFormat(20, 7, fmt.Sprintf("%d", item.Quantity), "1", 0, "C", false, 0, "")
		pdf.CellFormat(30, 7, fmt.Sprintf("%.2f", item.UnitPrice), "1", 0, "R", false, 0, "")
		pdf.CellFormat(30, 7, fmt.Sprintf("%.2f", item.GSTAmount), "1", 0, "R", false, 0, "")
		pdf.CellFormat(30, 7, fmt.Sprintf("%.2f", item.Subtotal), "1", 1, "R", false, 0, "")
	}

	pdf.Ln(4)
	pdf.SetFont("Arial", "B", 11)
	pdf.Cell(0, 8, fmt.Sprintf("Grand Total: Rs. %.2f", sale.GrandTotal))
	pdf.Ln(6)
	pdf.SetFont("Arial", "", 10)
	pdf.Cell(0, 6, "Payment: "+string(sale.PaymentMode))
	if settings.InvoiceFooter != "" {
		pdf.Ln(10)
		pdf.MultiCell(0, 6, settings.InvoiceFooter, "", "L", false)
	}

	var buf bytes.Buffer
	if err := pdf.Output(&buf); err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func (s *Services) ExportReportExcel(report map[string]interface{}) ([]byte, error) {
	f := excelize.NewFile()
	sheet := "Report"
	f.SetSheetName("Sheet1", sheet)
	f.SetCellValue(sheet, "A1", "Key")
	f.SetCellValue(sheet, "B1", "Value")
	row := 2
	for key, value := range report {
		f.SetCellValue(sheet, fmt.Sprintf("A%d", row), key)
		if value != nil {
			f.SetCellValue(sheet, fmt.Sprintf("B%d", row), fmt.Sprintf("%v", value))
		}
		row++
	}
	buf, err := f.WriteToBuffer()
	if err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func (s *Services) ExportReportCSV(report map[string]interface{}) ([]byte, error) {
	var buf bytes.Buffer
	w := csv.NewWriter(&buf)
	_ = w.Write([]string{"Key", "Value"})
	for key, value := range report {
		val := ""
		if value != nil {
			val = fmt.Sprintf("%v", value)
		}
		_ = w.Write([]string{key, val})
	}
	w.Flush()
	return buf.Bytes(), w.Error()
}

func (s *Services) ExportReport(reportType, format string, start, end time.Time) ([]byte, string, error) {
	report, err := s.GenerateReport(reportType, start, end)
	if err != nil {
		return nil, "", err
	}

	switch strings.ToLower(format) {
	case "xlsx", "excel":
		data, err := s.ExportReportExcel(report)
		return data, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", err
	case "csv":
		data, err := s.ExportReportCSV(report)
		return data, "text/csv", err
	default:
		data, err := s.ExportReportCSV(report)
		return data, "text/csv", err
	}
}
