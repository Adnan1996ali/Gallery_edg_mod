package alpha.ai.chat.ui.llmchat

import androidx.hilt.navigation.compose.hiltViewModel

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import alpha.ai.chat.GalleryEvent
import alpha.ai.chat.R
import alpha.ai.chat.data.BuiltInTaskId
import alpha.ai.chat.data.Model
import alpha.ai.chat.data.ModelCapability
import alpha.ai.chat.data.RuntimeType
import alpha.ai.chat.data.Task
import alpha.ai.chat.firebaseAnalytics
import alpha.ai.chat.ui.common.chat.ChatMessage
import alpha.ai.chat.ui.common.chat.ChatMessageAudioClip
import alpha.ai.chat.ui.common.chat.ChatMessageImage
import alpha.ai.chat.ui.common.chat.ChatMessageText
import alpha.ai.chat.ui.common.chat.ChatSide
import alpha.ai.chat.ui.common.chat.ChatView
import alpha.ai.chat.ui.common.chat.SendMessageTrigger
import alpha.ai.chat.ui.modelmanager.ModelManagerViewModel
import alpha.ai.chat.ui.theme.emptyStateContent
import alpha.ai.chat.ui.theme.emptyStateTitle
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message

private const val TAG = "AGLlmChatScreen"

enum class ToolType(
  val id: String,
  val label: String,
  val icon: ImageVector,
  val description: String,
) {
  AI_CHAT(BuiltInTaskId.LLM_CHAT, "AI Chat", Icons.Rounded.Forum, "Chat with on-device LLMs"),
  PROMPT_LAB(BuiltInTaskId.LLM_PROMPT_LAB, "Prompt Lab", Icons.Filled.Psychology, "Single-turn prompt workspace"),
  ASK_IMAGE(BuiltInTaskId.LLM_ASK_IMAGE, "Ask Image", Icons.Rounded.Image, "Ask about images"),
  AUDIO_SCRIBE(BuiltInTaskId.LLM_ASK_AUDIO, "Audio Scribe", Icons.Filled.Mic, "Transcribe audio"),
  AGENT_SKILLS(BuiltInTaskId.LLM_AGENT_CHAT, "Agent Skills", Icons.Filled.Build, "AI with tool calling"),
  MOBILE_ACTIONS(BuiltInTaskId.LLM_MOBILE_ACTIONS, "Mobile Actions", Icons.Filled.PhoneAndroid, "Control your device"),
  TINY_GARDEN(BuiltInTaskId.LLM_TINY_GARDEN, "Tiny Garden", Icons.Filled.Games, "Gardening mini-game"),
}

@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  taskId: String = BuiltInTaskId.LLM_CHAT,
  onFirstToken: (Model) -> Unit = {},
  onGenerateResponseDone: (Model) -> Unit = {},
  onSkillClicked: () -> Unit = {},
  onMcpClicked: () -> Unit = {},
  onResetSessionClickedOverride: ((Task, Model, List<ChatMessage>, Boolean, () -> Unit) -> Unit)? = null,
  composableBelowMessageList: @Composable (Model) -> Unit = {},
  viewModel: LlmChatViewModel = hiltViewModel(),
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
  emptyStateComposable: @Composable (Model) -> Unit = {},
  sendMessageTrigger: SendMessageTrigger? = null,
  showImagePicker: Boolean = false,
  showAudioPicker: Boolean = false,
  getActiveSkills: () -> List<String> = { emptyList() },
  skillCount: Int = 0,
  mcpCount: Int = 0,
  mcpToolsCount: Int = 0,
) {
  var selectedTool by remember { mutableStateOf(ToolType.AI_CHAT) }
  var toolsExpanded by remember { mutableStateOf(false) }

  val toolsBelowComposable: @Composable (Model) -> Unit = { model ->
    Column {
      // Tools panel (collapsible)
      ToolsPanel(
        selectedTool = selectedTool,
        expanded = toolsExpanded,
        onToggle = { toolsExpanded = !toolsExpanded },
        onToolSelected = { tool ->
          selectedTool = tool
          toolsExpanded = false
          // Update the chat's task and model based on selected tool
          val newTask = modelManagerViewModel.getTaskById(id = tool.id)
          if (newTask != null) {
            val defaultModel = newTask.models.firstOrNull()
            if (defaultModel != null) {
              modelManagerViewModel.selectModel(defaultModel)
            }
          }
        },
      )
      // Also include custom content below the tools
      composableBelowMessageList(model)
    }
  }

  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = if (selectedTool.id == BuiltInTaskId.LLM_CHAT) taskId else selectedTool.id,
    navigateUp = navigateUp,
    modifier = modifier,
    onSkillClicked = onSkillClicked,
    onMcpClicked = onMcpClicked,
    onFirstToken = onFirstToken,
    onGenerateResponseDone = onGenerateResponseDone,
    onResetSessionClickedOverride = onResetSessionClickedOverride,
    composableBelowMessageList = toolsBelowComposable,
    allowEditingSystemPrompt = allowEditingSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    skillCount = skillCount,
    mcpCount = mcpCount,
    mcpToolsCount = mcpToolsCount,
    onSystemPromptChanged = onSystemPromptChanged,
    emptyStateComposable = { model ->
      if (selectedTool == ToolType.ASK_IMAGE) {
        AskImageEmptyState(model)
      } else if (selectedTool == ToolType.AUDIO_SCRIBE) {
        AskAudioEmptyState()
      } else {
        emptyStateComposable(model)
      }
    },
    sendMessageTrigger = sendMessageTrigger,
    showImagePicker = showImagePicker || selectedTool == ToolType.ASK_IMAGE,
    showAudioPicker = showAudioPicker || selectedTool == ToolType.AUDIO_SCRIBE,
    getActiveSkills = getActiveSkills,
  )
}

