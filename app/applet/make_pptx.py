from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

prs = Presentation()
prs.slide_width = Inches(10)
prs.slide_height = Inches(5.625)

BG_COLOR = RGBColor(245, 247, 245)
PRIMARY_COLOR = RGBColor(15, 76, 58)
SECONDARY_COLOR = RGBColor(198, 145, 39)
TEXT_COLOR = RGBColor(30, 40, 35)
WHITE = RGBColor(255, 255, 255)
FOOTER_TEXT = "Qopheessaan Fayyeeraa Taaddasaa"

def add_slide_header_footer(slide, title_text):
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(10), Inches(5.625))
    bg.fill.solid()
    bg.fill.fore_color.rgb = BG_COLOR
    bg.line.color.rgb = BG_COLOR

    header = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(10), Inches(0.8))
    header.fill.solid()
    header.fill.fore_color.rgb = PRIMARY_COLOR
    header.line.color.rgb = PRIMARY_COLOR

    tf = header.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = f"KOOFKEET SCHOOL APP - {title_text}"
    p.font.size = Pt(20)
    p.font.bold = True
    p.font.color.rgb = WHITE
    p.alignment = PP_ALIGN.LEFT
    p.font.name = "Arial"

    footer = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, Inches(5.2), Inches(10), Inches(0.425))
    footer.fill.solid()
    footer.fill.fore_color.rgb = SECONDARY_COLOR
    footer.line.color.rgb = SECONDARY_COLOR

    ftf = footer.text_frame
    fp = ftf.paragraphs[0]
    fp.text = f"Maanuwaalii Professional • {FOOTER_TEXT}"
    fp.font.size = Pt(11)
    fp.font.bold = True
    fp.font.color.rgb = WHITE
    fp.alignment = PP_ALIGN.CENTER
    fp.font.name = "Arial"

blank_layout = prs.slide_layouts[6]
slide1 = prs.slides.add_slide(blank_layout)

bg1 = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(10), Inches(5.625))
bg1.fill.solid()
bg1.fill.fore_color.rgb = PRIMARY_COLOR
bg1.line.color.rgb = PRIMARY_COLOR

tb1 = slide1.shapes.add_textbox(Inches(1), Inches(1), Inches(8), Inches(3.2))
tf1 = tb1.text_frame
tf1.word_wrap = True

p = tf1.paragraphs[0]
p.text = "Maanuwaalii Manajimantii Mana Barumsaa Koofkeet"
p.font.size = Pt(28)
p.font.bold = True
p.font.color.rgb = SECONDARY_COLOR
p.alignment = PP_ALIGN.CENTER

p2 = tf1.add_paragraph()
p2.text = "Professional User Manual & System Guide for All Files and Modules"
p2.font.size = Pt(16)
p2.font.color.rgb = WHITE
p2.alignment = PP_ALIGN.CENTER

p3 = tf1.add_paragraph()
p3.text = f"\nFooter / Qopheessaa:\n{FOOTER_TEXT}"
p3.font.size = Pt(14)
p3.font.bold = True
p3.font.color.rgb = WHITE
p3.alignment = PP_ALIGN.CENTER

