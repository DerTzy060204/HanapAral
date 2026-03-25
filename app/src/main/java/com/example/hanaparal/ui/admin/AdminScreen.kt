package com.example.hanaparal.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanaparal.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel = viewModel()
) {
    val appConfig by mainViewModel.appConfig.collectAsState()
    val context = LocalContext.current

    var header by remember { mutableStateOf(appConfig.globalAnnouncementHeader) }
    var maxMembers by remember { mutableStateOf(appConfig.maxMembersPerGroup.toString()) }
    var isCreationEnabled by remember { mutableStateOf(appConfig.isGroupCreationEnabled) }
    var isJoiningEnabled by remember { mutableStateOf(appConfig.isJoiningGroupsEnabled) }