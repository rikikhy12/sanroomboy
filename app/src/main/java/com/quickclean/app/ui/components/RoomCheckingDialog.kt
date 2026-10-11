package com.quickclean.app.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickclean.app.data.api.ApiClient
import com.quickclean.app.data.api.RoomCheckingRequest
import com.quickclean.app.data.mock.MockData
import com.quickclean.app.ui.theme.BluePrimary
import kotlinx.coroutines.launch

@Composable
fun RoomCheckingDialog(
    roomNo: String,
    roomKotorId: String? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = MockData.currentUser

    // 6 Checklist items states:
    var lampuState by remember { mutableStateOf("Nyala") } // "Nyala" or "Mati"
    var acState by remember { mutableStateOf("Nyala") }
    var tvState by remember { mutableStateOf("Nyala") }
    var airWastafelState by remember { mutableStateOf("Nyala") }
    var airKamarMandiState by remember { mutableStateOf("Nyala") }
    var barangRusakState by remember { mutableStateOf("Tidak") } // "Ada" or "Tidak"
    var barangRusakNote by remember { mutableStateOf("") }
    var barangHilangState by remember { mutableStateOf("Tidak") } // "Ada" or "Tidak"
    var barangHilangNote by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }

    fun submitChecklist() {
        if (barangRusakState == "Ada" && barangRusakNote.isBlank()) {
            Toast.makeText(context, "Silakan isi catatan barang yang rusak.", Toast.LENGTH_SHORT).show()
            return
        }
        if (barangHilangState == "Ada" && barangHilangNote.isBlank()) {
            Toast.makeText(context, "Silakan isi catatan barang yang hilang.", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        scope.launch {
            try {
                val cleanRoomNo = roomNo.replace("room-", "", ignoreCase = true).replace("ROOM_", "", ignoreCase = true).trim()
                val activeName = user.displayName.ifBlank { user.name.ifBlank { user.username.ifBlank { user.adminname.ifBlank { "Roomboy" } } } }
                val activeId = user.id.ifBlank { user.username.ifBlank { "1" } }

                val req = RoomCheckingRequest(
                    room_kotor_id = roomKotorId,
                    room_no = cleanRoomNo,
                    lampu = lampuState,
                    ac = acState,
                    tv = tvState,
                    air_wastafel = airWastafelState,
                    air_kamar_mandi = airKamarMandiState,
                    barang_rusak = barangRusakState,
                    barang_rusak_note = if (barangRusakState == "Ada") barangRusakNote else null,
                    barang_hilang = barangHilangState,
                    barang_hilang_note = if (barangHilangState == "Ada") barangHilangNote else null,
                    catatan = additionalNotes.ifBlank { null },
                    user_id = activeId,
                    user_checking = activeName,
                    user_checking_name = activeName
                )

                val resp = ApiClient.apiService.submitRoomChecking(req)
                if (resp.isSuccessful && resp.body()?.status == true) {
                    Toast.makeText(context, "✓ Checklist Kamar $cleanRoomNo berhasil disimpan oleh $activeName!", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, resp.body()?.message ?: "Gagal menyimpan checklist.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Koneksi gagal: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isSubmitting = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── HEADER ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF2F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FactCheck,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Checklist Kamar Kotor",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Kamar $roomNo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Roomboy: ${user.displayName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { if (!isSubmitting) onDismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(14.dp))

                // ── CHECKLIST QUESTIONS (6 POIN) ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Silakan periksa kondisi kamar sebelum/sesudah pengerjaan:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )

                    // 1. Lampu
                    ChecklistTwoChoiceItem(
                        icon = Icons.Rounded.Lightbulb,
                        title = "1. Kondisi Lampu",
                        subtitle = "Apakah seluruh lampu kamar menyala normal?",
                        option1 = "Nyala",
                        option2 = "Mati",
                        selectedOption = lampuState,
                        onOptionSelected = { lampuState = it }
                    )

                    // 2. AC
                    ChecklistTwoChoiceItem(
                        icon = Icons.Rounded.AcUnit,
                        title = "2. Kondisi AC",
                        subtitle = "Apakah pendingin ruangan menyala dan dingin?",
                        option1 = "Nyala",
                        option2 = "Mati",
                        selectedOption = acState,
                        onOptionSelected = { acState = it }
                    )

                    // 3. TV
                    ChecklistTwoChoiceItem(
                        icon = Icons.Rounded.Tv,
                        title = "3. Kondisi TV",
                        subtitle = "Apakah TV & remote berfungsi normal?",
                        option1 = "Nyala",
                        option2 = "Mati",
                        selectedOption = tvState,
                        onOptionSelected = { tvState = it }
                    )

                    // 4. Air Wastafel
                    ChecklistTwoChoiceItem(
                        icon = Icons.Rounded.WaterDrop,
                        title = "4. Air Wastafel & Kran",
                        subtitle = "Apakah aliran air kran/wastafel menyala normal?",
                        option1 = "Nyala",
                        option2 = "Mati",
                        selectedOption = airWastafelState,
                        onOptionSelected = { airWastafelState = it }
                    )

                    // 5. Air Kamar Mandi / Shower
                    ChecklistTwoChoiceItem(
                        icon = Icons.Rounded.Shower,
                        title = "5. Air Kamar Mandi & Shower",
                        subtitle = "Apakah aliran air kamar mandi/shower menyala?",
                        option1 = "Nyala",
                        option2 = "Mati",
                        selectedOption = airKamarMandiState,
                        onOptionSelected = { airKamarMandiState = it }
                    )

                    // 6. Barang Rusak
                    Column {
                        ChecklistTwoChoiceItem(
                            icon = Icons.Rounded.ReportProblem,
                            title = "6. Barang / Fasilitas Rusak",
                            subtitle = "Apakah ada fasilitas kamar yang rusak/pecah?",
                            option1 = "Tidak",
                            option2 = "Ada",
                            option1Label = "Tidak Ada",
                            option2Label = "Ada Rusak",
                            selectedOption = barangRusakState,
                            onOptionSelected = { barangRusakState = it }
                        )

                        AnimatedVisibility(
                            visible = barangRusakState == "Ada",
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            OutlinedTextField(
                                value = barangRusakNote,
                                onValueChange = { barangRusakNote = it },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                label = { Text("Rincian barang yang rusak", fontSize = 12.sp) },
                                placeholder = { Text("Contoh: Gagang pintu longgar, remote AC pecah...", fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedLabelColor = BluePrimary,
                                    unfocusedLabelColor = Color(0xFF475569),
                                    focusedPlaceholderColor = Color(0xFF94A3B8),
                                    unfocusedPlaceholderColor = Color(0xFF94A3B8),
                                    cursorColor = BluePrimary,
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                    }

                    // 7. Barang Hilang
                    Column {
                        ChecklistTwoChoiceItem(
                            icon = Icons.Rounded.SearchOff,
                            title = "7. Barang Hilang / Tertinggal",
                            subtitle = "Apakah ada perlengkapan kamar yang hilang?",
                            option1 = "Tidak",
                            option2 = "Ada",
                            option1Label = "Tidak Ada",
                            option2Label = "Ada Hilang",
                            selectedOption = barangHilangState,
                            onOptionSelected = { barangHilangState = it }
                        )

                        AnimatedVisibility(
                            visible = barangHilangState == "Ada",
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            OutlinedTextField(
                                value = barangHilangNote,
                                onValueChange = { barangHilangNote = it },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                label = { Text("Rincian barang hilang", fontSize = 12.sp) },
                                placeholder = { Text("Contoh: Handuk kurang 1, hanger hilang...", fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedLabelColor = BluePrimary,
                                    unfocusedLabelColor = Color(0xFF475569),
                                    focusedPlaceholderColor = Color(0xFF94A3B8),
                                    unfocusedPlaceholderColor = Color(0xFF94A3B8),
                                    cursorColor = BluePrimary,
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                    }

                    // Catatan Tambahan (Opsional)
                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        label = { Text("Catatan Tambahan (Opsional)", fontSize = 12.sp) },
                        placeholder = { Text("Keterangan kondisi kamar lainnya...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedLabelColor = BluePrimary,
                            unfocusedLabelColor = Color(0xFF475569),
                            focusedPlaceholderColor = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF94A3B8),
                            cursorColor = BluePrimary,
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(14.dp))

                // ── SUBMIT BUTTONS ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (!isSubmitting) onDismiss() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Batal", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { submitChecklist() },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(2f).height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyimpan...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Checklist", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistTwoChoiceItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    option1: String,
    option2: String,
    option1Label: String = option1,
    option2Label: String = option2,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-choice pill buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 1
                val isOpt1Selected = selectedOption == option1
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOpt1Selected) Color(0xFFECFDF5) else Color.White,
                    border = BorderStroke(1.dp, if (isOpt1Selected) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionSelected(option1) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOpt1Selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = option1Label,
                            fontSize = 12.sp,
                            fontWeight = if (isOpt1Selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isOpt1Selected) Color(0xFF047857) else Color(0xFF475569)
                        )
                    }
                }

                // Option 2
                val isOpt2Selected = selectedOption == option2
                val opt2Color = if (option2 == "Mati" || option2 == "Ada") Color(0xFFDC2626) else Color(0xFF2563EB)
                val opt2Bg = if (option2 == "Mati" || option2 == "Ada") Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOpt2Selected) opt2Bg else Color.White,
                    border = BorderStroke(1.dp, if (isOpt2Selected) opt2Color else Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionSelected(option2) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOpt2Selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = opt2Color, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = option2Label,
                            fontSize = 12.sp,
                            fontWeight = if (isOpt2Selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isOpt2Selected) opt2Color else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}
