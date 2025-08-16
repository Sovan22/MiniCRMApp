import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        if (auth.currentUser != null) {
            _authState.value = AuthState.Success(auth.currentUser)
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }

        Log.d("AuthViewModel", "Login attempt: $email")
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                Log.d("AuthViewModel", "Login successful for: ${auth.currentUser?.uid}")
                _authState.value = AuthState.Success(auth.currentUser)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        Log.d("AuthViewModel", "Sign-up attempt: $email with name: $name")
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {

                auth.createUserWithEmailAndPassword(email, password).await()


                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }
                    user.updateProfile(profileUpdates).await()
                    Log.d("AuthViewModel", "Sign-up and profile update successful")
                }

                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign-up failed: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Sign-up failed")
            }
        }
    }

    fun saveInStore(){
        //TODO
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}