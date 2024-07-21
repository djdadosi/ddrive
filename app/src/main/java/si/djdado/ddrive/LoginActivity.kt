package si.djdado.ddrive

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import si.djdado.ddrive.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_login)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
    }

    fun postData(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Successfully logged with $email", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to login - ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun login(view: android.view.View) {
        val email = binding.etEmailPrijava.text.toString()
        val password = binding.etGesloPrijava.text.toString()
        postData(email, password)
    }

    /*fun back(view: android.view.View) {
        finish();
    }*/
}