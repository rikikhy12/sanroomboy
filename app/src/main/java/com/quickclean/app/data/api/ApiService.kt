package com.quickclean.app.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Backend API Interface for QuickClean by SanSanRoom
 * Configured for Host IP: 192.168.0.104 (Laragon Server)
 */
interface ApiService {

    // ================= REAL BACKEND AUTH (api/login.php) =================
    @POST("api/login.php")
    suspend fun loginSanSan(
        @Body request: SanSanLoginRequest
    ): Response<SanSanLoginResponse>

    // ================= MASTER ABSEN & LOCATION (api/mst_absen.php) =================
    @GET("api/mst_absen.php")
    suspend fun getMasterAbsen(): Response<MstAbsenResponse>

    // ================= ABSENSI DENGAN KOORDINAT LOKASI (api/absen.php) =================
    @POST("api/absen.php")
    suspend fun submitAbsen(
        @Body request: AbsenRequest
    ): Response<AbsenResponse>

    // ================= HISTORY ABSEN (api/history_absen.php) =================
    @GET("api/history_absen.php")
    suspend fun getHistoryAbsen(
        @Query("user_id") userId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("bulan") bulan: String? = null,
        @Query("status") status: String? = null
    ): Response<HistoryAbsenResponse>

    // ================= DASHBOARD JAGA MALAM PRIBADI (api/jaga_malam.php) =================
    @GET("api/jaga_malam.php")
    suspend fun getJagaMalam(
        @Query("user_id") userId: String,
        @Query("bulan") bulan: String? = null
    ): Response<JagaMalamResponse>

    // ================= VERIFIKASI WAJAH DEEP LEARNING (api/verify_face.php) =================
    @POST("api/verify_face.php")
    suspend fun verifyFace(
        @Body request: VerifyFaceRequest
    ): Response<VerifyFaceResponse>

    // ================= SUMMARY TOTALAN ABSEN (api/summary_absen.php) =================
    @GET("api/summary_absen.php")
    suspend fun getSummaryAbsen(
        @Query("user_id") userId: String?
    ): Response<SummaryAbsenResponse>

    // ================= REGISTRASI WAJAH MASTER PERTAMA KALI (api/register_face.php) =================
    @POST("api/register_face.php")
    suspend fun registerFace(
        @Body request: RegisterFaceRequest
    ): Response<RegisterFaceResponse>

    // ================= REGISTRASI SIDIK JARI / DEVICE (api/register_finger.php) =================
    @POST("api/register_finger.php")
    suspend fun registerFinger(
        @Body request: RegisterFingerRequest
    ): Response<RegisterFingerResponse>

    // ================= USERS LIST (api/users.php) =================
    @GET("api/users.php")
    suspend fun getUsers(): Response<SanSanUsersResponse>

    // ================= BOOKING OPTIONS (api/booking_options.php) =================
    @GET("api/booking_options.php")
    suspend fun getBookingOptions(): Response<BookingOptionsResponse>

    // ================= ROOMS LIST (api/rooms.php) =================
    @GET("api/rooms.php")
    suspend fun getRooms(): Response<RoomListApiResponse>

    // ================= SALES LIST (api/sales.php) =================
    @GET("api/sales.php")
    suspend fun getSales(): Response<SalesListApiResponse>

    // ================= DRAFT BOOKING (api/draft_booking.php) =================
    @POST("api/draft_booking.php")
    suspend fun submitDraftBooking(
        @Body request: DraftBookingRequest
    ): Response<DraftBookingResponse>

