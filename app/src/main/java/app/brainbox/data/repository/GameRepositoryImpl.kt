package app.brainbox.data.repository

import app.brainbox.domain.model.Category
import app.brainbox.domain.model.DailyChallenge
import app.brainbox.domain.repository.GameRepository
import app.brainbox.domain.repository.Language
import java.text.SimpleDateFormat
import java.util.*

class GameRepositoryImpl : GameRepository {

    private val startDate = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 28) // 28/01/2026
    }

    override fun getDailyChallenges(language: Language): Map<String, DailyChallenge> {
        val categories = when (language) {
            Language.FRENCH -> getFrenchCategories()
            Language.ENGLISH -> getEnglishCategories()
            Language.ARABIC -> getArabicCategories()
        }

        return generateDailyChallenges(categories)
    }

    override fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun generateDailyChallenges(categories: List<Category>): Map<String, DailyChallenge> {
        val challenges = mutableMapOf<String, DailyChallenge>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = startDate.clone() as Calendar

        categories.forEachIndexed { index, category ->
            val dateString = dateFormat.format(calendar.time)
            challenges[dateString] = DailyChallenge(
                date = dateString,
                categories = listOf(category)
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return challenges
    }

    private fun getFrenchCategories(): List<Category> {
        return listOf(
            Category("Fruits", listOf("Pomme", "Banane", "Raisin", "Pêche", "Datte")),
            Category("Couleurs", listOf("Rouge", "Bleu", "Jaune", "Vert", "Noir")),
            Category("Métiers", listOf("Médecin", "Professeur", "Infirmier", "Chef", "Pilote")),
            Category("Pays", listOf("Égypte", "France", "Japon", "Brésil", "Canada")),
            Category("Légumes", listOf("Carotte", "Pomme de terre", "Oignon", "Ail", "Tomate")),
            Category("Animaux", listOf("Lion", "Tigre", "Zèbre", "Cheval", "Chameau")),
            Category("Oiseaux", listOf("Aigle", "Faucon", "Perroquet", "Hibou", "Canard")),
            Category("Objets", listOf("Chaise", "Table", "Fenêtre", "Porte", "Horloge")),
            Category("Planètes", listOf("Mars", "Vénus", "Terre", "Saturne", "Jupiter")),
            Category("Métaux", listOf("Or", "Argent", "Fer", "Cuivre", "Plomb")),
            Category("Nature", listOf("Rivière", "Lac", "Mer", "Montagne", "Colline")),
            Category("Sports", listOf("Tennis", "Football", "Golf", "Rugby", "Judo"))
        )
    }

    private fun getEnglishCategories(): List<Category> {
        return listOf(
            Category("Fruits", listOf("Apple", "Banana", "Grape", "Peach", "Date")),
            Category("Colors", listOf("Red", "Blue", "Yellow", "Green", "Black")),
            Category("Jobs", listOf("Doctor", "Teacher", "Nurse", "Chef", "Pilot")),
            Category("Countries", listOf("Egypt", "France", "Japan", "Brazil", "Canada")),
            Category("Vegetables", listOf("Carrot", "Potato", "Onion", "Garlic", "Tomato")),
            Category("Animals", listOf("Lion", "Tiger", "Zebra", "Horse", "Camel")),
            Category("Birds", listOf("Eagle", "Falcon", "Parrot", "Owl", "Duck")),
            Category("Objects", listOf("Chair", "Table", "Window", "Door", "Clock")),
            Category("Planets", listOf("Mars", "Venus", "Earth", "Saturn", "Jupiter")),
            Category("Metals", listOf("Gold", "Silver", "Iron", "Copper", "Lead")),
            Category("Nature", listOf("River", "Lake", "Sea", "Mountain", "Hill")),
            Category("Sports", listOf("Tennis", "Soccer", "Golf", "Rugby", "Judo"))
        )
    }

    private fun getArabicCategories(): List<Category> {
        return listOf(
            Category("فواكه", listOf("تفاح", "موز", "عنب", "خوخ", "تمر")),
            Category("ألوان", listOf("أحمر", "أزرق", "أصفر", "أخضر", "أسود")),
            Category("مهن", listOf("طبيب", "معلم", "ممرض", "طاهي", "طيار")),
            Category("دول", listOf("مصر", "فرنسا", "اليابان", "البرازيل", "كندا")),
            Category("خضروات", listOf("جزر", "بطاطس", "بصل", "ثوم", "طماطم")),
            Category("حيوانات", listOf("أسد", "نمر", "حمار وحشي", "حصان", "جمل")),
            Category("طيور", listOf("نسر", "صقر", "ببغاء", "بومة", "بط")),
            Category("أشياء", listOf("كرسي", "طاولة", "نافذة", "باب", "ساعة")),
            Category("كواكب", listOf("المريخ", "الزهرة", "الأرض", "زحل", "المشتري")),
            Category("معادن", listOf("ذهب", "فضة", "حديد", "نحاس", "رصاص")),
            Category("طبيعة", listOf("نهر", "بحيرة", "بحر", "جبل", "تل")),
            Category("رياضات", listOf("تنس", "كرة قدم", "جولف", "رجبي", "جودو"))
        )
    }
}