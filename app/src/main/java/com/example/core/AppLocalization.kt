package com.example.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val badgeCode: String
) {
    AFAAN_OROMOO(code = "om", nativeName = "Afaan Oromoo", badgeCode = "OM"),
    ENGLISH(code = "en", nativeName = "English", badgeCode = "EN"),
    AMHARIC(code = "am", nativeName = "አማርኛ", badgeCode = "AM");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: AFAAN_OROMOO
    }
}

data class LocalizedLoginStrings(
    val languageSelectorLabel: String,
    val schoolBadge: String,
    val schoolFullTitle: String,
    val schoolMotto: String,
    val loginTitle: String,
    val loginSubtitle: String,
    val roleStudent: String,
    val roleTeacher: String,
    val roleParent: String,
    val roleAdmin: String,
    val privacyNotice: String,
    val signInWithGoogle: String,
    val signingIn: String,
    val signInCancelledHint: String,
    val signInNote: String,
    val footerQuote: String,
    // Onboarding localization
    val onboardingWelcome: String,
    val onboardingSubtitle: String,
    val onboardingRoleAdmin: String,
    val onboardingRoleAssignedPrefix: String,
    val onboardingRoleFirstAdmin: String,
    val onboardingRoleStudent: String,
    val fullNameLabel: String,
    val initialSetupLabel: String,
    val gradeLevelLabel: String,
    val continueButton: String,
    val switchAccountButton: String,
    // Top bar & language dialog
    val signOutLabel: String,
    val chooseLanguageTitle: String,
    val closeLabel: String
)

