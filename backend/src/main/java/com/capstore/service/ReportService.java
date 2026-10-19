package com.capstore.service;

import com.capstore.dto.ExtraDtos.ReportData;
import com.capstore.entity.CustomerOrder;
import com.capstore.entity.ProductVariant;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderItemRepository;
import com.capstore.repository.OrderRepository;
import com.capstore.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductVariantRepository variants;

    @Transactional(readOnly = true)
    public ReportData build(String type, LocalDate from, LocalDate to) {
        LocalDate toD = to == null ? LocalDate.now() : to;
        LocalDate fromD = from == null ? toD.minusDays(29) : from;
        if (fromD.isAfter(toD)) throw ApiException.badRequest("'From' date must be before 'To' date");
        LocalDateTime f = fromD.atStartOfDay();
        LocalDateTime t = toD.plusDays(1).atStartOfDay().minusNanos(1000);
        String range = " (" + fromD + " to " + toD + ")";
        return switch (type) {
            case "sales" -> total("Daily sales" + range, List.of("Date", "Orders", "Revenue (Rs.)"), orders.salesByDay(f, t), true);
            case "products" -> total("Product-wise sales" + range, List.of("Product", "Units sold", "Revenue (Rs.)"), items.productSales(f, t), false);
            case "categories" -> total("Category-wise sales" + range, List.of("Category", "Units sold", "Revenue (Rs.)"), items.categorySales(f, t), false);
            case "customer-types" -> total("B2B vs B2C sales" + range, List.of("Customer type", "Orders", "Revenue (Rs.)"), orders.salesByCustomerType(f, t), false);
            case "gst" -> gst(f, t, range);
            case "inventory" -> inventory();
            default -> throw ApiException.badRequest("Unknown report type");
        };
    }

    private ReportData total(String title, List<String> headers, List<Object[]> data, boolean totalRow) {
        List<List<Object>> rows = new ArrayList<>();
        long cnt = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (Object[] r : data) {
            Object first = r[0] == null ? "-" : r[0].toString();
            Number n = r[1] == null ? 0 : (Number) r[1];
            BigDecimal amt = r[2] == null ? BigDecimal.ZERO : new BigDecimal(r[2].toString());
            rows.add(new ArrayList<>(List.of(first, n, amt)));
            cnt += n.longValue();
            sum = sum.add(amt);
        }
        if (totalRow && !rows.isEmpty()) rows.add(new ArrayList<>(List.of("TOTAL", cnt, sum)));
        return new ReportData(title, headers, rows);
    }

    private ReportData gst(LocalDateTime f, LocalDateTime t, String range) {
        List<List<Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        BigDecimal taxable = BigDecimal.ZERO, tax = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (CustomerOrder o : orders.findPaidBetween(f, t)) {
            BigDecimal tv = o.getSubtotal().subtract(o.getDiscount() == null ? BigDecimal.ZERO : o.getDiscount());
            rows.add(new ArrayList<>(List.of("INV-" + o.getOrderNo(), o.getCreatedAt().format(fmt), o.getUser().getName(),
                    o.getUser().getGstNumber() == null ? "-" : o.getUser().getGstNumber(), o.getShipState(), tv, o.getTax(), o.getTotal())));
            taxable = taxable.add(tv);
            tax = tax.add(o.getTax());
            total = total.add(o.getTotal());
        }
        if (!rows.isEmpty()) rows.add(new ArrayList<>(List.of("TOTAL", "", "", "", "", taxable, tax, total)));
        return new ReportData("GST report" + range,
                List.of("Invoice", "Date", "Customer", "Buyer GSTIN", "Place of supply", "Taxable value (Rs.)", "GST (Rs.)", "Invoice total (Rs.)"), rows);
    }

    private ReportData inventory() {
        List<List<Object>> rows = new ArrayList<>();
        for (ProductVariant v : variants.findAll()) {
            BigDecimal price = v.getPrice() != null ? v.getPrice() : v.getProduct().getBasePrice();
            rows.add(new ArrayList<>(List.of(v.getProduct().getName(), v.getSku(), v.getSize() == null ? "-" : v.getSize(),
                    v.getColor() == null ? "-" : v.getColor(), v.getStockQty(), price,
                    price.multiply(BigDecimal.valueOf(v.getStockQty())))));
        }
        return new ReportData("Inventory report", List.of("Product", "SKU", "Size", "Color", "In stock", "Price (Rs.)", "Stock value (Rs.)"), rows);
    }

    public byte[] toXlsx(ReportData d) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("Report");
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);
            Row title = s.createRow(0);
            title.createCell(0).setCellValue(d.title());
            title.getCell(0).setCellStyle(bold);
            Row h = s.createRow(2);
            for (int i = 0; i < d.headers().size(); i++) {
                Cell c = h.createCell(i);
                c.setCellValue(d.headers().get(i));
                c.setCellStyle(bold);
            }
            int r = 3;
            for (List<Object> row : d.rows()) {
                Row rr = s.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    Object v = row.get(c);
                    Cell cell = rr.createCell(c);
                    if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                    else cell.setCellValue(v == null ? "" : v.toString());
                }
            }
            for (int i = 0; i < d.headers().size(); i++) s.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build Excel file", e);
        }
    }
}
