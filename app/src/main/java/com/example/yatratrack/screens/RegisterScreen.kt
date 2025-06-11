package com.example.yatratrack.screens

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.yatratrack.navigation.Screen
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as Activity
    val genderOptions = listOf("Male", "Female", "Other")

    // Registration form states
    var fullName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(genderOptions[0]) }
    var isGenderDropdownExpanded by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var nicNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    // OTP states
    var showOtpSection by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var storedVerificationId by remember { mutableStateOf("") }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var timeLeft by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val currentDate = LocalDate.now()
    val dateText = currentDate.format(DateTimeFormatter.ofPattern("EEE MMM dd yyyy"))

    val isFormValid = fullName.isNotBlank() && address.isNotBlank() &&
            nicNumber.isNotBlank() && phoneNumber.length == 10

    fun saveRegistered(context: Context) {
        context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("isRegistered", true).apply()
    }

    // Timer for resend functionality
    LaunchedEffect(showOtpSection) {
        if (showOtpSection) {
            timeLeft = 60
            canResend = false
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            canResend = true
        }
    }

    fun verifyPhoneWithCredential(credential: PhoneAuthCredential) {
        isLoading = true
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    saveRegistered(context)
                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun sendOtp(phone: String, isResend: Boolean = false) {
        isLoading = true
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .apply {
                if (isResend && resendToken != null) {
                    setForceResendingToken(resendToken!!)
                }
            }
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    isLoading = false
                    // Auto-verification (for testing numbers)
                    verifyPhoneWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isLoading = false
                    Log.e("OTP", "Verification failed", e)
                    Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    isLoading = false
                    storedVerificationId = verificationId
                    resendToken = token
                    showOtpSection = true
                    if (isResend) {
                        timeLeft = 60
                        canResend = false
                        Toast.makeText(context, "OTP resent successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "OTP sent to +91$phone", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String) {
        if (storedVerificationId.isNotEmpty() && otp.length == 6) {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
            verifyPhoneWithCredential(credential)
        } else {
            Toast.makeText(context, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show()
        }
    }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0), // or MaterialTheme.colorScheme.primary
            darkIcons = false
        )
    }

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .background(Color(0xFF1565C0)) // same as status bar color
    )

    Surface(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1565C0)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (showOtpSection) "Verifying OTP..." else "Sending OTP...",
                        color = Color.Gray
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding() // handles space for keyboard
            ){
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!showOtpSection) {
                        // Registration Form
                        Text(
                            text = "Register",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = isGenderDropdownExpanded,
                            onExpandedChange = { isGenderDropdownExpanded = !isGenderDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedGender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isGenderDropdownExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isGenderDropdownExpanded,
                                onDismissRequest = { isGenderDropdownExpanded = false }
                            ) {
                                genderOptions.forEach { gender ->
                                    DropdownMenuItem(
                                        text = { Text(gender) },
                                        onClick = {
                                            selectedGender = gender
                                            isGenderDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = nicNumber,
                            onValueChange = { nicNumber = it },
                            label = { Text("NIC Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it.filter { char -> char.isDigit() }.take(10)
                            },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                sendOtp(phoneNumber)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                disabledContainerColor = Color(0xFF90CAF9),
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Send OTP")
                        }
                    } else {
                        // OTP Verification Section
                        Text(
                            text = "Verify OTP",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "We've sent a verification code to\n+91 $phoneNumber",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                        // Debug info for test numbers (remove in production)
                        Text(
                            text = "For test numbers, use your configured OTP",
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1565C0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // OTP Input Field
                        OtpInputField(
                            otpText = otpCode,
                            onOtpTextChange = { otpCode = it }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                verifyOtp(otpCode)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = otpCode.length == 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                disabledContainerColor = Color(0xFF90CAF9),
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Verify OTP")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resend OTP section
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Didn't receive the code? ",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )

                            if (canResend) {
                                TextButton(
                                    onClick = {
                                        sendOtp(phoneNumber, isResend = true)
                                    }
                                ) {
                                    Text(
                                        text = "Resend",
                                        color = Color(0xFF1565C0),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "Resend in ${timeLeft}s",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                showOtpSection = false
                                otpCode = ""
                                timeLeft = 60
                                canResend = false
                            }
                        ) {
                            Text(
                                text = "← Change Phone Number",
                                color = Color(0xFF1565C0),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun OtpInputField(
    otpText: String,
    onOtpTextChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = TextFieldValue(otpText, selection = TextRange(otpText.length)),
            onValueChange = { value ->
                if (value.text.length <= 6 && value.text.all { it.isDigit() }) {
                    onOtpTextChange(value.text)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val char = otpText.getOrNull(index)?.toString() ?: ""

                        Box(
                            modifier = Modifier
                                .size(35.dp) // Slightly reduced size
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (char.isNotEmpty()) Color(0xFF1565C0) else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = TextStyle(
                                    fontSize = 18.sp, // Slightly reduced font size
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}