fun stringsFor(language: AppLanguage): LocalizedLoginStrings = when (language) {
    AppLanguage.AFAAN_OROMOO -> LocalizedLoginStrings(
        languageSelectorLabel = "Afaan Filadhu",
        schoolBadge = "KOOFKEET SCHOOL",
        schoolFullTitle = "Application Mana Barumsaa Koofkeet",
        schoolMotto = "\"Jalqabni Ogummaa Waaqayyoon Sodaachuudha.\" Fkn 1:7",
        loginTitle = "Baga Nagaan Dhuftan • Seensa",
        loginSubtitle = "Barattoota, barsiisota, maatii barattootaa fi bulchiinsa mana barumsaa bakka tokkotti kan walitti fidu.",
        roleStudent = "Barataa",
        roleTeacher = "Barsiisaa",
        roleParent = "Maatii",
        roleAdmin = "Bulchiinsa",
        privacyNotice = "Eegumsa Odeeffannoo: Namni hundi gahee isaaf kenname qofaan tajaajila argata.",
        signInWithGoogle = "Google-n Seenaa",
        signingIn = "Seenaa jira...",
        signInCancelledHint = "Seensi addaan citeera. Itti fufuuf maaloo account Google filadhaa.",
        signInNote = "Firebase Authentication fi Google-n nageenyaan seenaa.",
        footerQuote = "\"Milkaa'ina jechuun bakka tokko gahanii dhaabachuu utuun taane, adeemsa itti fufaadha.\" — Been Siwiitlaand",
        onboardingWelcome = "Baga Nagaan Dhuftan!",
        onboardingSubtitle = "Eegumsa odeeffannoo fi mirga fayyadamaa mirkaneessuuf odeeffannoo armaan gadii guutaa.",
        onboardingRoleAdmin = "Gahee Keessan: Bulchiinsa Mana Barumsaa (Admin)",
        onboardingRoleAssignedPrefix = "Gahee Ramadame: ",
        onboardingRoleFirstAdmin = "Mana barumsaa jalqabsiisuuf Admin ta'uu dandeessu",
        onboardingRoleStudent = "Gahee Keessan: Barataa (Student)",
        fullNameLabel = "Maqaa Guutuu (Full Name)",
        initialSetupLabel = "Filannoo Jalqabsiisaa (Initial Setup):",
        gradeLevelLabel = "Kutaa Barumsaa (Grade Level):",
        continueButton = "Itti Fufi (Enter Koofkeet School)",
        switchAccountButton = "Account Jijjiiri (Switch Account)",
        signOutLabel = "Ba'i (Sign Out)",
        chooseLanguageTitle = "Afaan Jijjiiri / Select Language",
        closeLabel = "Cufi"
    )

    AppLanguage.ENGLISH -> LocalizedLoginStrings(
        languageSelectorLabel = "Select Language",
        schoolBadge = "KOOFKEET SCHOOL",
        schoolFullTitle = "Kofket School Management System",
        schoolMotto = "\"The fear of the LORD is the beginning of wisdom.\" Prov 1:7",
        loginTitle = "Welcome Back • Sign In",
        loginSubtitle = "Connecting students, teachers, parents, and school administration securely in one place.",
        roleStudent = "Student",
        roleTeacher = "Teacher",
        roleParent = "Parent",
        roleAdmin = "Admin",
        privacyNotice = "Privacy & Role Protection: Each user only accesses records permitted for their assigned role.",
        signInWithGoogle = "Sign in with Google",
        signingIn = "Signing in...",
        signInCancelledHint = "Sign-in was cancelled. Please select a Google account to continue.",
        signInNote = "Secured with Firebase Authentication & Google Sign-In.",
        footerQuote = "\"Success is a journey, not a destination.\" — Ben Sweetland",
        onboardingWelcome = "Welcome to Koofkeet School!",
        onboardingSubtitle = "Complete your profile details below to set up your role and workspace.",
        onboardingRoleAdmin = "Your Role: School Administrator",
        onboardingRoleAssignedPrefix = "Assigned Role: ",
        onboardingRoleFirstAdmin = "Initial setup: you can register as the School Admin",
        onboardingRoleStudent = "Your Role: Student",
        fullNameLabel = "Full Name",
        initialSetupLabel = "Initial Setup Role:",
        gradeLevelLabel = "Grade Level:",
        continueButton = "Continue to Koofkeet School",
        switchAccountButton = "Switch Google Account",
        signOutLabel = "Sign Out",
        chooseLanguageTitle = "Select Language",
        closeLabel = "Close"
    )

    AppLanguage.AMHARIC -> LocalizedLoginStrings(
        languageSelectorLabel = "ቋንቋ ይምረጡ",
        schoolBadge = "KOOFKEET SCHOOL",
        schoolFullTitle = "የኮፍኬት ትምህርት ቤት አስተዳደር ሲስተም",
        schoolMotto = "\"የጥበብ መጀመሪያ እግዚአብሔርን መፍራት ነው።\" ምሳሌ 1:7",
        loginTitle = "እንኳን ደህና መጡ • መግቢያ",
        loginSubtitle = "ተማሪዎችን፣ መምህራንን፣ ወላጆችን እና የትምህርት ቤቱን አስተዳደር በአንድ ላይ የሚያገናኝ ዘመናዊ ሲስተም።",
        roleStudent = "ተማሪ",
        roleTeacher = "መምህር",
        roleParent = "ወላጅ",
        roleAdmin = "አስተዳደር",
        privacyNotice = "የመረጃ ጥበቃ፡ እያንዳንዱ ተጠቃሚ ለተሰጠው ሚና የተፈቀደውን መረጃ ብቻ ያገኛል።",
        signInWithGoogle = "በGoogle ይግቡ",
        signingIn = "በመግባት ላይ...",
        signInCancelledHint = "መግባቱ ተቋርጧል። ለመቀጠል እባክዎ የGoogle መለያ ይምረጡ።",
        signInNote = "በFirebase Authentication እና Google ደህንነቱ የተጠበቀ መግቢያ።",
        footerQuote = "\"ስኬት መዳረሻ ሳይሆን ጉዞ ነው።\" — ቤን ስዊትላንድ",
        onboardingWelcome = "እንኳን ወደ ኮፍኬት ትምህርት ቤት በደህና መጡ!",
        onboardingSubtitle = "የተጠቃሚ መገለጫዎን ለማዘጋጀት ከታች ያለውን መረጃ ይሙሉ።",
        onboardingRoleAdmin = "የእርስዎ ሚና፡ የትምህርት ቤት አስተዳዳሪ (Admin)",
        onboardingRoleAssignedPrefix = "የተመደበ ሚና፡ ",
        onboardingRoleFirstAdmin = "የመጀመሪያ ማዋቀር፡ የትምህርት ቤት አስተዳዳሪ መሆን ይችላሉ",
        onboardingRoleStudent = "የእርስዎ ሚና፡ ተማሪ (Student)",
        fullNameLabel = "ሙሉ ስም (Full Name)",
        initialSetupLabel = "የመጀመሪያ ሚና ምርጫ፡",
        gradeLevelLabel = "የክፍል ደረጃ (Grade Level)፡",
        continueButton = "ቀጥል (ወደ ትምህርት ቤቱ ግባ)",
        switchAccountButton = "መለያ ቀይር (Switch Account)",
        signOutLabel = "ውጣ (Sign Out)",
        chooseLanguageTitle = "ቋንቋ ይምረጡ / Select Language",
        closeLabel = "ዝጋ"
    )
}

private const val PREFS_NAME = "koofkeet_prefs"
private const val KEY_LANGUAGE = "selected_language"

fun loadSavedLanguage(context: Context): AppLanguage {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.AFAAN_OROMOO.code))
}

fun saveLanguagePreference(context: Context, language: AppLanguage) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LANGUAGE, language.code)
        .apply()
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.AFAAN_OROMOO }

@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppLanguage provides language) {
        content()
    }
}

@Composable
fun rememberAppLanguageState(): Pair<AppLanguage, (AppLanguage) -> Unit> {
    val context = LocalContext.current
    var current by remember { mutableStateOf(loadSavedLanguage(context)) }
    val update: (AppLanguage) -> Unit = { newLang ->
        current = newLang
        saveLanguagePreference(context, newLang)
    }
    return current to update
}
