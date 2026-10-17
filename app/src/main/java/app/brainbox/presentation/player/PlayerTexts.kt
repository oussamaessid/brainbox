package app.brainbox.presentation.player

import app.brainbox.data.player.ProfileError
import app.brainbox.domain.model.LeaderboardPeriod
import app.brainbox.domain.repository.Language

/** Textes des écrans joueur, dans la langue du téléphone. */
class PlayerTexts(val language: Language) {
    private fun t(fr: String, en: String, ar: String) = when (language) {
        Language.FRENCH -> fr
        Language.ENGLISH -> en
        Language.ARABIC -> ar
    }

    val welcomeTitle = t("Bienvenue ! 👋", "Welcome! 👋", "!مرحباً 👋")
    val welcomeText = t(
        "Votre compte joueur a été créé automatiquement. Gardez ce pseudo et cet avatar ou personnalisez-les pour apparaître dans le classement.",
        "Your player account was created automatically. Keep this name and avatar or customize them to appear on the leaderboard.",
        "تم إنشاء حسابك تلقائياً. احتفظ بهذا الاسم والصورة أو غيّرهما لتظهر في الترتيب."
    )
    val profileTitle = t("Mon profil", "My profile", "ملفي الشخصي")
    val profileText = t(
        "Un pseudo et un avatar pour vous retrouver dans le classement.",
        "A name and an avatar so others can find you on the leaderboard.",
        "اسم وصورة ليجدك الآخرون في الترتيب."
    )
    val nameLabel = t("Pseudo", "Name", "الاسم")
    val nameHelp = t("De 2 à 24 caractères", "2 to 24 characters", "من 2 إلى 24 حرفاً")
    val chooseAvatar = t("Choisissez votre avatar", "Choose your avatar", "اختر صورتك")
    val save = t("ENREGISTRER", "SAVE", "حفظ")
    val saving = t("Enregistrement…", "Saving…", "…جارٍ الحفظ")
    val keepDefault = t("Continuer avec ce profil", "Continue with this profile", "المتابعة بهذا الملف")
    val guestNote = t(
        "Compte invité conservé sur cet appareil : effacer les données de l'app le supprime.",
        "Guest account kept on this device: clearing the app data deletes it.",
        "حساب ضيف محفوظ على هذا الجهاز: مسح بيانات التطبيق يحذفه."
    )

    fun error(error: ProfileError) = when (error) {
        ProfileError.NAME_TAKEN -> t(
            "Un autre joueur a déjà ce pseudo avec cet avatar. Changez de pseudo ou d'avatar.",
            "Another player already has this name with this avatar. Change the name or the avatar.",
            "لاعب آخر يستخدم هذا الاسم مع هذه الصورة. غيّر الاسم أو الصورة."
        )
        ProfileError.OFFLINE -> t(
            "Pas de connexion : vérifiez Internet pour valider ce pseudo.",
            "No connection: check your internet to confirm this name.",
            "لا يوجد اتصال: تحقق من الإنترنت لتأكيد الاسم."
        )
        ProfileError.INVALID -> nameHelp
        ProfileError.UNKNOWN -> t(
            "Le pseudo n'a pas pu être vérifié. Réessayez dans un instant.",
            "The name couldn't be checked. Try again in a moment.",
            "تعذر التحقق من الاسم. حاول مرة أخرى بعد قليل."
        )
    }

    val leaderboardTitle = t("🏆 CLASSEMENT", "🏆 LEADERBOARD", "🏆 الترتيب")
    fun period(period: LeaderboardPeriod) = when (period) {
        LeaderboardPeriod.DAY -> t("Jour", "Day", "اليوم")
        LeaderboardPeriod.WEEK -> t("Semaine", "Week", "الأسبوع")
        LeaderboardPeriod.MONTH -> t("Mois", "Month", "الشهر")
    }
    val allLanguages = t("Toutes", "All", "الكل")
    val points = t("pts", "pts", "نقطة")
    fun wins(count: Int) = if (count == 1) t("1 victoire", "1 win", "انتصار واحد")
        else t("$count victoires", "$count wins", "$count انتصارات")
    val you = t("Vous", "You", "أنت")
    val myRank = t("Ma position", "My rank", "ترتيبي")
    val notRanked = t(
        "Gagnez un défi pour entrer dans ce classement !",
        "Win a challenge to enter this leaderboard!",
        "اربح تحدياً لتدخل هذا الترتيب!"
    )
    val empty = t(
        "Personne n'est encore classé. Soyez le premier ! 🚀",
        "Nobody is ranked yet. Be the first! 🚀",
        "لا أحد في الترتيب بعد. كن الأول! 🚀"
    )
    val offline = t(
        "Hors-ligne : dernier classement enregistré.",
        "Offline: last saved leaderboard.",
        "غير متصل: آخر ترتيب محفوظ."
    )
    val loadError = t(
        "Classement indisponible. Vérifiez votre connexion.",
        "Leaderboard unavailable. Check your connection.",
        "الترتيب غير متاح. تحقق من اتصالك."
    )
    val retry = t("Réessayer", "Retry", "إعادة المحاولة")
}
