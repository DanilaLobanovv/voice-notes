package com.voicenotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicenotes.app.ui.AuthScreen
import com.voicenotes.app.ui.AuthViewModel
import com.voicenotes.app.ui.NoteDetailScreen
import com.voicenotes.app.ui.NoteFormScreen
import com.voicenotes.app.ui.NoteFormViewModel
import com.voicenotes.app.ui.NoteViewModel
import com.voicenotes.app.ui.NotesListScreen
import com.voicenotes.app.ui.NotesViewModel
import com.voicenotes.app.ui.VoiceNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VoiceNotesApp).container
        setContent {
            VoiceNotesTheme { AppRoot(container) }
        }
    }
}

/** Пока нет токена показываем вход, после входа (или при токене с прошлого запуска) сами заметки. */
@Composable
private fun AppRoot(container: AppContainer) {
    val token by container.session.token.collectAsStateWithLifecycle()
    if (token == null) {
        AuthScreen(viewModel(key = "auth") { AuthViewModel(container.repository) })
    } else {
        MainNav(container)
    }
}

@Composable
private fun MainNav(container: AppContainer) {
    val nav = rememberNavController()
    val repo = container.repository
    val context = LocalContext.current.applicationContext

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            NotesListScreen(
                vm = viewModel { NotesViewModel(repo) },
                onOpen = { id -> nav.navigate("note/$id") },
                onNew = { nav.navigate("form") },
            )
        }
        composable("note/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments!!.getString("id")!!
            NoteDetailScreen(
                vm = viewModel { NoteViewModel(repo, id) },
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("form?id=$id") },
            )
        }
        composable(
            "form?id={id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            NoteFormScreen(
                vm = viewModel { NoteFormViewModel(repo, context, id) },
                isEdit = id != null,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
