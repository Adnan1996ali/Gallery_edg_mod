package alpha.ai.chat.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import alpha.ai.chat.GalleryTopAppBar
import alpha.ai.chat.R
import alpha.ai.chat.data.AppBarAction
import alpha.ai.chat.data.AppBarActionType
import alpha.ai.chat.ui.common.TaskIcon
import alpha.ai.chat.ui.common.tos.AppTosDialog
import alpha.ai.chat.ui.common.tos.TosViewModel
import alpha.ai.chat.ui.modelmanager.ModelManagerViewModel
import alpha.ai.chat.ui.theme.customColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  onChatClicked: () -> Unit,
  onModelsClicked: () -> Unit,
  onNotificationsClicked: () -> Unit,
  enableAnimation: Boolean,
  modifier: Modifier = Modifier,
  gm4: Boolean = false,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  if (!showTosDialog) {
    var loadingModelAllowlistDelayed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.loadingModelAllowlist) {
      if (uiState.loadingModelAllowlist) {
        delay(200)
        if (uiState.loadingModelAllowlist) {
          loadingModelAllowlistDelayed = true
        }
      } else {
        loadingModelAllowlistDelayed = false
      }
    }

    if (!loadingModelAllowlistDelayed && !uiState.loadingModelAllowlist) {
      val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

      val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

      LaunchedEffect(Unit) {
        delay(2000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
              PackageManager.PERMISSION_GRANTED
          ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(modifier = Modifier.fillMaxWidth()) {
                SquareDrawerItem(
                  label = stringResource(R.string.drawer_settings_label),
                  description = stringResource(R.string.drawer_settings_description),
                  icon = Icons.Rounded.Settings,
                  onClick = {
                    showSettingsDialog = true
                    scope.launch { drawerState.close() }
                  },
                  modifier = Modifier.weight(1f),
                  iconBrush = Brush.linearGradient(
                    colors = listOf(
                      MaterialTheme.customColors.taskBgGradientColors[2][0],
                      MaterialTheme.customColors.taskBgGradientColors[2][1],
                    )
                  ),
                )
                Spacer(modifier = Modifier.width(16.dp))
                SquareDrawerItem(
                  label = stringResource(R.string.drawer_models_label),
                  description = stringResource(R.string.drawer_models_description),
                  icon = Icons.AutoMirrored.Rounded.ListAlt,
                  onClick = {
                    scope.launch { drawerState.close() }
                    scope.launch {
                      delay(50)
                      onModelsClicked()
                    }
                  },
                  modifier = Modifier.weight(1f),
                  iconBrush = Brush.linearGradient(
                    colors = listOf(
                      MaterialTheme.customColors.taskBgGradientColors[1][0],
                      MaterialTheme.customColors.taskBgGradientColors[1][1],
                    )
                  ),
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
              Row(modifier = Modifier.fillMaxWidth()) {
                SquareDrawerItem(
                  label = stringResource(R.string.drawer_notifications_label),
                  description = stringResource(R.string.drawer_notifications_description),
                  icon = Icons.Rounded.Notifications,
                  onClick = {
                    scope.launch { drawerState.close() }
                    scope.launch {
                      delay(50)
                      onNotificationsClicked()
                    }
                  },
                  modifier = Modifier.weight(1f),
                  iconBrush = Brush.linearGradient(
                    colors = listOf(
                      MaterialTheme.customColors.taskBgGradientColors[0][0],
                      MaterialTheme.customColors.taskBgGradientColors[0][1],
                    )
                  ),
                )
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        },
        gesturesEnabled = drawerState.isOpen,
      ) {
        Scaffold(
          containerColor = MaterialTheme.colorScheme.background,
          topBar = {
            GalleryTopAppBar(
              title = stringResource(R.string.app_name),
              leftAction = AppBarAction(
                actionType = AppBarActionType.MENU,
                actionFn = {
                  scope.launch { drawerState.apply { if (isClosed) open() else close() } }
                },
              ),
            )
          },
        ) { innerPadding ->
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.surface)
              .padding(top = innerPadding.calculateTopPadding()),
          ) {
            // Main content: Chat entry button
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.padding(32.dp),
            ) {
              Text(
                text = "AlphaAi",
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 48.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Your On-Device AI Assistant",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(48.dp))

              Card(
                onClick = onChatClicked,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
              ) {
                Row(
                  modifier = Modifier.padding(24.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ListAlt,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Spacer(modifier = Modifier.width(16.dp))
                  Column {
                    Text(
                      text = "AI Chat",
                      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                      text = "Chat, analyze images, transcribe audio & more",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showTosDialog) {
    AppTosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      }
    )
  }

  if (showSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showSettingsDialog = false },
    )
  }
}
