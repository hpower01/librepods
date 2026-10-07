/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R

// גופן כתב-יד מצורף לאפליקציה (Dancing Script), כדי שגם הקרדיט וגם ה-© ייראו זהים בכל מכשיר
// The system "cursive" font on some devices has no © glyph, so it fell back to a plain one
val CreditFontFamily = FontFamily(Font(R.font.dancing_script))

// Same as the credit in the HPower apps: cursive, italic, 22sp
val CreditTextStyle = TextStyle(fontSize = 22.sp, fontFamily = CreditFontFamily, fontStyle = FontStyle.Italic)

// סימן זכויות יוצרים באותו גופן מסולסל של הקרדיט
@Composable
fun CopyrightMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = "©",
        style = CreditTextStyle,
        color = color,
        // חותך את מה שהנטייה דוחפת מחוץ לגבולות, כמו TextView ב-Smart Gallery
        // A TextView clips the synthetic-italic overhang, cutting the ring's right edge; Compose doesn't by default
        modifier = modifier.clipToBounds()
    )
}
