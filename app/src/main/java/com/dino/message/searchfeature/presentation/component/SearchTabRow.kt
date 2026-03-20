package com.dino.message.searchfeature.presentation.component

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dino.message.searchfeature.domain.model.SearchTab

@Composable
fun SearchTabRow(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = SearchTab.entries

    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        modifier = modifier
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = when (tab) {
                            SearchTab.USERS -> "Users"
                            SearchTab.POSTS -> "Posts"
                            SearchTab.BOTH -> "Both"
                        }
                    )
                }
            )
        }
    }
}
