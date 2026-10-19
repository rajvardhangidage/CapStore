package com.capstore.service;

import com.capstore.entity.CustomerOrder;
import com.capstore.entity.OrderItem;
import com.capstore.entity.PaymentStatus;
import com.capstore.exception.ApiException;
import com.capstore.repository.OrderRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final OrderService orderService;
    private final OrderRepository orders;

    @Value("${app.business.name}") private String bizName;
    @Value("${app.business.gstin}") private String bizGstin;
    @Value("${app.business.address}") private String bizAddress;
    @Value("${app.business.state}") private String bizState;
    @Value("${app.business.email}") private String bizEmail;
    @Value("${app.business.phone}") private String bizPhone;
    public record Invoice(String filename, byte[] pdf) {}

    @Transactional(readOnly = true)
    public Invoice forCustomer(String orderPublicId) { return render(orderService.findMine(orderPublicId)); }

    @Transactional(readOnly = true)
    public Invoice forAdmin(String orderPublicId) {
        return render(orders.findByPublicId(orderPublicId).orElseThrow(() -> ApiException.notFound("Order not found")));
    }

    private Invoice render(CustomerOrder o) {
        if (o.getPaymentStatus() != PaymentStatus.PAID)
            throw ApiException.badRequest("Invoice is available after payment is received");
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font norm = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            doc.add(new Paragraph("TAX INVOICE", title));
            doc.add(new Paragraph(bizName, bold));
            doc.add(new Paragraph(bizAddress + "\nGSTIN: " + bizGstin + "\n" + bizEmail + " | " + bizPhone, norm));
            doc.add(new Paragraph(" "));

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.addCell(plain("Invoice No: INV-" + o.getOrderNo() + "\nDate: "
                    + o.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + "\nPlace of supply: " + o.getShipState(), norm));
            String buyer = o.getShipName() + "\n" + o.getShipLine1() + (o.getShipLine2() == null || o.getShipLine2().isBlank() ? "" : ", " + o.getShipLine2())
                    + "\n" + o.getShipCity() + ", " + o.getShipState() + " - " + o.getShipPincode() + "\nPhone: " + o.getShipPhone();
            if (o.getUser().getCompanyName() != null)
                buyer = o.getUser().getCompanyName() + "\nGSTIN: " + o.getUser().getGstNumber() + "\n" + buyer;
            meta.addCell(plain("Bill to:\n" + buyer, norm));
            doc.add(meta);
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(new float[]{4.5f, 1f, 1.6f, 1.8f});
            t.setWidthPercentage(100);
            for (String h : new String[]{"Item", "Qty", "Rate (Rs.)", "Amount (Rs.)"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, bold));
                c.setBackgroundColor(new Color(230, 233, 240));
                c.setPadding(5);
                t.addCell(c);
            }
            for (OrderItem i : o.getItems()) {
                String desc = i.getProductName() + " (" + nz(i.getColor()) + (i.getSize() != null ? ", " + i.getSize() : "") + ")\nSKU: " + i.getSku() + "  HSN: 6505";
                t.addCell(cell(desc, norm, Element.ALIGN_LEFT));
                t.addCell(cell(String.valueOf(i.getQuantity()), norm, Element.ALIGN_CENTER));
                t.addCell(cell(money(i.getUnitPrice()), norm, Element.ALIGN_RIGHT));
                t.addCell(cell(money(i.getLineTotal()), norm, Element.ALIGN_RIGHT));
            }
            doc.add(t);
            doc.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(new float[]{6f, 2f});
            totals.setWidthPercentage(60);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            row(totals, "Subtotal", o.getSubtotal(), norm);
            if (o.getDiscount() != null && o.getDiscount().signum() > 0)
                row(totals, "Discount" + (o.getCouponCode() != null ? " (" + o.getCouponCode() + ")" : ""), o.getDiscount().negate(), norm);
            boolean sameState = o.getShipState() != null && o.getShipState().trim().equalsIgnoreCase(bizState.trim());
            if (sameState) {
                BigDecimal half = o.getTax().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                BigDecimal halfRate = o.getGstRatePercent().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                row(totals, "CGST @ " + halfRate.stripTrailingZeros().toPlainString() + "%", half, norm);
                row(totals, "SGST @ " + halfRate.stripTrailingZeros().toPlainString() + "%", o.getTax().subtract(half), norm);
            } else {
                row(totals, "IGST @ " + o.getGstRatePercent().stripTrailingZeros().toPlainString() + "%", o.getTax(), norm);
            }
            row(totals, "Shipping", o.getShipping(), norm);
            row(totals, "Total", o.getTotal(), bold);
            doc.add(totals);

            doc.add(new Paragraph("\nThis is a computer-generated invoice and does not require a signature.", small));
            doc.close();
            return new Invoice("invoice-" + o.getOrderNo() + ".pdf", out.toByteArray());
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate invoice", e);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String money(BigDecimal b) { return b.setScale(2, RoundingMode.HALF_UP).toPlainString(); }

    private PdfPCell plain(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell cell(String text, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        return c;
    }

    private void row(PdfPTable t, String label, BigDecimal amount, Font f) {
        PdfPCell a = new PdfPCell(new Phrase(label, f));
        a.setBorder(Rectangle.NO_BORDER);
        PdfPCell b = new PdfPCell(new Phrase(money(amount), f));
        b.setBorder(Rectangle.NO_BORDER);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(a);
        t.addCell(b);
    }
}
