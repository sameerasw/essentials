package com.sameerasw.essentials.ui.components.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun SimpleMarkdown(
    content: String,
    modifier: Modifier = Modifier
) {
    val lines = content.lines()
    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                trimmedLine.startsWith("![") && trimmedLine.contains("](") -> {
                    MarkdownImage(trimmedLine)
                }
                trimmedLine.contains("<img") && trimmedLine.contains("src=") -> {
                    HtmlImage(trimmedLine)
                }
                trimmedLine.startsWith("###") -> {
                    HeaderLine(trimmedLine.substringAfter("###").trim(), MaterialTheme.typography.titleMedium)
                }
                trimmedLine.startsWith("##") -> {
                    HeaderLine(trimmedLine.substringAfter("##").trim(), MaterialTheme.typography.titleLarge)
                }
                trimmedLine.startsWith("#") -> {
                    HeaderLine(trimmedLine.substringAfter("#").trim(), MaterialTheme.typography.headlineSmall)
                }
                trimmedLine.startsWith("-") || (trimmedLine.startsWith("*") && trimmedLine.getOrNull(1) == ' ') -> {
                    BulletPointLine(trimmedLine.substring(1).trim())
                }
                else -> {
                    MarkdownText(line)
                }
            }
        }
    }
}

@Composable
private fun HeaderLine(text: String, style: TextStyle) {
    Text(
        text = parseMarkdown(text),
        style = style,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun BulletPointLine(content: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        MarkdownText(content)
    }
}

@Composable
private fun MarkdownImage(line: String) {
    val url = line.substringAfter("](").substringBefore(")")
    val alt = line.substringAfter("![").substringBefore("]")
    
    RenderImage(url, alt)
}

@Composable
private fun HtmlImage(line: String) {
    val srcRegex = Regex("src=\"(.*?)\"")
    val altRegex = Regex("alt=\"(.*?)\"")
    val src = srcRegex.find(line)?.groupValues?.get(1)
    val alt = altRegex.find(line)?.groupValues?.get(1) ?: "image"
    
    if (src != null) {
        RenderImage(src, alt)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RenderImage(url: String, alt: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = false) // Default aspect ratio while loading
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = alt,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun MarkdownText(text: String) {
    val cleanText = text.replace(Regex("<img.*?>"), "").trim()
    if (cleanText.isNotBlank()) {
        Text(
            text = parseMarkdown(cleanText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        
        // Regex for Bold (**text**), Italic (*text* or _text_), and Links ([text](url))
        val regex = Regex("(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(_.*?_)|(\\[.*?]\\(.*?\\))")
        val matches = regex.findAll(text)
        
        matches.forEach { match ->
            val matchValue = match.value
            val start = match.range.first
            
            // Text before match
            if (start > cursor) {
                append(text.substring(cursor, start))
            }
            
            when {
                matchValue.startsWith("**") && matchValue.endsWith("**") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchValue.substring(2, matchValue.length - 2))
                    }
                }
                (matchValue.startsWith("*") && matchValue.endsWith("*") && !matchValue.startsWith("**")) || (matchValue.startsWith("_") && matchValue.endsWith("_")) -> {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(matchValue.substring(1, matchValue.length - 1))
                    }
                }
                matchValue.startsWith("[") && matchValue.contains("](") -> {
                    val title = matchValue.substringAfter("[").substringBefore("](")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                        append(title)
                    }
                }
                else -> append(matchValue)
            }
            
            cursor = match.range.last + 1
        }
        
        // Remaining text
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
