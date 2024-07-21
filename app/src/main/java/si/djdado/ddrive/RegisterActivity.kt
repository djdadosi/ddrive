package si.djdado.ddrive

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import si.djdado.ddrive.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
    }

    fun postData(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Successfully registered with $email", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to register - ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun register(view: android.view.View) {
        val email = binding.editEmail.text.toString()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()

        if (password != confirmPassword) binding.textError.text = "Passwords don't match"

        postData(email, password)
    }

    fun back(view: android.view.View) {
        finish();
    }
}