    @GET("api/draft_booking.php")
    suspend fun getDraftBookings(
        @Query("searchkey") searchKey: String? = null,
        @Query("date") date: String? = null,
        @Query("sd") sd: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<DraftBookingListResponse>

    @POST("api/draft_booking.php")
    suspend fun updateDraftBookingStatus(
        @Query("action") action: String = "checkin",
        @Query("id") id: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<DraftBookingResponse>

    // ================= PAYMENT METHODS (api/payment_methods.php) =================
    @GET("api/payment_methods.php")
    suspend fun getPaymentMethods(): Response<PaymentMethodsResponse>

    // ================= PRINT INVOICE DATA (api/print_invoice.php) =================
    @GET("api/print_invoice.php")
    suspend fun getPrintInvoiceJson(
        @Query("id") id: String? = null,
        @Query("kode_booking") kodeBooking: String? = null,
        @Query("format") format: String = "json"
    ): Response<PrintInvoiceResponse>

    // ================= ROOM KOTOR & ROOM CLEANING IN PROGRESS (api/room_kotor.php) =================
    @GET("api/room_kotor.php")
    suspend fun getRoomKotor(
        @Query("status_cleaning") statusCleaning: String? = null,
        @Query("tab") tab: String? = null,
        @Query("lokasi") lokasi: String? = null,
        @Query("search") search: String? = null,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int = 150
    ): Response<RoomKotorResponse>

    @POST("api/room_kotor.php")
    suspend fun updateRoomStatus(
        @Body request: Map<String, String>
    ): Response<BaseResponse>

    // ================= ROOM CHECKING & CHECKLIST TRANSAKSI (api/room_checking.php) =================
    @GET("api/room_checking.php")
    suspend fun getRoomChecking(
        @Query("room_no") roomNo: String? = null,
        @Query("status_checking") statusChecking: String? = null,
        @Query("lokasi") lokasi: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 150
    ): Response<RoomCheckingResponse>

    @POST("api/room_checking.php")
    suspend fun submitRoomChecking(
        @Body request: RoomCheckingRequest
    ): Response<RoomCheckingSubmitResponse>

    // ================= MASTER ITEM CHECKLIST (api/item_check.php) =================
    @GET("api/item_check.php")
    suspend fun getItemCheckList(): Response<ItemCheckResponse>

    // ================= REKAP TRANSAKSI, BOOKING & CHECK-IN (api/rekap_transaksi.php) =================
    @GET("api/rekap_transaksi.php")
    suspend fun getRekapTransaksi(
        @Query("type") type: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("lokasi") lokasi: String? = null,
        @Query("status_booking") statusBooking: String? = null,
        @Query("status_checkout") statusCheckout: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 150
    ): Response<RekapTransaksiResponse>
}

// ================= DTOs for Booking Options & Draft Booking =================
data class BaseResponse(
    val status: Boolean,
    val message: String,
    val data: Any? = null
)

data class BookingOptionsResponse(
    val status: Boolean,
    val message: String,
    val data: BookingOptionsData? = null
)

data class BookingOptionsData(
    val rooms: List<RoomOptionItem>? = null,
    val sales: List<SalesOptionItem>? = null,
    val jenis_harga: List<JenisHargaOptionItem>? = null,
    val type_room: List<TypeRoomOptionItem>? = null,
    val banks: List<BankOptionItem>? = null,
    val payment_methods: List<PaymentMethodItem>? = null
)

data class PaymentMethodsResponse(
    val status: Boolean,
    val message: String,
    val data: List<PaymentMethodItem>? = null
)

data class PaymentMethodItem(
    val id: Any? = null,
    val bank_code: String? = null,
    val nama_bank: String? = null,
    val nama_akun: String? = null,
    val logo: String? = null,
    val color: String? = null,
    val kd_akun: String? = null,
    val akun_id: Any? = null
) {
    val idInt: Int get() = id?.toString()?.toIntOrNull() ?: 15
    val displayName: String get() = (nama_bank ?: nama_akun ?: bank_code ?: "CASH").trim()
}

data class PrintInvoiceResponse(
    val status: Boolean,
    val message: String,
    val data: PrintInvoiceData? = null
)

data class PrintInvoiceData(
    val kode_booking: String? = null,
    val nama_tamu: String? = null,
    val total_bayar: Double? = 0.0,
    val dp: Double? = 0.0,
    val sisa_bayar: Double? = 0.0,
    val metode_bayar: String? = null,
    val plain_text: String? = null,
    val rawbt_url: String? = null,
    val print_url: String? = null
)

data class RoomOptionItem(
    val id: Any? = null,
    val kode_room: String? = null,
    val nomor_room: String? = null,
    val barcode: String? = null,
    val jenis_room: String? = null,
    val room_status: String? = null,
    val lantai: String? = null,
    val price: Any? = null
) {
    val displayNomor: String get() = nomor_room?.trim() ?: kode_room?.trim() ?: "Room"
}

data class SalesOptionItem(
    val id: Any? = null,
    val kode_sales: String? = null,
    val nama_sales: String? = null,
    val nama: String? = null,
    val name: String? = null,
    val sales_name: String? = null,
    val sales_kode: String? = null,
    val sales_id: String? = null,
    val identitas: String? = null,
    val label_colour: String? = null,
    val no_hp: String? = null,
    val phone: String? = null
) {
    val displayName: String get() = (nama_sales ?: sales_name ?: nama ?: name ?: kode_sales ?: identitas ?: "Sales").trim()
    val idString: String get() = id?.toString() ?: ""
}

data class JenisHargaOptionItem(
    val id: Any? = null,
    val kode_jenis: String? = null,
    val nama_jenis_harga: String? = null,
    val total_jam: String? = null,
    val total_harga: Any? = null,
    val tharga_1: Any? = null
) {
    val idInt: Int get() = id?.toString()?.toIntOrNull() ?: 0
    val displayName: String get() = (nama_jenis_harga ?: kode_jenis ?: "Paket").trim()
    val hargaDouble: Double get() {
        val s = total_harga?.toString() ?: tharga_1?.toString() ?: "0"
        return s.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
    }
}

data class TypeRoomOptionItem(
    val id: Any? = null,
    val nama_type: String? = null
)

data class BankOptionItem(
    val id: Any? = null,
    val nama_bank: String? = null,
    val no_rekening: String? = null,
    val atas_nama: String? = null
)

data class RoomListApiResponse(
    val status: Boolean,
    val message: String,
    val data: List<RoomOptionItem>? = null
)

data class SalesListApiResponse(
    val status: Boolean,
    val message: String,
    val data: List<SalesOptionItem>? = null
)

data class DraftBookingRequest(
    val nama_tamu: String,
    val no_hp: String,
    val no_room: String,
    val jenispaket: String,
    val checkin: String,
    val checkout: String? = null,
    val noktp: String? = null,
    val no_ktp: String? = null,
    val nik: String? = null,
    val total_bayar: Double,
    val dp: Double = 0.0,
    val akundp: Int? = null,
    val tipe_pembayaran: Int? = null,
    val nm_pembayaran: String? = null,
    val jmlroom: Int = 1,
    val sales: String? = null,
    val salesid: String? = null,
    val sales_id: String? = null,
    val sales_name: String? = null,
    val salesperson: String? = null,
    val keterangan: String? = null,
    val user_input: String? = null
)

data class DraftBookingResponse(
    val status: Boolean,
    val message: String,
    val data: DraftBookingData? = null
)

data class DraftBookingData(
    val id: Any? = null,
    val kode_booking: String? = null,
    val kode_booking_ref: String? = null,
    val nama_tamu: String? = null,
    val nmbooking: String? = null,
    val no_hp: String? = null,
    val no_room: String? = null,
    val room_nomor: String? = null,
    val tgl_booking: String? = null,
    val sales: Any? = null,
    val sales_id: Any? = null,
    val sales_name: String? = null,
    val sales_nm: String? = null,
    val checkin: String? = null,
    val check_in: String? = null,
    val checkout: String? = null,
    val check_out: String? = null,
    val no_ktp: String? = null,
    val nik: String? = null,
    val total_bayar: Any? = null,
    val total_booking: Any? = null,
    val dp: Any? = null,
    val akundp: Any? = null,
    val tipe_pembayaran: Any? = null,
    val nm_pembayaran: String? = null,
    val metode_pembayaran: String? = null,
    val jml_room: Any? = null,
    val status: String? = null,
    val status_booking: String? = null,
    val keterangan: String? = null,
    val user_input: String? = null,
    val created_at: String? = null,
    val create_date: String? = null,
    val note: String? = null
) {
    val displayGuestName: String get() = (nama_tamu ?: nmbooking ?: "Tamu").trim()
    val displayRoom: String get() = (no_room ?: room_nomor ?: "-").trim()
    val displayPhone: String get() = (no_hp ?: "-").trim()
    val displayTotalBayar: Double get() = (total_bayar?.toString() ?: total_booking?.toString() ?: "0").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
    val displayDp: Double get() = (dp?.toString() ?: "0").replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
    val displaySisa: Double get() = (displayTotalBayar - displayDp).coerceAtLeast(0.0)

    val stage: BookingStage get() {
        val s = (status_booking ?: "").uppercase()
        val st = (status ?: "").uppercase()
        return when {
            s.contains("CHECKIN") || s.contains("CHECK_IN") || s.contains("CHECKED") || st == "2" || st == "CHECKIN" -> BookingStage.CHECKIN
            s.contains("BOOKING") || s.contains("BOOKED") || st == "1" || st == "BOOKING" || displayDp > 0 -> BookingStage.BOOKING
            else -> BookingStage.DRAFT
        }
    }

    val displayStatus: String get() = stage.label
    val isCheckin: Boolean get() = stage == BookingStage.CHECKIN
    val isBooking: Boolean get() = stage == BookingStage.BOOKING
    val isDraft: Boolean get() = stage == BookingStage.DRAFT

    val displaySales: String get() = (sales_name ?: sales_nm ?: "-").trim()
    val displayCheckin: String get() = (checkin ?: check_in ?: tgl_booking ?: "-").trim()
    val displayCheckout: String get() = (checkout ?: check_out ?: "-").trim()
    val displayPaymentMethod: String get() = (nm_pembayaran ?: metode_pembayaran ?: "CASH").trim()
}

enum class BookingStage(val code: String, val label: String, val stepNumber: Int) {
    DRAFT("DRAFT", "Draft", 1),
    BOOKING("BOOKING", "Booking", 2),
    CHECKIN("CHECKIN", "Check-In", 3)
}

data class DraftBookingListResponse(
    val status: Boolean,
    val message: String,
    val data: DraftBookingListData? = null
)

data class DraftBookingListData(
    val total: Int = 0,
    val page: Int = 1,
    val summary: DraftBookingSummary? = null,
    val data: List<DraftBookingData>? = null,
    val draft_bookings: List<DraftBookingData>? = null
) {
    val items: List<DraftBookingData> get() = data ?: draft_bookings ?: emptyList()
}

data class DraftBookingSummary(
    val total_all: Int = 0,
    val total_checkin: Int = 0,
    val total_draft: Int = 0,
    val total_nominal: Double = 0.0,
    val total_dp: Double = 0.0
)

// ================= DTOs for PHP Backend =================
data class SummaryAbsenResponse(
    val status: Boolean,
    val message: String,
    val data: SummaryAbsenData? = null
)

data class SummaryAbsenData(
    val user_id: Int? = null,
    val total_absen: Int = 0,
    val total_hadir: Int = 0,
    val total_sakit: Int = 0,
    val total_izin: Int = 0,
    val total_alpa: Int = 0,
    val total_lembur: Int = 0,
    val tanggal_pertama: String? = null,
    val tanggal_terakhir: String? = null,
    val bulan_ini: SummaryBulanIniData? = null
)

data class SummaryBulanIniData(
    val bulan: String? = null,
    val total_bulan_ini: Int = 0,
    val hadir_bulan_ini: Int = 0,
    val sakit_bulan_ini: Int = 0,
    val izin_bulan_ini: Int = 0
)

data class RegisterFaceRequest(
    val user_id: String,
    val image_base64: String
)

data class RegisterFaceResponse(
    val status: Boolean,
    val message: String,
    val data: RegisterFaceData? = null
)

data class RegisterFaceData(
    val user_id: String? = null,
    val foto: String? = null,
    val foto_url: String? = null,
    val is_locked: Boolean = true,
    val registered_date: String? = null
)

data class VerifyFaceRequest(
    val user_id: String,
    val live_image: String
)

data class VerifyFaceResponse(
    val status: Boolean,
    val message: String,
    val data: VerifyFaceData? = null
)

data class VerifyFaceData(
    val match: Boolean = false,
    val similarity_percent: Float = 0f,
    val cosine_score: Float = 0f,
    val l2_distance: Float = 0f,
    val message: String = ""
)
data class HistoryAbsenResponse(
    val status: Boolean,
    val message: String,
    val data: HistoryAbsenData? = null
)

data class JagaMalamResponse(
    val status: Boolean,
    val message: String,
    val data: JagaMalamData? = null
)

data class JagaMalamData(
    val history: List<HistoryAbsenItem>? = null
)

data class HistoryAbsenData(
    val history: List<HistoryAbsenItem>? = null,
    val all_staff: List<AlpaKaryawanItem>? = null,
    val alpa_list: List<AlpaKaryawanItem>? = null,
    val hadir_list: List<AlpaKaryawanItem>? = null,
    val izin_list: List<AlpaKaryawanItem>? = null,
    val summary: HistorySummaryData? = null
)

data class HistorySummaryData(
    val total_absen: Int = 0,
    val total_hadir: Int = 0,
    val total_sakit: Int = 0,
    val total_izin: Int = 0,
    val total_alpa: Int = 0,
    val total_lembur: Int = 0,
    val total_karyawan_level_15_18: Int = 0,
    val total_hadir_level_15_18: Int = 0,
    val total_izin_level_15_18: Int = 0,
    val tanggal_pertama: String? = null,
    val tanggal_terakhir: String? = null
)

data class AlpaKaryawanItem(
    val id: String? = null,
    val user_id: String? = null,
    val username: String? = null,
    val adminname: String? = null,
    val level: String? = null,
    val level_label: String? = null,
    val status: String? = null,
    val status_label: String? = null,
    val tgl_absen: String? = null,
    val start_time: String? = null,
    val note: String? = null
) {
    val displayName: String get() = (adminname ?: username ?: "Karyawan").trim()
}

data class HistoryAbsenItem(
    val id: String? = null,
    val tgl_absen: String? = null,
    val kode_cleaning: String? = null,
    val officeboy: String? = null,
    val iduser: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val note: String? = null,
    val status: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val jarak_absen: String? = null,
    val create_date: String? = null,
    val nama_user: String? = null,
    val username_user: String? = null,
    val sub_bagian: String? = null
)
data class SanSanLoginRequest(
    val username: String,
    val password: String,
    val device_id: String? = null,
    val device_name: String? = null,
    val model: String? = null
)

data class SanSanLoginResponse(
    val status: Boolean,
    val message: String,
    val data: SanSanLoginData? = null
)

data class SanSanLoginData(
    val token: String? = null,
    val user: SanSanUserData? = null
)

data class SanSanUserData(
    val user_id: String? = null,
    val niklogin: String? = null,
    val username: String? = null,
    val userlogin: String? = null,
    val email: String? = null,
    val level: String? = null,
    val adminname: String? = null,
    val sub_bagian: String? = null,
    val danru: String? = null,
    val patch_img: String? = null,
    val devices_id: String? = null,
    val model: String? = null,
    val finger_id: String? = null,
    val foto: String? = null,
    val foto_url: String? = null
)

data class RegisterFingerRequest(
    val user_id: String,
    val finger_id: String,
    val device_id: String? = null,
    val model: String? = null
)

data class RegisterFingerResponse(
    val status: Boolean,
    val message: String,
    val data: RegisterFingerData? = null
)

data class RegisterFingerData(
    val user_id: String? = null,
    val finger_id: String? = null,
    val devices_id: String? = null,
    val model: String? = null
)

data class SanSanUsersResponse(
    val status: Boolean,
    val message: String,
    val data: List<SanSanUserData>? = null
)

// ================= DTOs for Presensi / Geo Location =================
data class AbsenRequest(
    val user_id: String? = null,
    val latitude: Double,
    val longitude: Double,
    val tipe: String = "MASUK",
    val kode_cleaning: String? = null,
    val barcode: String? = null,
    val note: String? = null,
    val max_radius: Double = 50.0,
    val face_image: String? = null,
    val device_id: String? = null,
    val device_name: String? = null,
    val model: String? = null,
    val finger_id: String? = null,
    val metode_absen: String = "BIOMETRIK"
)

data class AbsenResponse(
    val status: Boolean,
    val message: String,
    val data: AbsenResultData? = null
)

data class AbsenResultData(
    val user_id: String? = null,
    val tipe: String? = null,
    val user_coordinates: CoordinatesData? = null,
    val allowed_radius_meter: Double? = null,
    val can_absen: Boolean = false,
    val nearest_location: NearestLocationData? = null,
    val all_nearby: List<NearestLocationData>? = null
)

data class CoordinatesData(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class NearestLocationData(
    val id: String? = null,
    val kode_room: String? = null,
    val nomor_room: String? = null,
    val barcode: String? = null,
    val room_status: String? = null,
    val jenis_room: String? = null,
    val room_latitude: String? = null,
    val room_longitude: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val distance_meter: Double? = null,
    val distance_text: String? = null
)

data class MstAbsenResponse(
    val status: Boolean,
    val message: String,
    val data: List<MstAbsenItem>? = null
)

data class MstAbsenItem(
    val id: String? = null,
    val kode_room: String? = null,
    val nomor_room: String? = null,
    val barcode: String? = null,
    val lokasi: Int? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val room_status: String? = null,
    val jenis_room: String? = null
)

// ================= DTOs for Room Kotor, In Progress, & Checklist =================
data class RoomKotorResponse(
    val status: Boolean,
    val message: String,
    val data: RoomKotorData? = null
)

data class RoomKotorData(
    val summary: RoomStatusSummary? = null,
    val lokasi_list: List<LokasiItem>? = null,
    val room_chart: List<RoomChartItem>? = null,
    val in_progress: List<RoomInProgressItem>? = null,
    val rooms: List<RoomStatusItem>? = null
)

data class LokasiItem(
    val id: Int = 1,
    val kdlokasi: String = "",
    val nmlokasi: String = "",
    val label: String = ""
)

data class RoomChartItem(
    val id: String? = null,
    val room_no: String? = null,
    val kode_room: String? = null,
    val barcode: String? = null,
    val lokasi: Int? = null,
    val lokasi_nama: String? = null,
    val status_code: String? = null, // "READY", "KOTOR", "CHECKIN", "BOOKED", "MAINTENANCE", "CLEANING"
    val status_label: String? = null,
    val status_color: String? = null, // "RED", "BLUE", "ORANGE", "PURPLE", "YELLOW", "GREEN"
    val status_color_hex: String? = null,
    val guest_or_worker: String? = null,
    val note: String? = null,
    val is_kotor: Boolean = false,
    val is_in_progress: Boolean = false,
    val is_maintenance: Boolean = false,
    val is_cleaning: Boolean = false,
    val is_checkin: Boolean = false,
    val checkin_guest_name: String? = null,
    val checkin_code: String? = null,
    val checkin_time: String? = null,
    val is_booked: Boolean = false,
    val guest_name: String? = null,
    val sales_name: String? = null,
    val booking_ref: String? = null,
    val check_in_date: String? = null,
    val check_out_date: String? = null,
    val has_repair_request: Boolean = false,
    val perbaikan_id: String? = null,
    val deskripsi_kerusakan: String? = null,
    val pelapor_kerusakan: String? = null,
    val foto_kerusakan: String? = null,
    val foto_kerusakan_url: String? = null
) {
    val displayRoom: String get() = (room_no ?: "Room").trim()
}

data class RoomStatusSummary(
    val total_kamar_aktif: Int = 0,
    val total_kotor: Int = 0,
    val total_in_progress: Int = 0,
    val total_cleaning_in_progress: Int = 0,
    val total_maintenance_in_progress: Int = 0,
    val total_bersih: Int = 0,
    val total_checkin: Int = 0,
    val total_booked: Int = 0,
    val total_ready_vacant: Int = 0,
    val total_all: Int = 0,
    val total_gps: Int = 0,
    val total_pgv: Int = 0,
    val total_ekki: Int = 0,
    val total_per_lokasi: List<LokasiCountItem>? = null
)

data class LokasiCountItem(
    val id: Int = 1,
    val kdlokasi: String = "",
    val nmlokasi: String = "",
    val label: String? = null,
    val total: Int = 0
)

data class RoomInProgressItem(
    val id: String? = null,
    val tgl_cleaning: String? = null,
    val kode_cleaning: String? = null,
    val no_room: String? = null,
    val kode_room: String? = null,
    val officeboy: String? = null,
    val iduser: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val note: String? = null,
    val status: String? = null,
    val jenis: String? = null, // "C" = Cleaning, "M" = Maintenance
    val jenis_label: String? = null,
    val jenis_perbaikan: String? = null
) {
    val isCleaning: Boolean get() = jenis.equals("C", ignoreCase = true) || jenis.isNullOrBlank()
    val isMaintenance: Boolean get() = jenis.equals("M", ignoreCase = true)
    val displayRoom: String get() = (no_room ?: kode_room ?: "Room").trim()
    val displayWorker: String get() = (officeboy ?: "Staff OB").trim()
}

data class RoomStatusItem(
    val id: String? = null,
    val room_no: String? = null,
    val kode_room: String? = null,
    val barcode: String? = null,
    val lokasi: Int? = null,
    val date_time: String? = null,
    val status_cleaning: String? = null, // "0": kotor, "1": bersih, "2": in progress
    val status_label: String? = null, // "KOTOR", "BERSIH", "PROSES CLEANING", "PROSES MAINTENANCE", "SEDANG CHECK-IN", "SUDAH DIBOOKING"
    val user_id: String? = null,
    val auth_verf_name: String? = null,
    val jenis: String? = null,
    val officeboy: String? = null,
    val start_time: String? = null,
    val note: String? = null,
    val has_repair_request: Boolean = false,
    val perbaikan_id: String? = null,
    val deskripsi_kerusakan: String? = null,
    val pelapor_kerusakan: String? = null,
    val foto_kerusakan: String? = null,
    val foto_kerusakan_url: String? = null,
    val is_checkin: Boolean = false,
    val checkin_guest_name: String? = null,
    val checkin_code: String? = null,
    val checkin_time: String? = null,
    val checkout_time: String? = null,
    val is_booked: Boolean = false,
    val guest_name: String? = null,
    val sales_name: String? = null,
    val booking_ref: String? = null,
    val check_in_date: String? = null,
    val is_kotor: Boolean = false,
    val is_in_progress: Boolean = false,
    val is_cleaning: Boolean = false,
    val is_maintenance: Boolean = false,
    val status_checking: String? = null,
    val user_checking: String? = null,
    val is_checked: Boolean = false,
    val checklist_result: RoomChecklistResult? = null
) {
    val isKotor: Boolean get() = is_kotor || status_cleaning == "0" || status_label?.equals("KOTOR", ignoreCase = true) == true
    val isOccupiedCheckin: Boolean get() = is_checkin || status_label?.contains("CHECK-IN", ignoreCase = true) == true || status_label?.contains("CHECKIN", ignoreCase = true) == true
    val isAlreadyBooked: Boolean get() = is_booked || status_label?.contains("BOOKING", ignoreCase = true) == true
    val isBersih: Boolean get() = status_cleaning == "1" || status_label?.contains("BERSIH", ignoreCase = true) == true || isOccupiedCheckin || isAlreadyBooked
    val isReadyVacant: Boolean get() = isBersih && !isOccupiedCheckin && !isAlreadyBooked && !isInProgress && !isKotor
    val isInProgress: Boolean get() = is_in_progress || status_cleaning == "2" || (!officeboy.isNullOrBlank() && (status_cleaning == "2" || status_label?.contains("CLEAN", ignoreCase = true) == true || status_label?.contains("MAINT", ignoreCase = true) == true)) || status_label?.contains("PROSES", ignoreCase = true) == true
    val isMaintenance: Boolean get() = is_maintenance || jenis?.equals("M", ignoreCase = true) == true || status_label?.contains("MAINTENANCE", ignoreCase = true) == true
    val isCleaning: Boolean get() = is_cleaning || (isInProgress && !isMaintenance)
    val displayRoom: String get() = (room_no ?: "Room").trim()
    val displayReporter: String get() = (auth_verf_name ?: officeboy ?: "Staff").trim()
    val displayWorker: String get() = (officeboy ?: auth_verf_name ?: "Staff OB").trim()
    val isChecked: Boolean get() = is_checked || status_checking == "1" || checklist_result != null
}

data class RoomCheckingResponse(
    val status: Boolean,
    val message: String,
    val data: RoomCheckingData? = null
)

data class RoomCheckingData(
    val summary: RoomCheckingSummary? = null,
    val rooms: List<RoomCheckingItem> = emptyList()
)

data class RoomCheckingSummary(
    val total_kotor: Int = 0,
    val total_belum_checking: Int = 0,
    val total_sudah_checking: Int = 0,
    val total_sedang_dibersihkan: Int = 0,
    val total_sedang_maintenance: Int = 0,
    val total_gps: Int = 0,
    val total_pgv: Int = 0,
    val total_ekki: Int = 0
)

data class RoomCheckingItem(
    val id: String? = null,
    val room_no: String? = null,
    val kode_room: String? = null,
    val lokasi: Int? = null,
    val lokasi_nama: String? = null,
    val date_time: String? = null,
    val status_cleaning: String? = null,
    val status_checking: String? = null,
    val is_checked: Boolean = false,
    val user_checking: String? = null,
    val auth_verf_name: String? = null,
    val in_progress: Boolean = false,
    val is_maintenance: Boolean = false,
    val is_cleaning: Boolean = false,
    val officeboy: String? = null,
    val jenis: String? = null,
    val start_time: String? = null,
    val checklist_result: RoomChecklistResult? = null
) {
    val displayRoom: String get() = (room_no ?: "Room").trim()
    val isChecked: Boolean get() = is_checked || status_checking == "1" || checklist_result != null
    val displayWorker: String get() = (officeboy ?: "Staff OB").trim()
    val displayReporter: String get() = (auth_verf_name ?: user_checking ?: "Staff").trim()
}

data class RoomChecklistResult(
    val lampu: String = "Nyala",
    val ac: String = "Nyala",
    val tv: String = "Nyala",
    val air_wastafel: String = "Nyala",
    val air_kamar_mandi: String = "Nyala",
    val barang_rusak: String = "Tidak",
    val barang_rusak_note: String? = null,
    val barang_hilang: String = "Tidak",
    val barang_hilang_note: String? = null,
    val catatan: String? = null,
    val user_checking_name: String? = null,
    val created_at: String? = null
)

data class RoomCheckingRequest(
    val room_kotor_id: String? = null,
    val room_no: String,
    val lampu: String, // "Nyala" / "Mati"
    val ac: String, // "Nyala" / "Mati"
    val tv: String, // "Nyala" / "Mati"
    val air_wastafel: String, // "Nyala" / "Mati"
    val air_kamar_mandi: String = "Nyala", // "Nyala" / "Mati"
    val barang_rusak: String, // "Ada" / "Tidak"
    val barang_rusak_note: String? = null,
    val barang_hilang: String, // "Ada" / "Tidak"
    val barang_hilang_note: String? = null,
    val catatan: String? = null,
    val user_id: String? = null,
    val user_checking: String? = null,
    val user_checking_name: String? = null
)

data class RoomCheckingSubmitResponse(
    val status: Boolean,
    val message: String,
    val data: Any? = null
)

data class ItemCheckResponse(
    val status: Boolean,
    val message: String,
    val data: List<ItemCheckItem>? = null
)

data class ItemCheckItem(
    val id: Int? = null,
    val item_name: String? = null,
    val status: String? = "1" // "1" = Ya (Rapi/Bersih), "0" = Tidak (Kotor/Perlu Perbaikan)
)

// ================= DTOs for Rekap Transaksi, Booking & Check-In =================
data class RekapTransaksiResponse(
    val status: Boolean,
    val message: String,
    val data: RekapTransaksiData? = null
)

data class RekapTransaksiData(
    val filter: RekapFilterInfo? = null,
    val summary: RekapSummaryData? = null,
    val rekap_booking_harian: List<RekapHarianItem>? = null,
    val rekap_checkin_harian: List<RekapHarianItem>? = null,
    val bookings: List<BookingItem>? = null,
    val checkins: List<CheckinHeaderItem>? = null
)

data class RekapFilterInfo(
    val start_date: String? = null,
    val end_date: String? = null,
    val lokasi: String? = null
)

data class RekapSummaryData(
    val total_booking: Int = 0,
    val total_nominal_booking: Double = 0.0,
    val total_checkin: Int = 0,
    val total_nominal_checkin: Double = 0.0,
    val total_transaksi: Int = 0
)

data class RekapHarianItem(
    val tanggal: String? = null,
    val total_gps: Int = 0,
    val nominal_gps: Double = 0.0,
    val total_pgv: Int = 0,
    val nominal_pgv: Double = 0.0,
    val total_ekki: Int = 0,
    val nominal_ekki: Double = 0.0,
    val total_keseluruhan: Int = 0,
    val nominal_keseluruhan: Double = 0.0
)

data class BookingItem(
    val id: String? = null,
    val barcode: String? = null,
    val tgl_booking: String? = null,
    val kode_booking: String? = null,
    val kode_booking_ref: String? = null,
    val sales_name: String? = null,
    val nmbooking: String? = null,
    val check_in: String? = null,
    val check_out: String? = null,
    val room_nomor: String? = null,
    val total_room: Int = 1,
    val total_booking: Double = 0.0,
    val nominal_price: Double = 0.0,
    val dp: Double = 0.0,
    val status: String? = null,
    val status_label: String? = null,
    val lokasi: Int = 1,
    val lokasi_nama: String? = null,
    val auth_verf_name: String? = null
)

data class CheckinHeaderItem(
    val id: String? = null,
    val kode_checkin: String? = null,
    val tgl_checkin: String? = null,
    val tgl_checkout: String? = null,
    val check_out_time: String? = null,
    val sumber_data: String? = null,
    val kode_booking: String? = null,
    val room_no: String? = null,
    val nama_jenis_harga: String? = null,
    val sales: String? = null,
    val nama_tamu: String? = null,
    val price: Double = 0.0,
    val dp: Double = 0.0,
    val payment: Double = 0.0,
    val status_checkout: String? = null,
    val status_label: String? = null,
    val lokasi: Int = 1,
    val lokasi_nama: String? = null,
    val auth_verf_name: String? = null,
    val aut_verf_checkout: String? = null
)

// ================= Retrofit Client Singleton =================
object ApiClient {
    // Live Production Server
    const val BASE_URL = "https://api.sansanroom.id/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