slides_data = [
    ("Caasaa Faayilootaa fi Architecture", [
        "1. Core Models & Data: SchoolModels.kt, AppLocalization.kt (Afaan Oromoo, English, Amharic translations).",
        "2. Repositories: SchoolRepository.kt, CommunicationAndRecordsRepository.kt (Firestore Cloud & offline caching).",
        "3. ViewModel & State: SchoolViewModel.kt manages reactive flows for students, grades, attendance, AI chats, and calendar events.",
        "4. Screens & UI (Jetpack Compose): DashboardScreen.kt, AcademicsScreen.kt, MarklistScreen.kt, AttendanceScreen.kt, AdminManagementScreen.kt, AiTutorScreen.kt.",
        "5. Export & Sync Utilities: ReportExporter.kt (PDF, Word, CSV exports), CalendarSyncHelper.kt (.ics & device calendar sync)."
    ]),
    ("Bulchiinsa fi Galmee (Admin & User Role Management)", [
        "• Google Sign-In & Auth: Secure authentication via Jetpack Credential Manager linked to Firestore user profiles.",
        "• Role Distinction: Automatically distinguishes between Admin, Teacher, Student, and Parent accounts.",
        "• Navigation Guard: Checks user role from Firestore upon login and redirects to the appropriate dashboard securely.",
        "• Profile Setup: Triggers after first login to configure display name, grade level, and language preference (Oromoo, English, Amharic).",
        "• Admin Management Screen: Allows admins to issue role grants, register students, assign subjects to teachers, and export monthly reports."
    ]),
    ("To'annoo Argamaa fi Sync (Attendance & Cloud Sync)", [
        "• Daily Attendance Tracking: Teachers can mark students as Present, Absent, or Late daily with deterministic Firestore doc IDs.",
        "• Batch Actions: One-tap 'Mark All Present' for fast roll-calling.",
        "• Attendance Sync Status Bar: Tracks cloud connectivity in real time (Synced, Offline Pending writes count, or Syncing phase).",
        "• Manual Server Sync: Teachers and admins can trigger manual syncAttendanceWithServer anytime."
    ]),
    ("Qabxii fi Curriculum (Grades & Curriculum)", [
        "• Official Curriculum Subjects: Exact Grade 1-6 subjects (Afaan Oromoo, English, Afaan Amaaraa, Saayinsii Naannoo, Herrega, Gadaafi Safuu, Og-aartii, FJQ) and Grade 7-8 subjects.",
        "• Teacher Grade Recording: Teachers record assessment scores and remarks into Firestore.",
        "• Marklist Table View: Comprehensive student grades list view with automatic averages and filtering.",
        "• Assignments & Quizzes: Interactive homework creation and student submission tracking."
    ]),
    ("AI Tutor • Gargaarsa Barattootaa (Private AI Tutor)", [
        "• Firebase AI Logic Integration: Powered by Firebase AI Logic (Firebase.ai) for fast, secure cloud AI responses.",
        "• Private Student Support: All AI interactions are securely stored in private student subcollections (users/{uid}/ai_chats).",
        "• Study Support Modes: Supports multiple modes including Explain Concept, Practice Quiz, and Summarize Notes.",
        "• Multilingual & Grade-Aware: Tailors responses to the student's grade level and preferred language."
    ]),
    ("Kalandarii fi Gabaasa Baasuu (Calendar & Export)", [
        "• Multi-Format Report Export: Export monthly executive and student performance reports as PDF, Word (.docx), or CSV files.",
        "• School Calendar Events: Firestore collection /school_events/{eventId} for school-wide calendar coordination.",
        "• Academic Sync: One-tap synchronization of assignments and quizzes to the school calendar.",
        "• Device Calendar & .ics Export: Zero-permission insertion into Android system calendar and RFC 5545 .ics file export."
    ]),
    ("Goolaba & Galata (Conclusion & Attribution)", [
        "• Enterprise-Grade Architecture: Built with Kotlin, Jetpack Compose M3, Firestore, and Firebase AI Logic.",
        "• Fully Localized: Complete Afaan Oromoo, English, and Amharic support.",
        "• Secure & Reliable: Strict security rules, offline persistence, and zero-permission report/calendar exports.",
        f"• Qopheessaa (Author / Creator): {FOOTER_TEXT}",
        "• System Status: Thank you for using Koofkeet School App Management System!"
    ])
]

for section_title, line_list in slides_data:
    slide = prs.slides.add_slide(blank_layout)
    add_slide_header_footer(slide, section_title)
    tb = slide.shapes.add_textbox(Inches(0.8), Inches(1.0), Inches(8.4), Inches(3.8))
    tf = tb.text_frame
    tf.word_wrap = True
    for single_line in line_list:
        p = tf.add_paragraph()
        p.text = single_line
        p.font.size = Pt(13)
        p.font.color.rgb = TEXT_COLOR
        p.space_after = Pt(8)

prs.save("Kofket_School_App_User_Manual.pptx")
print("Successfully generated Kofket_School_App_User_Manual.pptx")
