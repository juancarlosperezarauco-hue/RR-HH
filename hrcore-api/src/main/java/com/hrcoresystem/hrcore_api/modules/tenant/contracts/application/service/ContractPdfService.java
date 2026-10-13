package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.EmploymentContract;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.Department;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.Position;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ContractPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color NAVY = new Color(15, 59, 94);
    private static final Color LIGHT_BLUE = new Color(232, 243, 249);
    private final ContractPolicy policy;
    private final EmployeeRepository employees;
    private final EmployeeAssignmentRepository assignments;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final TenantSettingRepository settings;

    public ContractPdfService(ContractPolicy policy, EmployeeRepository employees,
                              EmployeeAssignmentRepository assignments, DepartmentRepository departments,
                              PositionRepository positions, TenantSettingRepository settings) {
        this.policy = policy; this.employees = employees; this.assignments = assignments;
        this.departments = departments; this.positions = positions; this.settings = settings;
    }

    public ContractDocument generate(java.util.UUID contractId) {
        policy.assertOperational();
        EmploymentContract contract = policy.require(contractId);
        Employee employee = employees.findById(contract.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for contract"));
        EmployeeAssignment assignment = contract.employeeAssignmentId() == null ? null
                : assignments.findByIdAndEmployeeId(contract.employeeAssignmentId(), employee.id()).orElse(null);
        Department department = assignment == null ? null : departments.findById(assignment.departmentId()).orElse(null);
        Position position = assignment == null ? null : positions.findById(assignment.positionId()).orElse(null);
        String company = settings.findByKey("company.name").map(value -> value.settingValue())
                .filter(value -> value != null && !value.isBlank()).orElse("Organización contratante");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 48, 48, 46, 48);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        writer.setPageEvent(new ContractPageEvent(company, contract.contractNumber()));
        document.open();
        addHeader(document, company, contract);
        addIntroduction(document, company, employee);
        addDetails(document, contract, employee, department, position);
        addTerms(document, contract);
        addSignatures(document, company, employee);
        document.close();
        String safeNumber = contract.contractNumber().replaceAll("[^A-Za-z0-9._-]", "_");
        return new ContractDocument(output.toByteArray(), "contrato-" + safeNumber + ".pdf");
    }

    private void addHeader(Document document, String company, EmploymentContract contract) throws DocumentException {
        Font eyebrow = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NAVY);
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
        Font subtitle = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Paragraph brand = new Paragraph(company.toUpperCase(Locale.ROOT), eyebrow);
        brand.setAlignment(Element.ALIGN_CENTER); document.add(brand);
        Paragraph heading = new Paragraph("CONTRATO INDIVIDUAL DE TRABAJO", title);
        heading.setAlignment(Element.ALIGN_CENTER); heading.setSpacingBefore(5); document.add(heading);
        Paragraph number = new Paragraph("N.º " + contract.contractNumber() + "  ·  Estado: " + status(contract), subtitle);
        number.setAlignment(Element.ALIGN_CENTER); number.setSpacingAfter(18); document.add(number);
        if (contract.status() == com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.ContractStatus.DRAFT) {
            PdfPTable warning = new PdfPTable(1); warning.setWidthPercentage(100);
            PdfPCell cell = new PdfPCell(new Phrase("DOCUMENTO EN BORRADOR — SUJETO A REVISIÓN", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(146, 86, 0))));
            cell.setBackgroundColor(new Color(255, 244, 214)); cell.setBorderColor(new Color(238, 190, 87));
            cell.setPadding(8); cell.setHorizontalAlignment(Element.ALIGN_CENTER); warning.addCell(cell);
            warning.setSpacingAfter(15); document.add(warning);
        }
    }

    private void addIntroduction(Document document, String company, Employee employee) throws DocumentException {
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, Color.BLACK);
        Paragraph paragraph = new Paragraph(); paragraph.setAlignment(Element.ALIGN_JUSTIFIED); paragraph.setLeading(16);
        paragraph.add("Conste por el presente contrato individual de trabajo, celebrado entre ");
        paragraph.add(new Chunk(company, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f)));
        paragraph.add(", en adelante EL EMPLEADOR, y ");
        paragraph.add(new Chunk(fullName(employee), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f)));
        paragraph.add(", con documento " + employee.documentType() + " N.º " + employee.documentNumber()
                + ", en adelante EL TRABAJADOR, bajo las siguientes condiciones:");
        paragraph.setFont(body); paragraph.setSpacingAfter(15); document.add(paragraph);
    }

    private void addDetails(Document document, EmploymentContract c, Employee e,
                            Department department, Position position) throws DocumentException {
        addSectionTitle(document, "1. DATOS Y CONDICIONES DE CONTRATACIÓN");
        PdfPTable table = new PdfPTable(new float[]{1.35f, 2.65f}); table.setWidthPercentage(100); table.setSpacingAfter(15);
        row(table, "Trabajador", fullName(e)); row(table, "Documento", e.documentType() + " " + e.documentNumber());
        row(table, "Departamento", department == null ? "No especificado" : department.name());
        row(table, "Cargo o puesto", position == null ? "No especificado" : position.name());
        row(table, "Tipo de contrato", contractType(c)); row(table, "Forma", c.contractForm() == null ? "Escrito" : label(c.contractForm().name()));
        row(table, "Jornada", c.workSchedule() == null ? "Tiempo completo" : label(c.workSchedule().name()));
        row(table, "Modalidad", c.workModality() == null ? "Presencial" : label(c.workModality().name()));
        row(table, "Vigencia", DATE.format(c.startDate()) + " al " + (c.endDate() == null ? "tiempo indefinido" : DATE.format(c.endDate())));
        row(table, "Remuneración", money(c.baseSalary(), c.currency()) + " · " + payment(c));
        row(table, "Horas semanales", c.weeklyHours() == null ? "No especificadas" : c.weeklyHours().stripTrailingZeros().toPlainString());
        if (c.probationEndDate() != null) row(table, "Periodo de prueba hasta", DATE.format(c.probationEndDate()));
        document.add(table);
    }

    private void addTerms(Document document, EmploymentContract c) throws DocumentException {
        addSectionTitle(document, "2. OBJETO, OBLIGACIONES Y CONDICIONES");
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, Color.BLACK);
        String terms = c.terms() == null || c.terms().isBlank()
                ? "EL TRABAJADOR se compromete a desempeñar las funciones inherentes al cargo asignado, cumplir la normativa interna y actuar con diligencia y confidencialidad. EL EMPLEADOR se compromete a pagar la remuneración acordada y otorgar los beneficios reconocidos por la legislación laboral aplicable."
                : c.terms();
        Paragraph paragraph = new Paragraph(terms, body); paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        paragraph.setLeading(16); paragraph.setSpacingAfter(12); document.add(paragraph);
        Paragraph acceptance = new Paragraph("Las partes declaran haber leído y aceptado el contenido del presente documento, firmando dos ejemplares del mismo tenor.", body);
        acceptance.setAlignment(Element.ALIGN_JUSTIFIED); acceptance.setLeading(16); acceptance.setSpacingAfter(40); document.add(acceptance);
    }

    private void addSignatures(Document document, String company, Employee employee) throws DocumentException {
        PdfPTable table = new PdfPTable(2); table.setWidthPercentage(100); table.setWidths(new float[]{1, 1});
        table.setSpacingBefore(25);
        signatureCell(table, company, "EL EMPLEADOR");
        signatureCell(table, fullName(employee) + "\n" + employee.documentType() + " " + employee.documentNumber(), "EL TRABAJADOR");
        document.add(table);
        Paragraph place = new Paragraph("Lugar y fecha: ________________________________________________",
                FontFactory.getFont(FontFactory.HELVETICA, 9.5f));
        place.setSpacingBefore(28); document.add(place);
    }

    private void addSectionTitle(Document document, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, NAVY));
        title.setSpacingBefore(4); title.setSpacingAfter(8); document.add(title);
    }
    private void row(PdfPTable table, String label, String value) {
        PdfPCell left = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, NAVY)));
        left.setBackgroundColor(LIGHT_BLUE); left.setPadding(7); left.setBorderColor(Color.WHITE);
        PdfPCell right = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9.5f)));
        right.setPadding(7); right.setBorderColor(new Color(224, 229, 234)); table.addCell(left); table.addCell(right);
    }
    private void signatureCell(PdfPTable table, String name, String role) {
        PdfPCell cell = new PdfPCell(); cell.setBorder(Rectangle.NO_BORDER); cell.setPadding(12);
        cell.addElement(new Paragraph("________________________________", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        Paragraph n = new Paragraph(name, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)); n.setAlignment(Element.ALIGN_CENTER); cell.addElement(n);
        Paragraph r = new Paragraph(role, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY)); r.setAlignment(Element.ALIGN_CENTER); cell.addElement(r); table.addCell(cell);
    }
    private String fullName(Employee e) { return String.join(" ", java.util.stream.Stream.of(e.firstName(), e.middleName(), e.lastName(), e.secondLastName()).filter(v -> v != null && !v.isBlank()).toList()); }
    private String money(BigDecimal value, String currency) { return NumberFormat.getNumberInstance(new Locale("es", "BO")).format(value) + " " + currency; }
    private String payment(EmploymentContract c) { return switch (c.paymentFrequency()) { case MONTHLY -> "pago mensual"; case BIWEEKLY -> "pago quincenal"; case WEEKLY -> "pago semanal"; case DAILY -> "pago diario"; }; }
    private String status(EmploymentContract c) { return label(c.status().name()); }
    private String contractType(EmploymentContract c) { return switch (c.contractType()) { case INDEFINITE -> "Tiempo indefinido"; case FIXED_TERM -> "Plazo fijo"; case SEASONAL -> "Temporada"; case WORK_OR_SERVICE -> "Obra o servicio"; case EVENTUAL, TEMPORARY -> "Eventual o temporal"; case INTERNSHIP -> "Pasantía"; case PART_TIME -> "Tiempo parcial (heredado)"; case OTHER -> "Otro"; }; }
    private String label(String value) { return switch (value) { case "WRITTEN" -> "Escrito"; case "VERBAL" -> "Verbal"; case "FULL_TIME" -> "Tiempo completo"; case "PART_TIME" -> "Tiempo parcial"; case "ONSITE" -> "Presencial"; case "REMOTE" -> "Remoto"; case "HYBRID" -> "Híbrido"; case "DRAFT" -> "Borrador"; case "PENDING_SIGNATURE" -> "Pendiente de firma"; case "ACTIVE" -> "Activo"; case "EXPIRED" -> "Vencido"; case "TERMINATED" -> "Finalizado"; case "CANCELLED" -> "Cancelado"; default -> value; }; }

    public record ContractDocument(byte[] content, String filename) { }

    private static class ContractPageEvent extends PdfPageEventHelper {
        private final String company; private final String number;
        private ContractPageEvent(String company, String number) { this.company = company; this.number = number; }
        @Override public void onEndPage(PdfWriter writer, Document document) {
            Phrase footer = new Phrase(company + " · Contrato " + number + " · Página " + writer.getPageNumber(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2, document.bottom() - 22, 0);
        }
    }
}
