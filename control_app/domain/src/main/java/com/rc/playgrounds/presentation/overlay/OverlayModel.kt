package com.rc.playgrounds.presentation.overlay

import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import com.rc.playgrounds.presentation.announce.AnnounceModel
import com.rc.playgrounds.presentation.announce.AnnounceViewModel
import com.rc.playgrounds.presentation.lock.LockModel
import com.rc.playgrounds.presentation.lock.LockViewModel
import com.rc.playgrounds.presentation.quickconfig.QuickConfigModel
import com.rc.playgrounds.presentation.quickconfig.QuickConfigViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OverlayModel(
    activeScreenProvider: ActiveScreenProvider,
    lockModel: LockModel,
    announceModel: AnnounceModel,
    quickConfigModel: QuickConfigModel,
) {
    val state: Flow<OverlayViewModel?> = combine(
        activeScreenProvider.screen,
        announceModel.viewModel,
        lockModel.viewModel,
        quickConfigModel.viewModel
    ) { screen, announce, lock, quickConfig ->
        when (screen) {
            Screen.MAIN -> Unit
            Screen.CONFIG -> return@combine null
        }
        when (announce) {
            AnnounceViewModel.Hidden -> Unit
            is AnnounceViewModel.Visible -> return@combine OverlayViewModel.ANNOUNCE
        }

        when (quickConfig) {
            QuickConfigViewModel.Hidden -> Unit
            is QuickConfigViewModel.DashboardVisible, is QuickConfigViewModel.Visible -> return@combine OverlayViewModel.QUICK_CONFIG
        }

        when (lock) {
            LockViewModel.Hidden -> null
            LockViewModel.Visible -> OverlayViewModel.LOCK_SCREEN
        }
    }
}