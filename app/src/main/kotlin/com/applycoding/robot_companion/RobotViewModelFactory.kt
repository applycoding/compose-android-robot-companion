// SPDX-License-Identifier: Apache-2.0

package com.applycoding.robot_companion

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

class RobotViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val transportFactory: TransportFactory = DefaultTransportFactory,
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        if (!modelClass.isAssignableFrom(RobotViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
        }
        @Suppress("UNCHECKED_CAST")
        return RobotViewModel(handle, transportFactory) as T
    }
}