@Composable
private fun ToolsPanel(
  selectedTool: ToolType,
  expanded: Boolean,
  onToggle: () -> Unit,
  onToolSelected: (ToolType) -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 4.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Column {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onToggle)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "الأدوات",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = if (expanded) "Collapse" else "Expand",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Active tool indicator
      if (!expanded) {
        Row(
          modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = selectedTool.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = selectedTool.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Tool list (collapsible)
      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          ToolType.entries.forEach { tool ->
            val isSelected = tool == selectedTool
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface
                )
                .clickable { onToolSelected(tool) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = tool.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = tool.label,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                          else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = tool.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun AskImageEmptyState(model: Model) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp).padding(bottom = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(stringResource(R.string.askimage_emptystate_title), style = emptyStateTitle)
      val contentRes = if (model.runtimeType == RuntimeType.AICORE) {
        R.string.askimage_emptystate_content_aicore
      } else {
        R.string.askimage_emptystate_content
      }
      Text(
        stringResource(contentRes),
        style = emptyStateContent,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun AskAudioEmptyState() {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp).padding(bottom = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(stringResource(R.string.askaudio_emptystate_title), style = emptyStateTitle)
      Text(
        stringResource(R.string.askaudio_emptystate_content),
        style = emptyStateContent,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun LlmAskImageScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskImageViewModel = hiltViewModel(),
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_ASK_IMAGE,
    navigateUp = navigateUp,
    modifier = modifier,
    allowEditingSystemPrompt = allowEditingSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    onSystemPromptChanged = onSystemPromptChanged,
    showImagePicker = true,
    showAudioPicker = false,
    emptyStateComposable = { model ->
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp).padding(bottom = 48.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(stringResource(R.string.askimage_emptystate_title), style = emptyStateTitle)
          val contentRes = if (model.runtimeType == RuntimeType.AICORE) {
            R.string.askimage_emptystate_content_aicore
          } else {
            R.string.askimage_emptystate_content
          }
          Text(
            stringResource(contentRes),
            style = emptyStateContent,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    },
  )
}

@Composable
fun LlmAskAudioScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskAudioViewModel = hiltViewModel(),
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    taskId = BuiltInTaskId.LLM_ASK_AUDIO,
    navigateUp = navigateUp,
    modifier = modifier,
    allowEditingSystemPrompt = allowEditingSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    onSystemPromptChanged = onSystemPromptChanged,
    showImagePicker = false,
    showAudioPicker = true,
    emptyStateComposable = {
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp).padding(bottom = 48.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(stringResource(R.string.askaudio_emptystate_title), style = emptyStateTitle)
          Text(
            stringResource(R.string.askaudio_emptystate_content),
            style = emptyStateContent,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    },
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: LlmChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  taskId: String,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  onSkillClicked: () -> Unit = {},
  onMcpClicked: () -> Unit = {},
  onFirstToken: (Model) -> Unit = {},
  onGenerateResponseDone: (Model) -> Unit = {},
  onResetSessionClickedOverride: ((Task, Model, List<ChatMessage>, Boolean, () -> Unit) -> Unit)? = null,
  composableBelowMessageList: @Composable (Model) -> Unit = {},
  emptyStateComposable: @Composable (Model) -> Unit = {},
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  onSystemPromptChanged: (String) -> Unit = {},
  sendMessageTrigger: SendMessageTrigger? = null,
  showImagePicker: Boolean = false,
  showAudioPicker: Boolean = false,
  getActiveSkills: () -> List<String> = { emptyList() },
  skillCount: Int = 0,
  mcpCount: Int = 0,
  mcpToolsCount: Int = 0,
) {
  val context = LocalContext.current
  val task = modelManagerViewModel.getTaskById(id = taskId)!!
  val scope = rememberCoroutineScope()

  ChatView(
    task = task,
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    onSendMessage = { model, messages ->
      for (message in messages) {
        viewModel.addMessage(model = model, message = message)
      }

      var text = ""
      val images: MutableList<Bitmap> = mutableListOf()
      val audioMessages: MutableList<ChatMessageAudioClip> = mutableListOf()
      var chatMessageText: ChatMessageText? = null
      for (message in messages) {
        if (message is ChatMessageText) {
          chatMessageText = message
          text = message.content
        } else if (message is ChatMessageImage) {
          images.addAll(message.bitmaps)
        } else if (message is ChatMessageAudioClip) {
          audioMessages.add(message)
        }
      }
      if ((text.isNotEmpty() && chatMessageText != null) || audioMessages.isNotEmpty()) {
        if (text.isNotEmpty()) {
          modelManagerViewModel.addTextInputHistory(text)
        }
        viewModel.generateResponse(
          model = model,
          input = text,
          images = images,
          audioMessages = audioMessages,
          onFirstToken = onFirstToken,
          onDone = { onGenerateResponseDone(model) },
          onError = { errorMessage ->
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              errorMessage = errorMessage,
              modelManagerViewModel = modelManagerViewModel,
            )
          },
          allowThinking = task.allowCapability(ModelCapability.LLM_THINKING, model),
        )

        val activeSkills = getActiveSkills()
        Log.d(TAG, "Analytics: generate_action, capability_name=${task.id}")
        firebaseAnalytics?.logEvent(
          GalleryEvent.GENERATE_ACTION.id,
          Bundle().apply {
            putString("capability_name", task.id)
            putString("model_id", model.name)
            putBoolean("has_image", images.isNotEmpty())
            putInt("image_count", images.size)
            putBoolean("has_audio", audioMessages.isNotEmpty())
            putInt("audio_count", audioMessages.size)
            putInt("active_skills_count", activeSkills.size)
            putString("active_skills_list", activeSkills.joinToString(","))
            putInt("active_mcp_servers_count", mcpCount)
            putInt("active_mcp_tools_count", mcpToolsCount)
          },
        )
      }
    },
    onRunAgainClicked = { model, message ->
      if (message is ChatMessageText) {
        viewModel.runAgain(
          model = model,
          message = message,
          onError = { errorMessage ->
            viewModel.handleError(
              context = context,
              task = task,
              model = model,
              errorMessage = errorMessage,
              modelManagerViewModel = modelManagerViewModel,
            )
          },
          allowThinking = task.allowCapability(ModelCapability.LLM_THINKING, model),
        )
      }
    },
    onBenchmarkClicked = { _, _, _, _ -> },
    onResetSessionClicked = { model, chatMessages, clearHistory, onDone ->
      val litertMessages = chatMessages.mapNotNull { convertToLitertMessage(it) }
      if (onResetSessionClickedOverride != null) {
        onResetSessionClickedOverride(task, model, chatMessages, clearHistory, onDone)
      } else {
        viewModel.resetSession(
          task = task,
          model = model,
          systemInstruction = Contents.of(curSystemPrompt),
          supportImage = showImagePicker,
          supportAudio = showAudioPicker,
          initialMessages = litertMessages,
          onDone = onDone,
          clearHistory = clearHistory,
        )
      }
    },
    showStopButtonInInputWhenInProgress = true,
    onStopButtonClicked = { model -> viewModel.stopResponse(model = model) },
    onSkillClicked = onSkillClicked,
    onMcpClicked = onMcpClicked,
    navigateUp = navigateUp,
    skillCount = skillCount,
    mcpCount = mcpCount,
    modifier = modifier,
    composableBelowMessageList = composableBelowMessageList,
    showImagePicker = showImagePicker,
    emptyStateComposable = emptyStateComposable,
    allowEditingSystemPrompt = allowEditingSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    onSystemPromptChanged = onSystemPromptChanged,
    sendMessageTrigger = sendMessageTrigger,
    showAudioPicker = showAudioPicker,
  )
}

private fun convertToLitertMessage(chatMessage: ChatMessage): Message? {
  if (chatMessage is ChatMessageText) {
    return when (chatMessage.side) {
      ChatSide.USER -> Message.user(chatMessage.content)
      ChatSide.AGENT -> Message.model(chatMessage.content)
      ChatSide.SYSTEM -> null
    }
  }
  return null
}
