package de.mineking.hexo.board.render.image.theme

enum class FontType {
    SansSerifBold {
        private val firstPrintableCharacterCode = 32
        private val unknownCharacterWidth = 1.05
        private val openSansExtraBoldCharacterWidths = floatArrayOf(
            0.259766f, 0.290039f, 0.523926f, 0.662109f, 0.585938f, 0.948242f, 0.798828f, 0.291992f,
            0.358887f, 0.358887f, 0.537109f, 0.565918f, 0.309082f, 0.317871f, 0.289063f, 0.441895f,
            0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f, 0.585938f,
            0.585938f, 0.585938f, 0.289063f, 0.296875f, 0.565918f, 0.565918f, 0.565918f, 0.504883f,
            0.896973f, 0.726074f, 0.673828f, 0.648926f, 0.733887f, 0.548828f, 0.539063f, 0.740234f,
            0.766113f, 0.348145f, 0.352051f, 0.687012f, 0.582031f, 0.966797f, 0.833984f, 0.796875f,
            0.631836f, 0.796875f, 0.676758f, 0.577148f, 0.590820f, 0.756836f, 0.693848f, 1.039063f,
            0.723145f, 0.664063f, 0.610840f, 0.324219f, 0.441895f, 0.324219f, 0.524902f, 0.500000f,
            0.598145f, 0.623047f, 0.643066f, 0.539063f, 0.643066f, 0.618164f, 0.413086f, 0.605957f,
            0.669922f, 0.325195f, 0.324219f, 0.659180f, 0.323242f, 1.000000f, 0.669922f, 0.637207f,
            0.643066f, 0.643066f, 0.469238f, 0.533203f, 0.459961f, 0.669922f, 0.610840f, 0.910156f,
            0.629883f, 0.609863f, 0.506836f, 0.433105f, 0.508789f, 0.433105f, 0.565918f,
        )

        override fun estimateTextWidth(text: String) = text.sumOf { character ->
            openSansExtraBoldCharacterWidths
                .getOrNull(character.code - firstPrintableCharacterCode)
                ?.toDouble()
                ?: unknownCharacterWidth
        }
    },
    MonospaceRegular {
        private val characterWidth = 0.6
        override fun estimateTextWidth(text: String) = text.length * characterWidth
    },
    ;

    abstract fun estimateTextWidth(text: String): Double
}
