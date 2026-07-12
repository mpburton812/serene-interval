package com.example.meditationparticles.domain.affirmations

enum class AffirmationListKind {
    Affirmations,
    KatiesLoveList,
    ;

    val displayTitle: String
        get() = when (this) {
            Affirmations -> "Affirmations"
            KatiesLoveList -> "Katie's Love List"
        }

    val shortNavLabel: String
        get() = when (this) {
            Affirmations -> "Affirmations"
            KatiesLoveList -> "Love List"
        }

    val experienceDescription: String
        get() = when (this) {
            Affirmations -> "Personal affirmations you can review, reorder, and revisit."
            KatiesLoveList -> "A loving list of what Katie cherishes about you — browse and review like affirmations."
        }

    val collectionSubtitle: String
        get() = when (this) {
            Affirmations -> "Your personal echoes of strength"
            KatiesLoveList -> "Reasons you are loved, just for being you"
        }

    val heroLabel: String
        get() = when (this) {
            Affirmations -> "CURRENT AFFIRMATION"
            KatiesLoveList -> "FROM KATIE'S LOVE LIST"
        }

    val reviewButtonLabel: String
        get() = when (this) {
            Affirmations -> "Affirmations Review"
            KatiesLoveList -> "Love List Review"
        }

    val itemNoun: String
        get() = when (this) {
            Affirmations -> "affirmation"
            KatiesLoveList -> "love note"
        }

    val emptyCollectionMessage: String
        get() = when (this) {
            Affirmations -> "No affirmations yet. Add your first one."
            KatiesLoveList -> "No love notes yet. Add your first one."
        }

    val heroEmptyMessage: String
        get() = when (this) {
            Affirmations -> "Add affirmations to begin your collection."
            KatiesLoveList -> "Add love notes to begin Katie's list."
        }

    val reminderRequestCode: Int
        get() = when (this) {
            Affirmations -> 42_002
            KatiesLoveList -> 42_003
        }

    val notificationId: Int
        get() = when (this) {
            Affirmations -> 9_002
            KatiesLoveList -> 9_003
        }

    val preferencesName: String
        get() = when (this) {
            Affirmations -> "affirmation_preferences"
            KatiesLoveList -> "katie_love_list_preferences"
        }

    val defaultTexts: List<String>
        get() = when (this) {
            Affirmations -> DefaultAffirmationTexts
            KatiesLoveList -> DefaultKatiesLoveListTexts
        }

    companion object {
        fun fromStored(raw: String?): AffirmationListKind =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Affirmations
    }
}

private val DefaultAffirmationTexts = listOf(
    "I am worthy of peace, and I allow myself to breathe deeply through every moment.",
    "My anxiety does not define my future or my value as a human being.",
    "I am releasing the need to control the uncontrollable.",
    "This feeling is temporary. I have survived 100% of my bad days.",
    "I choose to be kind to myself in this moment of struggle.",
)

private val DefaultKatiesLoveListTexts = listOf(
    "Your inquisitiveness and creativity - the way you are always looking at the world in new and different ways and are always interested in learning new things",
    "Your interest in learning and growing, and your perennial fascination with new projects as well as enduring interests in old hobbies",
    "Your kind heart and your desire to help and protect other people",
    "Your shoulders and your eyes and your butt :3",
    "Your ability to tell stories and captivate an audience",
    "Your keen mind and sharp wit",
    "How dedicated you are to improving yourself and changing patterns you don't like, even when it's really hard and scary and painful",
    "Your bravery in embracing who you are and moving through the world authentically and with joy",
    "Your willingness to be vulnerable",
    "How much you care about the people you love and what a wonderful parent you were/are to Verl",
    "How fun you are to be around and how we always have a good time traveling or just bumming around at home",
    "Your delightfully evolving quirky fashion sense",
    "Your sense of humor that sometimes makes me LOL so hard I feel like I need to cover my mouth, especially when horses are terrible people",
    "How you fit in with my family and how my siblings and Evee and parents love you",
    "How you fit in with my friends and the cool kids and karaoke people always ask about you",
    "What a great party host you are",
    "How you put up with all my idiosyncrasies and neuroses with humor and grace and just the right amount of calling me on my ridiculousness without ever belittling me or making me feel embarrassed",
    "How snuggling up to you will knock me out in minutes",
    "Michael smells",
    "Your way with words",
    "How we are an unstoppable Frankenfurter sexy power couple",
    "People like you just for you.",
    "People at karaoke are happy to see you and enjoy seeing you sing and enjoy yourself, and you're not doing anything for them aside from just being yourself.",
    "Annie and Eren adore you and are on your side and want only good things for you, just because they know what a special person you are.",
    "Shannon and Terrick see how special you are and they wanted you to share your light with their families on a very special day to them, just by being yourself.",
    "Bailey cares about you and wants to protect you, not because you are doing anything for him or being useful, but just because he sees and likes you as a person and wants you to be safe and happy.",
    "JIB, Darran, Darren, and Rick have all known you for a very long time and trust you with their most personal thoughts, because they value your counsel and know that you will listen with an open heart.",
    "And I love you for you too.  You are a beautiful soul, radiant, vibrant, always growing and changing.  You motivate me to keep growing and changing too.  You keep me grounded when I'm ranting about hot air balloons and banks (and more serious stuff too).  You are my most faithful and trusted companion, and I'm so excited and grateful to get to step into this new, much-wanted, much-dreamed-about and hoped-for chapter of our lives together.",
)
