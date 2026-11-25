package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.docs.DocumentationContent
import com.example.docs.GuideSection
import com.example.ui.studio.StudioTab

@Composable
fun GuideDocsScreen(
  onNavigateTab: (StudioTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCategory by remember { mutableStateOf("すべて") }
  var searchQuery by remember { mutableStateOf("") }
  var expandedIds by remember {
    mutableStateOf(setOf("video_extension_bridge", "quickstart_sync"))
  }

  val filteredSections = remember(selectedCategory, searchQuery) {
    DocumentationContent.sections.filter { sec ->
      val catMatch = selectedCategory == "すべて" || sec.category == selectedCategory
      val q = searchQuery.trim()
      val queryMatch = q.isEmpty() ||
        sec.title.contains(q, ignoreCase = true) ||
        sec.summary.contains(q, ignoreCase = true) ||
        sec.steps.any { it.contains(q, ignoreCase = true) } ||
        sec.technicalNote.contains(q, ignoreCase = true)
      catMatch && queryMatch
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.MenuBook,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Column {
          Text("ドキュメント & 利用ガイド", style = MaterialTheme.typography.headlineSmall)
          Text(
            "全機能の使い方・既存ベースへの動画壁紙拡張・Wear OS同期仕様",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = { Text("キーワード検索（例: 動画、LTPO、Data Layer、拡張）") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("guide_search_input"),
      )
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DocumentationContent.categories) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
          )
        }
      }
    }

    if (filteredSections.isEmpty()) {
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "該当するドキュメントが見つかりませんでした。検索条件を変更してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
          )
        }
      }
    } else {
      items(filteredSections, key = { it.id }) { section ->
        val isExpanded = section.id in expandedIds
        GuideSectionCard(
          section = section,
          expanded = isExpanded,
          onToggle = {
            expandedIds = if (isExpanded) expandedIds - section.id else expandedIds + section.id
          },
          onJump = { section.targetTab?.let(onNavigateTab) },
        )
      }
    }
  }
}

@Composable
private fun GuideSectionCard(
  section: GuideSection,
  expanded: Boolean,
  onToggle: () -> Unit,
  onJump: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onToggle)
      .testTag("guide_card_${section.id}"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(
      width = 1.dp,
      color = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
    ),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
          Text(
            text = section.category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = section.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = if (expanded) "折りたたむ" else "展開する",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(
          modifier = Modifier.padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          section.steps.forEach { step ->
            Text(
              text = step,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "技術メモ: ${section.technicalNote}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(10.dp),
            )
          }

          if (section.targetTab != null && section.actionLabel != null) {
            OutlinedButton(
              onClick = onJump,
              modifier = Modifier.testTag("guide_jump_${section.id}"),
            ) {
              Text(section.actionLabel)
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }
    }
  }
}
