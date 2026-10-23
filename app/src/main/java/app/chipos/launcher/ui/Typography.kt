package app.chipos.launcher.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.chipos.launcher.R

/**
 * Inter (SIL Open Font License), bundled in res/font. It replaces Android's default Roboto, which is
 * a big part of why stock Android TV UIs look like Android.
 */
val ChipFont = FontFamily(
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
