package com.example.quanlysach.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.example.quanlysach.data.api.AddressApiService
import com.example.quanlysach.data.model.Address
import com.example.quanlysach.data.model.District
import com.example.quanlysach.data.model.GHNResponse
import com.example.quanlysach.data.model.Province
import com.example.quanlysach.data.model.Ward
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddAddressActivity : AppCompatActivity() {

    // 1. CÁC BIẾN GIAO DIỆN (Đã xóa ivBack và tvHeaderTitle cũ, dùng Toolbar mới)
    private lateinit var spnProvince: AutoCompleteTextView
    private lateinit var spnDistrict: AutoCompleteTextView
    private lateinit var spnWard: AutoCompleteTextView
    private lateinit var edtName: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtDetail: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var swDefault: SwitchMaterial
    private lateinit var btnDelete: Button

    private var username: String = ""
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    // 2. BIẾN LƯU DỮ LIỆU ĐỊA CHỈ GHN
    private var selectedProvinceName = ""
    private var selectedProvinceId = 0
    private var selectedDistrictName = ""
    private var selectedDistrictId = 0
    private var selectedWardName = ""
    private var selectedWardCode = ""

    private val addressApiService = AddressApiService.create()

    private var isEditMode = false
    private var addressToEdit: Address? = null

    // Cờ kiểm soát lần đầu load khi sửa địa chỉ
    private var isFirstLoadProvince = true
    private var isFirstLoadDistrict = true
    private var isFirstLoadWard = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_address)

        checkUserIdentity()
        initViews()
        setupInitialData()
        setupListeners()

        // Bắt đầu quy trình lấy dữ liệu từ GHN
        loadProvinces()
    }

    private fun checkUserIdentity() {
        username = intent.getStringExtra("USERNAME") ?: ""
        if (username.isEmpty()) {
            val sharedPref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
            username = sharedPref.getString("AppBanSachPrefs", "") ?: ""
        }

        addressToEdit = intent.getSerializableExtra("EDIT_ADDRESS") as? Address
        isEditMode = (addressToEdit != null)
    }

    private fun initViews() {
        // Chỉ ánh xạ những View còn tồn tại trong XML mới
        spnProvince = findViewById(R.id.spnProvince)
        spnDistrict = findViewById(R.id.spnDistrict)
        spnWard = findViewById(R.id.spnWard)
        edtName = findViewById(R.id.edtName)
        edtPhone = findViewById(R.id.edtPhone)
        edtDetail = findViewById(R.id.edtDetail)
        btnSave = findViewById(R.id.btnSaveAddress)
        swDefault = findViewById(R.id.cbDefaultAddress)
        btnDelete = findViewById(R.id.btnDeleteAddress)
    }

    private fun setupInitialData() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        if (isEditMode) {
            toolbar.title = "Cập nhật địa chỉ" // Cập nhật title thẳng vào Toolbar
            btnSave.text = "CẬP NHẬT"
            addressToEdit?.let {
                edtName.setText(it.receiverName)
                edtPhone.setText(it.phone)
                edtDetail.setText(it.detail)
                swDefault.isChecked = it.isDefault
            }
            btnDelete.visibility = View.VISIBLE
        } else {
            toolbar.title = "Địa chỉ mới"
            btnDelete.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        // Xử lý nút Back bằng Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
                .setPositiveButton("Xóa") { _, _ ->
                    addressToEdit?.id?.let { id ->
                        FirebaseDatabase.getInstance(dbUrl).getReference("Addresses")
                            .child(username).child(id).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đã xóa địa chỉ!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                }
                .setNegativeButton("Hủy", null).show()
        }

        btnSave.setOnClickListener { saveAddressToFirebase() }
    }

    // ==========================================
    // PHẦN 2: GỌI API GHN (DÙNG AUTOCOMPLETETEXTVIEW)
    // ==========================================

    private fun loadProvinces() {
        addressApiService.getProvinces().enqueue(object : Callback<GHNResponse<List<Province>>> {
            override fun onResponse(call: Call<GHNResponse<List<Province>>>, response: Response<GHNResponse<List<Province>>>) {
                val provinces = response.body()?.data ?: return
                val adapter = ArrayAdapter(this@AddAddressActivity, android.R.layout.simple_list_item_1, provinces)
                spnProvince.setAdapter(adapter)

                if (isEditMode && isFirstLoadProvince) {
                    val index = provinces.indexOfFirst { it.ProvinceID == addressToEdit?.provinceId }
                    if (index >= 0) {
                        spnProvince.setText(provinces[index].ProvinceName, false)
                        selectedProvinceId = provinces[index].ProvinceID
                        selectedProvinceName = provinces[index].ProvinceName
                        loadDistricts(selectedProvinceId)
                    }
                    isFirstLoadProvince = false
                }

                spnProvince.setOnItemClickListener { _, _, position, _ ->
                    val chosen = adapter.getItem(position) ?: return@setOnItemClickListener
                    selectedProvinceName = chosen.ProvinceName
                    selectedProvinceId = chosen.ProvinceID

                    spnDistrict.setText("", false)
                    spnWard.setText("", false)
                    loadDistricts(chosen.ProvinceID)
                }
            }
            override fun onFailure(call: Call<GHNResponse<List<Province>>>, t: Throwable) {
                Toast.makeText(this@AddAddressActivity, "Lỗi tải Tỉnh: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadDistricts(provinceId: Int) {
        addressApiService.getDistricts(provinceId).enqueue(object : Callback<GHNResponse<List<District>>> {
            override fun onResponse(call: Call<GHNResponse<List<District>>>, response: Response<GHNResponse<List<District>>>) {
                val districts = response.body()?.data ?: return
                val adapter = ArrayAdapter(this@AddAddressActivity, android.R.layout.simple_list_item_1, districts)
                spnDistrict.setAdapter(adapter)

                if (isEditMode && isFirstLoadDistrict) {
                    val index = districts.indexOfFirst { it.DistrictID == addressToEdit?.districtId }
                    if (index >= 0) {
                        spnDistrict.setText(districts[index].DistrictName, false)
                        selectedDistrictId = districts[index].DistrictID
                        selectedDistrictName = districts[index].DistrictName
                        loadWards(selectedDistrictId)
                    }
                    isFirstLoadDistrict = false
                }

                spnDistrict.setOnItemClickListener { _, _, position, _ ->
                    val chosen = adapter.getItem(position) ?: return@setOnItemClickListener
                    selectedDistrictName = chosen.DistrictName
                    selectedDistrictId = chosen.DistrictID

                    spnWard.setText("", false)
                    loadWards(chosen.DistrictID)
                }
            }
            override fun onFailure(call: Call<GHNResponse<List<District>>>, t: Throwable) {}
        })
    }

    private fun loadWards(districtId: Int) {
        addressApiService.getWards(districtId).enqueue(object : Callback<GHNResponse<List<Ward>>> {
            override fun onResponse(call: Call<GHNResponse<List<Ward>>>, response: Response<GHNResponse<List<Ward>>>) {
                val wards = response.body()?.data ?: return
                val adapter = ArrayAdapter(this@AddAddressActivity, android.R.layout.simple_list_item_1, wards)
                spnWard.setAdapter(adapter)

                if (isEditMode && isFirstLoadWard) {
                    val index = wards.indexOfFirst { it.WardCode == addressToEdit?.wardCode }
                    if (index >= 0) {
                        spnWard.setText(wards[index].WardName, false)
                        selectedWardName = wards[index].WardName
                        selectedWardCode = wards[index].WardCode
                    }
                    isFirstLoadWard = false
                }

                spnWard.setOnItemClickListener { _, _, position, _ ->
                    val chosen = adapter.getItem(position) ?: return@setOnItemClickListener
                    selectedWardName = chosen.WardName
                    selectedWardCode = chosen.WardCode
                }
            }
            override fun onFailure(call: Call<GHNResponse<List<Ward>>>, t: Throwable) {}
        })
    }

    // ==========================================
    // PHẦN 3: LƯU FIREBASE
    // ==========================================

    private fun saveAddressToFirebase() {
        val name = edtName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val detail = edtDetail.text.toString().trim()
        val isDefaultChecked = swDefault.isChecked

        if (name.isEmpty() || phone.isEmpty() || detail.isEmpty() || selectedProvinceId == 0) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin địa chỉ!", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Addresses").child(username)
        val addressId = if (isEditMode) addressToEdit!!.id else dbRef.push().key ?: return

        val newAddress = Address(
            id = addressId,
            username = username,
            receiverName = name,
            phone = phone,
            cityProvince = selectedProvinceName,
            provinceId = selectedProvinceId,
            district = selectedDistrictName,
            districtId = selectedDistrictId,
            ward = selectedWardName,
            wardCode = selectedWardCode,
            detail = detail,
            isDefault = isDefaultChecked
        )

        if (isDefaultChecked) {
            dbRef.get().addOnSuccessListener { snapshot ->
                for (child in snapshot.children) {
                    child.ref.child("isDefault").setValue(false)
                }
                executeSave(dbRef, addressId, newAddress)
            }
        } else {
            executeSave(dbRef, addressId, newAddress)
        }
    }

    private fun executeSave(dbRef: DatabaseReference, id: String, address: Address) {
        dbRef.child(id).setValue(address).addOnSuccessListener {
            Toast.makeText(this, "Đã lưu địa chỉ thành công!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}