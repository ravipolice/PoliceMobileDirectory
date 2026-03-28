package com.example.policemobiledirectory.viewmodel

import com.example.policemobiledirectory.repository.ConstantsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * User-specific ConstantsViewModel.
 * Extends BaseConstantsViewModel and adds user-side logic if needed.
 */
@HiltViewModel
class ConstantsViewModel @Inject constructor(
    private val userRepo: ConstantsRepository
) : BaseConstantsViewModel(userRepo) {
    
    // Additional user-specific UI logic can go here (e.g. check local DB sync status)
}
