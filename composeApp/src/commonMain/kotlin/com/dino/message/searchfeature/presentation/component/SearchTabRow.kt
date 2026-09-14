package com.dino.message.searchfeature.presentation.component

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.both
import com.dino.message.composeapp.generated.resources.posts
import com.dino.message.composeapp.generated.resources.users
import com.dino.message.searchfeature.domain.model.SearchTab
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchTabRow(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = SearchTab.entries
    TabRow(selectedTabIndex = tabs.indexOf(selectedTab), modifier = modifier) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        when (tab) {
                            SearchTab.USERS -> stringResource(Res.string.users)
                            SearchTab.POSTS -> stringResource(Res.string.posts)
                            SearchTab.BOTH -> stringResource(Res.string.both)
                        },
                    )
                },
            )
        }
    }
}
