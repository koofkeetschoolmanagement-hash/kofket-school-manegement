package com.example.core

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.AttendanceRecord
import com.example.data.model.ExamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class MonthlyReportData(
    val monthLabel: String,
    val totalStudents: Int,
    val totalTeachers: Int,
    val totalAssignments: Int,
    val totalExamsRecorded: Int,
    val averageExamScore: Double,
    val attendanceRatePercent: Double,
    val openEarlyWarnings: Int,
    val resolvedEarlyWarnings: Int,
    val studentRows: List<ReportStudentRow>,
    val warningRows: List<String>
)

data class ReportStudentRow(
    val studentName: String,
    val gradeLevel: String,
    val averageScore: Double,
    val attendancePercent: Double
)

object ReportExporter {

    suspend fun exportPdf(
        context: Context,
        uri: Uri,
        report: MonthlyReportData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to open PDF destination"))
            outputStream.use { stream ->
                writePdfToStream(stream, report)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportDocx(
        context: Context,
        uri: Uri,
        report: MonthlyReportData
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to open Word destination"))
            outputStream.use { stream ->
                writeDocxToStream(stream, report)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportCsv(
        context: Context,
        uri: Uri,
        report: MonthlyReportData,
        examResults: List<ExamResult> = emptyList(),
        attendance: List<AttendanceRecord> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to open CSV destination"))
            val csv = buildCsvContent(report, examResults, attendance)
            outputStream.use { stream ->
                // UTF-8 BOM for clean Excel / Google Sheets rendering of Amharic & Afaan Oromoo
                stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                stream.write(csv.toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildCsvContent(
        report: MonthlyReportData,
        examResults: List<ExamResult> = emptyList(),
        attendance: List<AttendanceRecord> = emptyList()
    ): String {
        fun esc(value: String): String {
            val clean = value.replace("\r\n", " ").replace("\n", " ")
            return "\"${clean.replace("\"", "\"\"")}\""
        }
        return buildString {
            appendLine("Section,Field,Value,Extra1,Extra2,Extra3,Extra4")
            appendLine("Summary,Month,${esc(report.monthLabel)},,,,")
            appendLine("Summary,Total Students,${report.totalStudents},,,,")
            appendLine("Summary,Total Teachers,${report.totalTeachers},,,,")
            appendLine("Summary,Assignments,${report.totalAssignments},,,,")
            appendLine("Summary,Exams Recorded,${report.totalExamsRecorded},,,,")
            appendLine("Summary,Average Exam Score,${"%.1f".format(report.averageExamScore)}%,,,,")
            appendLine("Summary,Attendance Rate,${"%.1f".format(report.attendanceRatePercent)}%,,,,")
            appendLine("Summary,Early Warnings Open,${report.openEarlyWarnings},Resolved,${report.resolvedEarlyWarnings},,")
            appendLine("")
            appendLine("Marklist Summary,Student Name,Grade Level,Average Score %,Attendance %,,")
            report.studentRows.forEach { row ->
                appendLine(
                    "Marklist Summary,${esc(row.studentName)},${esc(row.gradeLevel)},${"%.1f".format(row.averageScore)},${"%.1f".format(row.attendancePercent)},,"
                )
            }
            if (examResults.isNotEmpty()) {
                appendLine("")
                appendLine("Subject Grades,Student Name,Grade Level,Subject,Assessment,Month,Score %,Remarks")
                examResults.forEach { r ->
                    appendLine(
                        "Subject Grades,${esc(r.studentName)},${esc(r.gradeLevel)},${esc(r.subject)},${esc(r.examName)},${esc(r.monthLabel)},${"%.1f".format(r.score)},${esc(r.remarks)}"
                    )
                }
            }
            if (attendance.isNotEmpty()) {
                appendLine("")
                appendLine("Attendance History,Date,Student Name,Grade Level,Status,Note,")
                attendance.forEach { rec ->
                    appendLine(
                        "Attendance History,${esc(rec.dateLabel)},${esc(rec.studentName)},${esc(rec.gradeLevel)},${esc(rec.status)},${esc(rec.note)},"
                    )
                }
            }
        }
    }

    private fun writePdfToStream(stream: OutputStream, report: MonthlyReportData) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val headerBgPaint = Paint().apply { color = Color.rgb(15, 76, 58) }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(255, 229, 158)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 76, 58)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(20, 30, 26)
            textSize = 11f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(191, 203, 195)
            strokeWidth = 1f
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(90, 105, 96)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        // Header banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 92f, headerBgPaint)
        canvas.drawText("MANA BARUMSAA KOOFKEET - GABAASA JI'AA", 36f, 38f, titlePaint)
        canvas.drawText("\"Jalqabni Ogummaa Waaqayyoon Sodaachuudha.\" Fkn 1:7", 36f, 60f, subtitlePaint)
        canvas.drawText("Ji'a (Month): ${report.monthLabel}", 36f, 78f, subtitlePaint)

        var y = 122f
        canvas.drawText("1. Cuunfaa Waliigalaa (Executive Summary)", 36f, y, sectionPaint)
        y += 10f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 20f

        val kpis = listOf(
            "Baay'ina Barattootaa (Students): ${report.totalStudents}",
            "Baay'ina Barsiisotaa (Teachers): ${report.totalTeachers}",
            "Hojii Manaa Kenname (Assignments): ${report.totalAssignments}",
            "Qabxii Qormaataa Galmaa'e (Exam Records): ${report.totalExamsRecorded}",
            "Giddu-galeessa Qabxii (Average Score): ${"%.1f".format(report.averageExamScore)}%",
            "Argamaa Barattootaa (Attendance Rate): ${"%.1f".format(report.attendanceRatePercent)}%",
            "Deeggarsa Dursa-Beeksisaa (Early Warnings): ${report.openEarlyWarnings} banaa / ${report.resolvedEarlyWarnings} furame"
        )
        kpis.forEach { line ->
            canvas.drawText("• $line", 44f, y, bodyPaint)
            y += 18f
        }

        y += 12f
        canvas.drawText("2. Raawwii Barattootaa (Student Performance)", 36f, y, sectionPaint)
        y += 10f
        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 18f

        if (report.studentRows.isEmpty()) {
            canvas.drawText("Ragaan barattootaa ji'a kanaaf hin galmoofne.", 44f, y, bodyPaint)
            y += 18f
        } else {
            report.studentRows.forEach { row ->
                if (y > pageHeight - 90f) {
                    canvas.drawText(
                        "\"Milkaa'ina jechuun bakka tokko gahanii dhaabachuu utuun taane, adeemsa itti fufaadha.\" — Been Siwiitlaand",
                        36f,
                        pageHeight - 28f,
                        footerPaint
                    )
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 48f
                }
                val line = "${row.studentName} (${row.gradeLevel}) — Qabxii: ${"%.1f".format(row.averageScore)}% | Argamaa: ${"%.1f".format(row.attendancePercent)}%"
                canvas.drawText(line, 44f, y, bodyPaint)
                y += 17f
            }
        }

        if (report.warningRows.isNotEmpty()) {
            y += 12f
            if (y > pageHeight - 100f) {
                document.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 48f
            }
            canvas.drawText("3. Akeekkachiisa Dursa-Beeksisaa (Early Warnings)", 36f, y, sectionPaint)
            y += 10f
            canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
            y += 18f
            report.warningRows.forEach { warn ->
                if (y > pageHeight - 70f) {
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 48f
                }
                canvas.drawText("• $warn", 44f, y, bodyPaint)
                y += 17f
            }
        }

        canvas.drawText(
            "\"Milkaa'ina jechuun bakka tokko gahanii dhaabachuu utuun taane, adeemsa itti fufaadha.\" — Been Siwiitlaand",
            36f,
            pageHeight - 28f,
            footerPaint
        )
        document.finishPage(page)
        document.writeTo(stream)
        document.close()
    }

    private fun writeDocxToStream(stream: OutputStream, report: MonthlyReportData) {
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()

        val docBody = buildString {
            append(paragraph("MANA BARUMSAA KOOFKEET — GABAASA JI'AA (MONTHLY REPORT)", bold = true, sizeHalfPoints = 32, colorHex = "0F4C3A"))
            append(paragraph("\"Jalqabni Ogummaa Waaqayyoon Sodaachuudha.\" Fkn 1:7", italic = true, sizeHalfPoints = 22, colorHex = "B57F08"))
            append(paragraph("Ji'a (Month): ${report.monthLabel}", bold = true, sizeHalfPoints = 24))
            append(paragraph(""))
            append(paragraph("1. Cuunfaa Waliigalaa (Executive Summary)", bold = true, sizeHalfPoints = 26, colorHex = "0F4C3A"))
            append(paragraph("• Baay'ina Barattootaa (Students): ${report.totalStudents}"))
            append(paragraph("• Baay'ina Barsiisotaa (Teachers): ${report.totalTeachers}"))
            append(paragraph("• Hojii Manaa Kenname (Assignments): ${report.totalAssignments}"))
            append(paragraph("• Qabxii Qormaataa Galmaa'e (Exam Results): ${report.totalExamsRecorded}"))
            append(paragraph("• Giddu-galeessa Qabxii (Average Score): ${"%.1f".format(report.averageExamScore)}%"))
            append(paragraph("• Argamaa Barattootaa (Attendance Rate): ${"%.1f".format(report.attendanceRatePercent)}%"))
            append(paragraph("• Deeggarsa Dursa-Beeksisaa (Early Warnings): ${report.openEarlyWarnings} banaa / ${report.resolvedEarlyWarnings} furame"))
            append(paragraph(""))
            append(paragraph("2. Raawwii Barattootaa (Student Performance)", bold = true, sizeHalfPoints = 26, colorHex = "0F4C3A"))
            if (report.studentRows.isEmpty()) {
                append(paragraph("Ragaan barattootaa ji'a kanaaf hin galmoofne."))
            } else {
                report.studentRows.forEach { row ->
                    append(
                        paragraph(
                            "• ${row.studentName} (${row.gradeLevel}) — Qabxii: ${"%.1f".format(row.averageScore)}% | Argamaa: ${"%.1f".format(row.attendancePercent)}%"
                        )
                    )
                }
            }
            if (report.warningRows.isNotEmpty()) {
                append(paragraph(""))
                append(paragraph("3. Akeekkachiisa Dursa-Beeksisaa (Early Warnings)", bold = true, sizeHalfPoints = 26, colorHex = "B83227"))
                report.warningRows.forEach { warn ->
                    append(paragraph("• $warn"))
                }
            }
            append(paragraph(""))
            append(
                paragraph(
                    "\"Milkaa'ina jechuun bakka tokko gahanii dhaabachuu utuun taane, adeemsa itti fufaadha.\" — Been Siwiitlaand",
                    italic = true,
                    sizeHalfPoints = 20,
                    colorHex = "45574E"
                )
            )
        }

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
$docBody
    <w:sectPr/>
  </w:body>
</w:document>""".trimIndent()

        ZipOutputStream(stream).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            writeZipEntry(zos, "_rels/.rels", relsXml)
            writeZipEntry(zos, "word/document.xml", documentXml)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun paragraph(
        text: String,
        bold: Boolean = false,
        italic: Boolean = false,
        sizeHalfPoints: Int = 22,
        colorHex: String = "141E1A"
    ): String {
        val escaped = escapeXml(text)
        val rPr = buildString {
            append("<w:rPr>")
            if (bold) append("<w:b/>")
            if (italic) append("<w:i/>")
            append("<w:color w:val=\"$colorHex\"/>")
            append("<w:sz w:val=\"$sizeHalfPoints\"/>")
            append("</w:rPr>")
        }
        return "    <w:p><w:r>$rPr<w:t xml:space=\"preserve\">$escaped</w:t></w:r></w:p>\n"
    }

    private fun escapeXml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
