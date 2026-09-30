package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.CaseReportSummaryDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PdfReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PdfReportGenerator.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Palette
    private static final Color CRIMSON_HEADER = new Color(193, 18, 31);
    private static final Color TEXT_DARK = new Color(20, 20, 25);
    private static final Color TEXT_MUTED = new Color(90, 90, 95);
    private static final Color BG_LIGHT = new Color(248, 249, 250);
    private static final Color BG_AMBER = new Color(254, 243, 199);
    private static final Color BORDER_AMBER = new Color(245, 158, 11);
    private static final Color BORDER_LIGHT = new Color(220, 225, 230);
    private static final Color TABLE_HEADER_BG = new Color(235, 238, 242);

    // Fonts
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL, Color.WHITE);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL, new Color(240, 240, 240));
    private static final Font FONT_SECTION_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.NORMAL, CRIMSON_HEADER);
    private static final Font FONT_SUBSECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Font.NORMAL, TEXT_DARK);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Font.NORMAL, TEXT_DARK);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Font.NORMAL, TEXT_DARK);
    private static final Font FONT_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Font.NORMAL, TEXT_MUTED);
    private static final Font FONT_NOTICE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.0f, Font.NORMAL, new Color(120, 53, 15));

    public byte[] generatePdfReport(CaseReportSummaryDTO data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 45, 45);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Set Header & Footer Page Event
            writer.setPageEvent(new HeaderFooterEvent(data.getCaseNumber()));

            document.open();

            // 1. Cover / Header Banner
            buildHeaderBanner(document, data);
            document.add(Chunk.NEWLINE);

            // 2. Responsible AI Advisory Notice Box
            buildNoticeBox(document, "RESPONSIBLE AI INVESTIGATION ADVISORY", data.getResponsibleAiNotice());
            document.add(Chunk.NEWLINE);

            // 3. Case Overview Section
            buildCaseOverviewSection(document, data);
            document.add(Chunk.NEWLINE);

            // 4. FIR Section
            buildFirSection(document, data);
            document.add(Chunk.NEWLINE);

            // 5. Victims Section
            buildVictimsSection(document, data);
            document.add(Chunk.NEWLINE);

            // 6. Suspects Section
            buildSuspectsSection(document, data);
            document.add(Chunk.NEWLINE);

            // 7. Evidence Section
            buildEvidenceSection(document, data);
            document.add(Chunk.NEWLINE);

            // 8. Evidence Gap Analysis & Case State Section
            buildCaseStateAndGapsSection(document, data);
            document.add(Chunk.NEWLINE);

            // 9. AI Analysis Section
            buildAiAnalysisSection(document, data);
            document.add(Chunk.NEWLINE);

            // 10. Investigator Review Section (Phase 12)
            buildInvestigatorReviewSection(document, data);
            document.add(Chunk.NEWLINE);

            // 11. Public Information Section (Phase 14)
            buildPublicInformationSection(document, data);
            document.add(Chunk.NEWLINE);

            // 12. Notification Summary Section (Phase 13)
            buildNotificationSummarySection(document, data);
            document.add(Chunk.NEWLINE);

            // 13. Case Report Summary & Metadata Footer
            buildReportSummaryFooter(document, data);

            document.close();
        } catch (DocumentException ex) {
            logger.error("Failed to render PDF report for case {}: {}", data.getCaseNumber(), ex.getMessage(), ex);
            throw new RuntimeException("Error rendering PDF case report: " + ex.getMessage(), ex);
        }

        return out.toByteArray();
    }

    private void buildHeaderBanner(Document document, CaseReportSummaryDTO data) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(CRIMSON_HEADER);
        cell.setPadding(12);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph p1 = new Paragraph("CrimeSphere AI — INVESTIGATION CASE REPORT", FONT_TITLE);
        p1.setAlignment(Element.ALIGN_LEFT);

        Paragraph p2 = new Paragraph("CASE NUMBER: " + safe(data.getCaseNumber()) + " | STATION: " + safe(data.getPoliceStationName()) + " (" + safe(data.getPoliceStationDistrict()) + ")", FONT_SUBTITLE);
        p2.setAlignment(Element.ALIGN_LEFT);

        cell.addElement(p1);
        cell.addElement(p2);
        headerTable.addCell(cell);

        document.add(headerTable);
    }

    private void buildNoticeBox(Document document, String titleText, String noticeContent) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_AMBER);
        cell.setBorderColor(BORDER_AMBER);
        cell.setBorderWidth(1.0f);
        cell.setPadding(8);

        Paragraph titleP = new Paragraph(titleText, FONT_BODY_BOLD);
        titleP.setSpacingAfter(3);
        Paragraph contentP = new Paragraph(noticeContent, FONT_NOTICE);

        cell.addElement(titleP);
        cell.addElement(contentP);
        box.addCell(cell);

        document.add(box);
    }

    private void buildSectionHeader(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, FONT_SECTION_HEAD);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        document.add(p);

        // Thin separator line
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell c = new PdfPCell();
        c.setFixedHeight(1.5f);
        c.setBackgroundColor(CRIMSON_HEADER);
        c.setBorder(Rectangle.NO_BORDER);
        line.addCell(c);
        line.setSpacingAfter(6);
        document.add(line);
    }

    private void buildCaseOverviewSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "1. CASE OVERVIEW & PRECINCT METADATA");

        PdfPTable table = createGridTable(new float[]{1.2f, 2.0f, 1.2f, 2.0f});

        addGridRow(table, "Case Number:", safe(data.getCaseNumber()), "Case Type:", safe(data.getCaseType()));
        addGridRow(table, "Case Title:", safe(data.getCaseTitle()), "Priority:", safe(data.getPriority()));
        addGridRow(table, "Status:", safe(data.getStatus()), "Investigation State:", safe(data.getInvestigationStateLabel()));
        addGridRow(table, "Police Station:", safe(data.getPoliceStationName()), "District:", safe(data.getPoliceStationDistrict()));
        addGridRow(table, "Date Registered:", formatDate(data.getCreatedAt()), "Last Updated:", formatDate(data.getUpdatedAt()));
        addGridRow(table, "Generated By:", safe(data.getGeneratedByOfficerName()) + " (" + safe(data.getGeneratedByOfficerBadge()) + ")", "Report Version:", safe(data.getReportVersion()));

        document.add(table);

        if (data.getDescription() != null && !data.getDescription().trim().isEmpty()) {
            Paragraph descTitle = new Paragraph("Case Narrative Briefing:", FONT_SUBSECTION);
            descTitle.setSpacingBefore(4);
            descTitle.setSpacingAfter(2);
            document.add(descTitle);

            Paragraph descP = new Paragraph(data.getDescription(), FONT_BODY);
            document.add(descP);
        }
    }

    private void buildFirSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "2. FIRST INFORMATION REPORT (FIR) INFORMATION");

        if (!data.isFirExists()) {
            Paragraph emptyP = new Paragraph("FIR: NOT REGISTERED / INFORMATION NOT AVAILABLE", FONT_BODY_BOLD);
            document.add(emptyP);
            return;
        }

        PdfPTable table = createGridTable(new float[]{1.4f, 2.0f, 1.4f, 2.0f});

        addGridRow(table, "FIR Number:", safe(data.getFirNumber()), "Registration Date:", formatDate(data.getFirRegDate()));
        addGridRow(table, "Complainant Name:", safe(data.getComplainantName()), "Contact Phone:", safe(data.getComplainantContact()));
        addGridRow(table, "Incident Date:", formatDate(data.getIncidentDate()), "Incident Location:", safe(data.getIncidentLocation()));
        addGridRow(table, "Offence Type:", safe(data.getOffenceType()), "Sections of Law:", safe(data.getSectionsOfLaw()));

        document.add(table);

        if (data.getFirDescription() != null && !data.getFirDescription().trim().isEmpty()) {
            Paragraph descTitle = new Paragraph("FIR Statement / Brief Description:", FONT_SUBSECTION);
            descTitle.setSpacingBefore(4);
            descTitle.setSpacingAfter(2);
            document.add(descTitle);

            Paragraph descP = new Paragraph(data.getFirDescription(), FONT_BODY);
            document.add(descP);
        }
    }

    private void buildVictimsSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "3. VICTIM REGISTRY");

        if (data.getVictims() == null || data.getVictims().isEmpty()) {
            Paragraph emptyP = new Paragraph("No victim record currently available.", FONT_MUTED);
            document.add(emptyP);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{1.8f, 0.6f, 0.8f, 1.2f, 1.2f, 1.4f, 1.0f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeader(table, new String[]{"Victim Name", "Age", "Gender", "Phone", "Occupation", "Relationship", "Status"});

        for (CaseReportSummaryDTO.VictimSummaryDTO v : data.getVictims()) {
            addTableCell(table, safe(v.getFullName()), FONT_BODY_BOLD);
            addTableCell(table, v.getAge() != null ? String.valueOf(v.getAge()) : "-", FONT_BODY);
            addTableCell(table, safe(v.getGender()), FONT_BODY);
            addTableCell(table, safe(v.getPhone()), FONT_BODY);
            addTableCell(table, safe(v.getOccupation()), FONT_BODY);
            addTableCell(table, safe(v.getRelationshipToCase()), FONT_BODY);
            addTableCell(table, safe(v.getStatus()), FONT_BODY_BOLD);
        }

        document.add(table);
    }

    private void buildSuspectsSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "4. SUSPECT & PERSON OF INTEREST REGISTRY");

        if (data.getSuspects() == null || data.getSuspects().isEmpty()) {
            Paragraph emptyP = new Paragraph("No suspect record currently available.", FONT_MUTED);
            document.add(emptyP);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{1.8f, 1.2f, 0.6f, 0.8f, 1.2f, 1.4f, 1.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeader(table, new String[]{"Full Name", "Alias", "Age", "Gender", "Phone", "Occupation", "Investigation Status"});

        for (CaseReportSummaryDTO.SuspectSummaryDTO s : data.getSuspects()) {
            addTableCell(table, safe(s.getFullName()), FONT_BODY_BOLD);
            addTableCell(table, safe(s.getAlias()), FONT_BODY);
            addTableCell(table, s.getAge() != null ? String.valueOf(s.getAge()) : "-", FONT_BODY);
            addTableCell(table, safe(s.getGender()), FONT_BODY);
            addTableCell(table, safe(s.getPhone()), FONT_BODY);
            addTableCell(table, safe(s.getOccupation()), FONT_BODY);
            addTableCell(table, safe(s.getInvestigationStatus()), FONT_BODY_BOLD);
        }

        document.add(table);
    }

    private void buildEvidenceSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "5. FORENSIC EVIDENCE METADATA ARCHIVE");

        Paragraph statsP = new Paragraph("Total Evidence Items: " + data.getTotalEvidenceCount() +
                "  |  Verified: " + data.getVerifiedEvidenceCount() +
                "  |  Under Review: " + data.getUnderReviewEvidenceCount() +
                "  |  Rejected: " + data.getRejectedEvidenceCount(), FONT_BODY_BOLD);
        statsP.setSpacingAfter(4);
        document.add(statsP);

        if (data.getEvidenceItems() == null || data.getEvidenceItems().isEmpty()) {
            Paragraph emptyP = new Paragraph("No physical or digital evidence items recorded in vault.", FONT_MUTED);
            document.add(emptyP);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{1.2f, 1.6f, 1.1f, 1.0f, 1.8f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeader(table, new String[]{"Evidence #", "Title / File Name", "Type", "Status", "SHA-256 File Hash", "Collected By / At"});

        for (CaseReportSummaryDTO.EvidenceSummaryDTO e : data.getEvidenceItems()) {
            addTableCell(table, safe(e.getEvidenceNumber()), FONT_BODY_BOLD);
            addTableCell(table, safe(e.getTitle()) + "\n(" + safe(e.getOriginalFileName()) + ")", FONT_BODY);
            addTableCell(table, safe(e.getEvidenceType()), FONT_BODY);
            addTableCell(table, safe(e.getStatus()), FONT_BODY_BOLD);
            addTableCell(table, safe(e.getFileHash()), FONT_MUTED);
            addTableCell(table, safe(e.getCollectedBy()) + "\n" + formatDate(e.getCollectedAt()), FONT_BODY);
        }

        document.add(table);

        Paragraph storageNotice = new Paragraph("Note: Evidence metadata referenced above. Actual binary evidence files and storage credentials are kept isolated in the encrypted Evidence Vault.", FONT_MUTED);
        storageNotice.setSpacingBefore(4);
        document.add(storageNotice);
    }

    private void buildCaseStateAndGapsSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "6. INVESTIGATION STATE & EVIDENCE GAP COMPLETENESS");

        PdfPTable table = createGridTable(new float[]{1.4f, 2.0f, 1.4f, 2.0f});
        addGridRow(table, "Current State Label:", safe(data.getInvestigationStateLabel()), "Completeness Score:", String.format("%.1f%%", data.getCompletenessPercentage()));
        addGridRow(table, "Completed Areas:", joinList(data.getCompletedAreas()), "Missing / Open Areas:", joinList(data.getMissingAreas()));
        addGridRow(table, "Last Evaluated:", formatDate(data.getStateLastEvaluatedAt()), "Open Evidence Gaps:", String.valueOf(data.getEvidenceGaps().size()));
        document.add(table);

        Paragraph gapNotice = new Paragraph("Evidence gap analysis is a system-generated completeness assessment and is not a legal determination.", FONT_MUTED);
        gapNotice.setSpacingBefore(4);
        gapNotice.setSpacingAfter(4);
        document.add(gapNotice);

        if (data.getEvidenceGaps() != null && !data.getEvidenceGaps().isEmpty()) {
            PdfPTable gapTable = new PdfPTable(new float[]{1.2f, 1.6f, 2.4f, 0.8f, 0.8f});
            gapTable.setWidthPercentage(100);
            gapTable.setSpacingBefore(4);

            addTableHeader(gapTable, new String[]{"Category", "Title", "Description / Reason", "Severity", "Status"});

            for (CaseReportSummaryDTO.EvidenceGapSummaryDTO g : data.getEvidenceGaps()) {
                addTableCell(gapTable, safe(g.getCategory()), FONT_BODY);
                addTableCell(gapTable, safe(g.getTitle()), FONT_BODY_BOLD);
                addTableCell(gapTable, safe(g.getDescription()) + (g.getReason() != null ? "\nReason: " + g.getReason() : ""), FONT_BODY);
                addTableCell(gapTable, safe(g.getSeverity()), FONT_BODY_BOLD);
                addTableCell(gapTable, safe(g.getStatus()), FONT_BODY);
            }

            document.add(gapTable);
        }
    }

    private void buildAiAnalysisSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "7. CRIMESPHERE AI INVESTIGATION FINDINGS");

        if (!data.isAiAnalysisAvailable()) {
            Paragraph emptyP = new Paragraph("No Groq AI analysis has been generated for this case record.", FONT_MUTED);
            document.add(emptyP);
            return;
        }

        PdfPTable metaTable = createGridTable(new float[]{1.4f, 2.0f, 1.4f, 2.0f});
        addGridRow(metaTable, "Analysis Version:", safe(data.getAnalysisVersion()), "AI Model:", safe(data.getModelName()));
        addGridRow(metaTable, "Analysis Date:", formatDate(data.getAnalysisDate()), "Findings Count:", String.valueOf(data.getAiFindings().size()));
        document.add(metaTable);

        if (data.getAiSummary() != null && !data.getAiSummary().trim().isEmpty()) {
            Paragraph sumTitle = new Paragraph("Executive AI Investigation Summary:", FONT_SUBSECTION);
            sumTitle.setSpacingBefore(4);
            sumTitle.setSpacingAfter(2);
            document.add(sumTitle);

            Paragraph sumP = new Paragraph(data.getAiSummary(), FONT_BODY);
            document.add(sumP);
        }

        if (data.getAiFindings() != null && !data.getAiFindings().isEmpty()) {
            Paragraph findTitle = new Paragraph("Detailed Evidence-Grounded Observations & Leads:", FONT_SUBSECTION);
            findTitle.setSpacingBefore(6);
            findTitle.setSpacingAfter(4);
            document.add(findTitle);

            PdfPTable table = new PdfPTable(new float[]{1.2f, 1.6f, 2.6f, 1.2f, 1.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(2);

            addTableHeader(table, new String[]{"Classification", "Finding Title", "AI Analysis Content", "Evidence Refs", "Verification Status"});

            for (CaseReportSummaryDTO.AiItemSummaryDTO item : data.getAiFindings()) {
                addTableCell(table, safe(item.getCategory()), FONT_BODY_BOLD);
                addTableCell(table, safe(item.getTitle()), FONT_BODY_BOLD);
                addTableCell(table, safe(item.getContent()), FONT_BODY);
                addTableCell(table, joinList(item.getEvidenceReferences()), FONT_MUTED);
                addTableCell(table, safe(item.getVerificationStatus()) + "\n(" + safe(item.getSupportLevel()) + ")", FONT_BODY_BOLD);
            }

            document.add(table);
        }

        Paragraph verNotice = new Paragraph("Verification status indicates how the AI recommendation relates to available case evidence. It does not establish truth, guilt, or innocence.", FONT_MUTED);
        verNotice.setSpacingBefore(4);
        document.add(verNotice);
    }

    private void buildInvestigatorReviewSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "8. HUMAN INVESTIGATOR REVIEW & DECISIONS (HUMAN-IN-THE-LOOP)");

        if (data.getInvestigatorReviews() == null || data.getInvestigatorReviews().isEmpty()) {
            Paragraph emptyP = new Paragraph("No investigator review decisions recorded yet. AI findings remain pending human review.", FONT_MUTED);
            document.add(emptyP);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{1.4f, 1.8f, 2.2f, 1.2f, 1.4f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeader(table, new String[]{"Finding Title", "Original AI Content", "Investigator Decision / Content", "Reviewer Name", "Review Date"});

        for (CaseReportSummaryDTO.InvestigatorReviewSummaryDTO r : data.getInvestigatorReviews()) {
            addTableCell(table, safe(r.getItemTitle()), FONT_BODY_BOLD);
            addTableCell(table, safe(r.getOriginalAiContent()), FONT_BODY);

            String decisionText = "DECISION: " + safe(r.getInvestigatorDecision());
            if ("APPROVED".equalsIgnoreCase(r.getInvestigatorDecision())) {
                decisionText += " (Approved by investigator)";
            } else if ("EDITED".equalsIgnoreCase(r.getInvestigatorDecision())) {
                decisionText += "\nEDITED CONTENT: " + safe(r.getReviewedContent());
            } else if ("REJECTED".equalsIgnoreCase(r.getInvestigatorDecision())) {
                decisionText += " (REJECTED BY INVESTIGATOR)";
            }
            if (r.getReviewComment() != null && !r.getReviewComment().isEmpty()) {
                decisionText += "\nComment: " + r.getReviewComment();
            }

            addTableCell(table, decisionText, FONT_BODY);
            addTableCell(table, safe(r.getReviewerName()), FONT_BODY);
            addTableCell(table, formatDate(r.getReviewDate()), FONT_MUTED);
        }

        document.add(table);
    }

    private void buildPublicInformationSection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "9. PUBLIC INFORMATION & NEWS INTELLIGENCE");

        buildNoticeBox(document, "PUBLIC INFORMATION — REQUIRES INDEPENDENT VERIFICATION",
                "Public information is collected from external news and web sources and is strictly informational. It does NOT constitute verified forensic evidence and is not copied into the Evidence Vault.");
        document.add(Chunk.NEWLINE);

        if (data.getPublicInformationItems() == null || data.getPublicInformationItems().isEmpty()) {
            Paragraph emptyP = new Paragraph("No public news or web intelligence items bookmarked for this case.", FONT_MUTED);
            document.add(emptyP);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{1.6f, 1.2f, 1.2f, 1.8f, 2.2f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addTableHeader(table, new String[]{"Article Title", "Publisher", "Published Date", "Verification Status", "Investigator Notes"});

        for (CaseReportSummaryDTO.PublicInfoSummaryDTO p : data.getPublicInformationItems()) {
            addTableCell(table, safe(p.getTitle()) + "\n(" + safe(p.getSourceUrl()) + ")", FONT_BODY_BOLD);
            addTableCell(table, safe(p.getSourceName()), FONT_BODY);
            addTableCell(table, formatDate(p.getPublishedAt()), FONT_MUTED);
            addTableCell(table, safe(p.getVerificationStatus()), FONT_BODY_BOLD);
            addTableCell(table, safe(p.getInvestigatorNotes()), FONT_BODY);
        }

        document.add(table);
    }

    private void buildNotificationSummarySection(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "10. NOTIFICATION DISPATCH SUMMARY");

        PdfPTable table = createGridTable(new float[]{1.4f, 2.0f, 1.4f, 2.0f});
        addGridRow(table, "Total Alerts Sent:", String.valueOf(data.getTotalNotificationsSent()), "Failed Dispatches:", String.valueOf(data.getTotalNotificationsFailed()));
        addGridRow(table, "Skipped (Preference):", String.valueOf(data.getTotalNotificationsSkipped()), "Email (SMTP):", String.valueOf(data.getEmailCount()));
        addGridRow(table, "WhatsApp (Meta Cloud API):", String.valueOf(data.getWhatsappCount()), "Audit Logged:", "Yes (Precinct Isolated)");
        document.add(table);
    }

    private void buildReportSummaryFooter(Document document, CaseReportSummaryDTO data) throws DocumentException {
        buildSectionHeader(document, "11. CASE REPORT SUMMARY & SIGN-OFF");

        PdfPTable table = createGridTable(new float[]{1.4f, 2.0f, 1.4f, 2.0f});
        addGridRow(table, "Case Number:", safe(data.getCaseNumber()), "Current Status:", safe(data.getStatus()));
        addGridRow(table, "Investigation State:", safe(data.getInvestigationStateLabel()), "Evidence Count:", data.getTotalEvidenceCount() + " (" + data.getVerifiedEvidenceCount() + " verified)");
        addGridRow(table, "Open Evidence Gaps:", String.valueOf(data.getEvidenceGaps().size()), "AI Analysis Status:", data.isAiAnalysisAvailable() ? "AVAILABLE" : "NONE");
        addGridRow(table, "Investigator Reviews:", String.valueOf(data.getInvestigatorReviews().size()), "Public News Items:", String.valueOf(data.getPublicInformationItems().size()));
        addGridRow(table, "Report Version:", safe(data.getReportVersion()), "Generated Timestamp:", formatDate(data.getGeneratedAt()));
        document.add(table);

        Paragraph signOffP = new Paragraph("Generated By Authorized Officer: " + safe(data.getGeneratedByOfficerName()) + " (Police ID: " + safe(data.getGeneratedByOfficerBadge()) + ")", FONT_BODY_BOLD);
        signOffP.setSpacingBefore(6);
        signOffP.setSpacingAfter(4);
        document.add(signOffP);

        buildNoticeBox(document, "CONFIDENTIAL INVESTIGATION DOCUMENT",
                "This document contains classified police investigation report data generated by CrimeSphere AI. Unauthorized distribution or reproduction is strictly prohibited.");
    }

    // Helper Methods

    private PdfPTable createGridTable(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(4);
        return table;
    }

    private void addGridRow(PdfPTable table, String lbl1, String val1, String lbl2, String val2) {
        addTableCell(table, lbl1, FONT_BODY_BOLD, BG_LIGHT, BORDER_LIGHT);
        addTableCell(table, val1, FONT_BODY, Color.WHITE, BORDER_LIGHT);
        addTableCell(table, lbl2, FONT_BODY_BOLD, BG_LIGHT, BORDER_LIGHT);
        addTableCell(table, val2, FONT_BODY, Color.WHITE, BORDER_LIGHT);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_BODY_BOLD));
            cell.setBackgroundColor(TABLE_HEADER_BG);
            cell.setBorderColor(BORDER_LIGHT);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        addTableCell(table, text, font, Color.WHITE, BORDER_LIGHT);
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bg, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String safe(String val) {
        return (val != null && !val.trim().isEmpty()) ? val : "N/A";
    }

    private String formatDate(OffsetDateTime dt) {
        return dt != null ? dt.format(DATE_FORMATTER) : "N/A";
    }

    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) return "None";
        return String.join(", ", list);
    }

    // Inner Footer Page Event
    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final String caseNumber;

        public HeaderFooterEvent(String caseNumber) {
            this.caseNumber = caseNumber != null ? caseNumber : "CS-REPORT";
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();

            String footerText = "Case: " + caseNumber + "  |  Confidential Investigation Document  |  Page " + writer.getPageNumber();

            cb.beginText();
            try {
                cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED), 7.5f);
            } catch (Exception e) {
                // Fallback
            }
            cb.setColorFill(new Color(120, 120, 125));
            cb.showTextAligned(Element.ALIGN_CENTER, footerText, (document.right() + document.left()) / 2, document.bottom() - 20, 0);
            cb.endText();

            cb.restoreState();
        }
    }
}
