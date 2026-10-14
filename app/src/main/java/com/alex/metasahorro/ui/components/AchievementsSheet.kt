package com.alex.metasahorro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.metasahorro.R
import com.alex.metasahorro.data.AchievementUnlock
import com.alex.metasahorro.repository.Achievement
import com.alex.metasahorro.ui.theme.GoldColor
import com.alex.metasahorro.ui.theme.SurfaceAltColor
import com.alex.metasahorro.ui.theme.TextMutedColor
import com.alex.metasahorro.util.fmtDateShort

/** Vitrina de logros: los desbloqueados primero (con su fecha) y luego los que faltan, atenuados y con candado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsSheet(unlocked: List<AchievementUnlock>, onDismiss: () -> Unit) {
    val unlockedAt = unlocked.associate { it.id to it.unlockedAtMillis }
    val all = Achievement.entries
    val ordered = all.sortedWith(compareBy({ unlockedAt[it.name] == null }, { -(unlockedAt[it.name] ?: 0L) }))
    val count = all.count { unlockedAt[it.name] != null }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.achievements_title), fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                pluralText(R.plurals.achievements_progress, count, count, all.size),
                fontSize = 12.sp, color = TextMutedColor, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            ordered.forEach { achievement -> AchievementRow(achievement, unlockedAt[achievement.name]) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_close)) }
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlockedAtMillis: Long?) {
    val locked = unlockedAtMillis == null
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).alpha(if (locked) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(SurfaceAltColor),
            contentAlignment = Alignment.Center
        ) { Text(achievement.emoji, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(achievement.title), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(stringResource(achievement.description), fontSize = 11.5.sp, color = TextMutedColor)
        }
        if (unlockedAtMillis != null) {
            Text(fmtDateShort(unlockedAtMillis), fontSize = 11.sp, color = GoldColor, fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.cd_locked), tint = TextMutedColor, modifier = Modifier.size(18.dp))
        }
    }
}

/** Aviso de celebración cuando se desbloquean uno o más logros a la vez. */
@Composable
fun AchievementUnlockedDialog(list: List<Achievement>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralText(R.plurals.achievement_unlocked_title, list.size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.emoji, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(a.title), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(a.description), fontSize = 12.sp, color = TextMutedColor)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.achievement_unlocked_ok)) } }
    )
}
