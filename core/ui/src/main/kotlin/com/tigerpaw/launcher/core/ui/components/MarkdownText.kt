package com.tigerpaw.launcher.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Block model
// ---------------------------------------------------------------------------

private sealed class MdBlock {
    data class Paragraph(val text: String) : MdBlock()
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class CodeBlock(val code: String) : MdBlock()
    data class BulletItem(val text: String) : MdBlock()
    data class NumberedItem(val number: Int, val text: String) : MdBlock()
    data object HorizontalRule : MdBlock()
}

// ---------------------------------------------------------------------------
// Parser
// ---------------------------------------------------------------------------

private fun parseMarkdown(raw: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = raw.split('\n')
    var i = 0
    val para = StringBuilder()

    fun flushParagraph() {
        val p = para.toString().trim()
        if (p.isNotEmpty()) blocks.add(MdBlock.Paragraph(p))
        para.clear()
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Fenced code block
        if (trimmed.startsWith("```")) {
            flushParagraph()
            i++
            val code = StringBuilder()
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (code.isNotEmpty()) code.append('\n')
                code.append(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MdBlock.CodeBlock(code.toString()))
            continue
        }

        // ATX heading: # / ## / ###
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            blocks.add(
                MdBlock.Heading(
                    level = headingMatch.groupValues[1].length,
                    text = headingMatch.groupValues[2],
                )
            )
            i++; continue
        }

        // Horizontal rule
        if (Regex("^(---+|___+|\\*\\*\\*+)$").matches(trimmed)) {
            flushParagraph()
            blocks.add(MdBlock.HorizontalRule)
            i++; continue
        }

        // Bullet list item
        val bulletMatch = Regex("^\\s*[-*+]\\s+(.+)$").matchEntire(line)
        if (bulletMatch != null) {
            flushParagraph()
            blocks.add(MdBlock.BulletItem(bulletMatch.groupValues[1].trim()))
            i++; continue
        }

        // Numbered list item
        val numberedMatch = Regex("^\\s*(\\d+)\\.\\s+(.+)$").matchEntire(line)
        if (numberedMatch != null) {
            flushParagraph()
            blocks.add(
                MdBlock.NumberedItem(
                    number = numberedMatch.groupValues[1].toIntOrNull() ?: 1,
                    text = numberedMatch.groupValues[2].trim(),
                )
            )
            i++; continue
        }

        // Blank line → paragraph break
        if (trimmed.isEmpty()) {
            flushParagraph()
            i++; continue
        }

        // Continuation of paragraph
        if (para.isNotEmpty()) para.append(' ')
        para.append(trimmed)
        i++
    }

    flushParagraph()
    return blocks
}

// ---------------------------------------------------------------------------
// Inline renderer
// ---------------------------------------------------------------------------

private fun renderInline(text: String, codeSpanBackground: Color): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold-italic ***text***
                text.startsWith("***", i) -> {
                    val end = text.indexOf("***", i + 3)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 3, end))
                        }
                        i = end + 3
                    } else { append(text[i]); i++ }
                }

                // Bold **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }

                // Italic *text*  (only when surrounded by non-space)
                text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && text[end - 1] != ' ') {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }

                // Inline code `text`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                background = codeSpanBackground,
                            )
                        ) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }

                else -> { append(text[i]); i++ }
            }
        }
    }

// ---------------------------------------------------------------------------
// Public composable
// ---------------------------------------------------------------------------

/**
 * Renders a Markdown string with support for:
 *  - Headings (# / ## / ###)
 *  - **Bold**, *Italic*, ***Bold-Italic***
 *  - `Inline code`
 *  - ``` Fenced code blocks ```
 *  - Bullet lists (- / * / +)
 *  - Numbered lists
 *  - Horizontal rules (---)
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val codeBlockBg = Color.Black.copy(alpha = 0.18f)
    val codeSpanBg = Color.Black.copy(alpha = 0.12f)
    val blocks = remember(text) { parseMarkdown(text) }

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            // Small gap between blocks; tighter between list items
            if (index > 0) {
                val prevIsList = blocks[index - 1] is MdBlock.BulletItem ||
                    blocks[index - 1] is MdBlock.NumberedItem
                val currIsList = block is MdBlock.BulletItem || block is MdBlock.NumberedItem
                Spacer(Modifier.height(if (prevIsList && currIsList) 2.dp else 6.dp))
            }

            when (block) {
                is MdBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = renderInline(block.text, codeSpanBg),
                        style = headingStyle,
                        color = color,
                    )
                }

                is MdBlock.Paragraph -> {
                    Text(
                        text = renderInline(block.text, codeSpanBg),
                        style = style,
                        color = color,
                    )
                }

                is MdBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(codeBlockBg, RoundedCornerShape(8.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = block.code,
                            style = style.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            color = color,
                        )
                    }
                }

                is MdBlock.BulletItem -> {
                    Row {
                        Text("• ", style = style, color = color)
                        Text(
                            text = renderInline(block.text, codeSpanBg),
                            style = style,
                            color = color,
                        )
                    }
                }

                is MdBlock.NumberedItem -> {
                    Row {
                        Text("${block.number}. ", style = style, color = color)
                        Text(
                            text = renderInline(block.text, codeSpanBg),
                            style = style,
                            color = color,
                        )
                    }
                }

                is MdBlock.HorizontalRule -> {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(color.copy(alpha = 0.25f)),
                    )
                }
            }
        }
    }
}
